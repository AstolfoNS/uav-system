import math
from typing import Tuple
import torch
import torch.nn as nn
import torch.nn.functional as F
__all__ = ['SFS']
class SFS(nn.Module):
    """
    Spiral-aware Feature Sampling (SFS)
    AI缝合怪自研复现！全网唯一
    Inputs:
        x:  (B, C, Hq, Wq)
        y:  (B, C, Hk, Wk)
    Output:
        out: (B, C, Hq, Wq)
    """
 
    def __init__(
        self,
        dim: int = 64,          # channel C
        num_heads: int = 8,     # H
        num_points: int = 4,    # P
        l0: float = 0.0,        # initial radius (pixel units in Y map)
        dl: float = 1.0,        # radial step (pixel units in Y map)
        ref_stride: int = 1,    # 1 => dense (every query), 2 => every 2 pixels, etc.
        attn_dropout: float = 0.0,
        proj_dropout: float = 0.0,
        align_corners: bool = True,
    ):
        super().__init__()
        assert dim % num_heads == 0, "dim must be divisible by num_heads"
 
        self.cfg_ref_stride = int(ref_stride)
        self.cfg_align_corners = bool(align_corners)
 
        self.dim = dim
        self.num_heads = num_heads
        self.num_points = num_points
        self.head_dim = dim // num_heads
        self.scale = self.head_dim ** -0.5
 
        # Learnable shared bias offsets ε: (H, P, 2)
        self.eps = nn.Parameter(torch.zeros(num_heads, num_points, 2))
 
        # Projections
        self.q_proj = nn.Linear(dim, dim, bias=True)
        self.k_proj = nn.Linear(dim, self.head_dim, bias=True)
        self.v_proj = nn.Linear(dim, self.head_dim, bias=True)
        self.out_proj = nn.Linear(dim, dim, bias=True)
 
        self.q_ln = nn.LayerNorm(dim)
        self.kv_ln = nn.LayerNorm(dim)
 
        self.attn_drop = nn.Dropout(attn_dropout)
        self.proj_drop = nn.Dropout(proj_dropout)
 
        # Precompute spiral base offsets s(h,k) as a buffer (pixel units)
        s = self._build_spiral_offsets(num_heads, num_points, l0, dl)  # (H,P,2)
        self.register_buffer("spiral_s", s, persistent=False)
 
    @staticmethod
    def _meshgrid_ij(a: torch.Tensor, b: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """Compatibility helper for older PyTorch without indexing='ij'."""
        try:
            return torch.meshgrid(a, b, indexing="ij")
        except TypeError:
            return torch.meshgrid(a, b)
 
    @staticmethod
    def _build_spiral_offsets(H: int, P: int, l0: float, dl: float) -> torch.Tensor:
        """
        s(h,k) = l_s [cos(theta_{h,k}), sin(theta_{h,k})]
        theta_{h,k} = 2πk/P + 2πh/H
        l_s = l0 + k*dl
        Returns: (H, P, 2) in pixel units (x,y).
        """
        offsets = torch.zeros(H, P, 2, dtype=torch.float32)
        for h in range(H):
            for k in range(P):
                kk = k + 1  # paper indexes k in [1,P]
                theta = 2.0 * math.pi * kk / P + 2.0 * math.pi * (h + 1) / H
                ls = l0 + kk * dl
                offsets[h, k, 0] = ls * math.cos(theta)  # x
                offsets[h, k, 1] = ls * math.sin(theta)  # y
        return offsets
 
    @staticmethod
    def _make_reference_grid(
        Hq: int, Wq: int, Hk: int, Wk: int, device, dtype, ref_stride: int = 1
    ) -> Tuple[torch.Tensor, int, int]:
        """
        Build reference points p for each query location, mapped into Y (key/value) feature coords,
        then normalized to [-1, 1] for grid_sample.
        Returns:
            ref_grid_norm: (1, Ns, 2) where Ns = Hs*Ws, in (x,y) normalized coords for Y
            Hs, Ws: spatial size after striding
        """
        ys = torch.arange(0, Hq, step=ref_stride, device=device, dtype=dtype)
        xs = torch.arange(0, Wq, step=ref_stride, device=device, dtype=dtype)
        Hs, Ws = ys.numel(), xs.numel()
 
        # Query grid in its own index space
        try:
            yy, xx = torch.meshgrid(ys, xs, indexing="ij")
        except TypeError:
            yy, xx = torch.meshgrid(ys, xs)
 
        # Map query pixel centers -> key/value pixel coords (in Y map)
        scale_y = Hq / max(Hk, 1)
        scale_x = Wq / max(Wk, 1)
 
        yk = (yy + 0.5) / scale_y - 0.5
        xk = (xx + 0.5) / scale_x - 0.5
 
        # Normalize to [-1,1] for grid_sample (x then y)
        if Wk > 1:
            x_norm = 2.0 * xk / (Wk - 1.0) - 1.0
        else:
            x_norm = torch.zeros_like(xk)
 
        if Hk > 1:
            y_norm = 2.0 * yk / (Hk - 1.0) - 1.0
        else:
            y_norm = torch.zeros_like(yk)
 
        ref = torch.stack([x_norm, y_norm], dim=-1)  # (Hs,Ws,2)
        ref = ref.view(1, Hs * Ws, 2)
        return ref, Hs, Ws
 
    @staticmethod
    def _normalize_offsets(offsets_xy: torch.Tensor, Hk: int, Wk: int) -> torch.Tensor:
        ox, oy = offsets_xy[..., 0], offsets_xy[..., 1]
        if Wk > 1:
            ox = 2.0 * ox / (Wk - 1.0)
        else:
            ox = torch.zeros_like(ox)
        if Hk > 1:
            oy = 2.0 * oy / (Hk - 1.0)
        else:
            oy = torch.zeros_like(oy)
        return torch.stack([ox, oy], dim=-1)
 
    def forward(self, DATA):
        y,x = DATA
        B, C, Hq, Wq = x.shape
        By, Cy, Hk, Wk = y.shape
 
        assert B == By, "Batch size mismatch"
        assert C == Cy == self.dim, f"Channel mismatch"
 
        # LN
        x_cl = x.permute(0, 2, 3, 1).contiguous()       # (B,Hq,Wq,C)
        x_ln = self.q_ln(x_cl).view(B, Hq * Wq, C)      # (B,Nq,C)
 
        ref_grid, Hs, Ws = self._make_reference_grid(
            Hq, Wq, Hk, Wk, device=x.device, dtype=x.dtype, ref_stride=self.cfg_ref_stride
        )  # (1,Ns,2)
        Ns = Hs * Ws
 
        delta_pix = self.spiral_s.to(dtype=x.dtype, device=x.device) + self.eps.to(dtype=x.dtype, device=x.device)
        delta_norm = self._normalize_offsets(delta_pix, Hk, Wk)  # (H,P,2)
 
        ref = ref_grid.view(1, 1, 1, Ns, 2).expand(B, self.num_heads, self.num_points, Ns, 2)
        dlt = delta_norm.view(1, self.num_heads, self.num_points, 1, 2).expand(B, self.num_heads, self.num_points, Ns, 2)
        grid = (ref + dlt).view(B, self.num_heads, self.num_points, Hs, Ws, 2)
 
        # Sample Y with bilinear interpolation
        y_rep = y.unsqueeze(1).unsqueeze(1).expand(B, self.num_heads, self.num_points, C, Hk, Wk)
        y_rep = y_rep.contiguous().view(B * self.num_heads * self.num_points, C, Hk, Wk)
        grid_rep = grid.contiguous().view(B * self.num_heads * self.num_points, Hs, Ws, 2)
 
        sampled = F.grid_sample(
            y_rep,
            grid_rep,
            mode="bilinear",
            padding_mode="zeros",
            align_corners=self.cfg_align_corners,
        )  # (B*H*P, C, Hs, Ws)
 
        sampled = sampled.view(B, self.num_heads, self.num_points, C, Hs, Ws)
        sampled = sampled.permute(0, 4, 5, 1, 2, 3).contiguous()   # (B,Hs,Ws,H,P,C)
        sampled = sampled.view(B, Ns, self.num_heads, self.num_points, C)  # (B,Ns,H,P,C)
 
        # LN on sampled KV (channel-last)
        sampled_ln = self.kv_ln(sampled)  # (B,Ns,H,P,C)
 
        # Project Q from x_ln (dense, Nq)
        q = self.q_proj(x_ln).view(B, Hq * Wq, self.num_heads, self.head_dim)  # (B,Nq,H,dh)
 
        k = self.k_proj(sampled_ln)  # (B,Ns,H,P,dh)
        v = self.v_proj(sampled_ln)  # (B,Ns,H,P,dh)
 
        if self.cfg_ref_stride == 1:
            q_used = q
            k_used = k
            v_used = v
            out_index_map = None
        else:
            ys = torch.arange(0, Hq, step=self.cfg_ref_stride, device=x.device)
            xs = torch.arange(0, Wq, step=self.cfg_ref_stride, device=x.device)
            try:
                yy, xx = torch.meshgrid(ys, xs, indexing="ij")
            except TypeError:
                yy, xx = torch.meshgrid(ys, xs)
            dense_idx = (yy * Wq + xx).reshape(-1)
 
            q_used = q[:, dense_idx, :, :]
            k_used = k
            v_used = v
            out_index_map = dense_idx
 
        # attn: (B,N,H,P)
        attn = (q_used.unsqueeze(3) * k_used).sum(-1) * self.scale
        attn = F.softmax(attn, dim=3)
        attn = self.attn_drop(attn)
 
        # out: (B,N,H,dh)
        out = (attn.unsqueeze(-1) * v_used).sum(dim=3)
        out = out.reshape(B, -1, self.dim)  # (B,N,C)
        out = self.out_proj(out)
        out = self.proj_drop(out)
 
        if out_index_map is not None:
            out_dense = torch.zeros(B, Hq * Wq, C, device=x.device, dtype=x.dtype)
            out_dense[:, out_index_map, :] = out
            out = out_dense
 
        Fs = out.view(B, Hq, Wq, C).permute(0, 3, 1, 2).contiguous()
 
        # Residual: Yi = X'_i + Fs
        return x + Fs
 
 