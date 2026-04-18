import torch
import torch.nn as nn
import torch.nn.functional as F
__all__ = ['CAFMfusion']
def channel_shuffle(x: torch.Tensor, groups: int = 2) -> torch.Tensor:
    """
    Channel Shuffle
    x: [B, C, H, W]
    """
    b, c, h, w = x.shape
    if c % groups != 0:
        raise ValueError(f"channels={c} must be divisible by groups={groups}")
 
    x = x.view(b, groups, c // groups, h, w)
    x = x.transpose(1, 2).contiguous()
    x = x.view(b, c, h, w)
    return x
class DepthwiseConv2d(nn.Module):
    """
    Depthwise Conv used in detail estimation.
    """
    def __init__(self, channels: int, kernel_size: int = 3, dilation: int = 1, bias: bool = False):
        super().__init__()
        padding = dilation * (kernel_size // 2)
        self.conv = nn.Conv2d(
            in_channels=channels,
            out_channels=channels,
            kernel_size=kernel_size,
            stride=1,
            padding=padding,
            dilation=dilation,
            groups=channels,
            bias=bias,
        )
 
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.conv(x)
 
 
class CAFMfusion(nn.Module):
    """
    Cross-Semantic Adaptive Filtering Module (CAFM)
    """
    def __init__(self, channels: int, shuffle_groups: int = 2):
        super().__init__()
        self.channels = channels
        self.shuffle_groups = shuffle_groups
        self.dwconv_k3 = DepthwiseConv2d(channels, kernel_size=3, dilation=1, bias=False)
        self.dwconv_k3_d2 = DepthwiseConv2d(channels, kernel_size=3, dilation=2, bias=False)
        self.detail_fuse = nn.Conv2d(2 * channels, channels, kernel_size=1, bias=True)
        self.global_fuse = nn.Conv2d(2, 1, kernel_size=1, bias=True)
        self.selector = nn.Conv2d(channels, 2, kernel_size=1, bias=True)
        self.interact_l = nn.Conv2d(2 * channels, channels, kernel_size=1, bias=True)
        self.interact_h = nn.Conv2d(2 * channels, channels, kernel_size=1, bias=True)
        self.reconstruct = nn.Conv2d(channels, channels, kernel_size=1, bias=True)
        self.sigmoid = nn.Sigmoid()
 
    def forward(self, data):
        x_l, x_h = data
        """
        Args:
            x_l: [B, C, H, W]
            x_h: [B, C, H, W] or [B, C, h, w]
        Returns:
            y:   [B, C, H, W]
        """
        if x_l.dim() != 4 or x_h.dim() != 4:
            raise ValueError("x_l and x_h must be 4D tensors [B, C, H, W].")
 
        # Align spatial size if needed
        if x_h.shape[2:] != x_l.shape[2:]:
            x_h = F.interpolate(x_h, size=x_l.shape[2:], mode="bilinear", align_corners=False)
 
        # Channels must match the paper setting
        if x_l.shape[1] != self.channels or x_h.shape[1] != self.channels:
            raise ValueError(
                f"Expected both inputs to have {self.channels} channels, "
                f"but got x_l={x_l.shape[1]}, x_h={x_h.shape[1]}"
            )
 
        # 1 Detail Estimation
        x_d_1 = self.dwconv_k3(x_l)
        x_d_2 = self.dwconv_k3_d2(x_l)
        x_d = self.detail_fuse(torch.cat([x_d_1, x_d_2], dim=1))
 
        # 2 Global Estimation
        x_h_avg = torch.mean(x_h, dim=1, keepdim=True)
        x_h_max, _ = torch.max(x_h, dim=1, keepdim=True)
        x_g = self.global_fuse(torch.cat([x_h_avg, x_h_max], dim=1))
 
        x_prime = x_d + x_g
        spatial_logits = self.selector(x_prime)      # [B,2,H,W]
        spatial_weights = torch.softmax(spatial_logits, dim=1)
 
        w_l = spatial_weights[:, 0:1, :, :]          # WL: [B,1,H,W]
        w_h = spatial_weights[:, 1:2, :, :]          # WH: [B,1,H,W]
 
        x_l_w = w_l * x_l
        x_h_w = w_h * x_h
 
        x_l_cat = torch.cat([x_l_w, x_l], dim=1)
        x_h_cat = torch.cat([x_h_w, x_h], dim=1)
 
        x_l_cat = channel_shuffle(x_l_cat, groups=self.shuffle_groups)
        x_h_cat = channel_shuffle(x_h_cat, groups=self.shuffle_groups)
 
        x_l_tilde = self.interact_l(x_l_cat)
        x_h_tilde = self.interact_h(x_h_cat)
 
        w_s = x_l_tilde * self.sigmoid(x_h_tilde) + x_h_tilde * self.sigmoid(x_l_tilde)
 
        w_f = self.sigmoid(self.reconstruct(w_s))
        y = w_f * x_l
        return y
 
 