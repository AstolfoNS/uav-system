import os
import shutil
from fastapi import UploadFile

from yoloservice import BASE_ROOT
from yoloservice.core.detector import yolo_manager
import logging

logger = logging.getLogger("yolo-service")

SUPPORTED_WEIGHT_EXTENSIONS = (".pt", ".onnx")

class WeightService:
    def __init__(self):
        self.weights_dir = os.path.join(BASE_ROOT, "weights")
        os.makedirs(self.weights_dir, exist_ok=True)

    def list_weights(self) -> dict:
        """列出所有可用的权重文件，并标识出当前激活的权重"""
        files = []
        if os.path.exists(self.weights_dir):
            for file_name in os.listdir(self.weights_dir):
                if self._is_supported_weight(file_name):
                    files.append(file_name)

        files.sort()
        
        current_path = yolo_manager.current_model_path
        active_weight = os.path.basename(current_path) if current_path else None
        
        return {
            "active": active_weight,
            "available": files
        }

    @staticmethod
    def _normalize_filename(filename: str) -> str:
        return os.path.basename((filename or "").strip())

    @staticmethod
    def _get_extension(filename: str) -> str:
        return os.path.splitext(filename)[1].lower()

    def _is_supported_weight(self, filename: str) -> bool:
        return self._get_extension(filename) in SUPPORTED_WEIGHT_EXTENSIONS

    def _ensure_supported_weight(self, filename: str):
        if not self._is_supported_weight(filename):
            supported = ", ".join(SUPPORTED_WEIGHT_EXTENSIONS)
            raise ValueError(f"仅支持以下权重格式: {supported}")

    def _resolve_weight_filename(self, filename: str) -> str:
        normalized = self._normalize_filename(filename)
        if not normalized:
            raise ValueError("权重文件名不能为空")

        if self._is_supported_weight(normalized):
            return normalized

        # 兼容旧调用：如果未传扩展名且仅匹配到一个已存在文件，则自动补全。
        candidates = []
        for ext in SUPPORTED_WEIGHT_EXTENSIONS:
            candidate = f"{normalized}{ext}"
            if os.path.exists(os.path.join(self.weights_dir, candidate)):
                candidates.append(candidate)

        if len(candidates) == 1:
            return candidates[0]

        if len(candidates) > 1:
            raise ValueError("存在同名不同后缀权重文件，请传入包含扩展名的完整文件名")

        raise FileNotFoundError(f"找不到权重文件 {normalized}，请先上传")

    def upload_weight(self, file: UploadFile) -> dict:
        """上传新的权重文件"""
        normalized = self._normalize_filename(file.filename or "")
        if not normalized:
            raise ValueError("权重文件名不能为空")

        self._ensure_supported_weight(normalized)
            
        save_path = os.path.join(self.weights_dir, normalized)
        
        try:
            with open(save_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)
            logger.info(f"成功上传并保存权重文件: {normalized}")
            return {"message": "权重文件上传成功", "filename": normalized}
        except Exception as e:
            logger.error(f"保存权重文件失败: {e}")
            raise RuntimeError(f"保存权重文件失败: {e}")

    def delete_weight(self, filename: str) -> dict:
        """删除指定的权重文件"""
        filename = self._resolve_weight_filename(filename)

        file_path = os.path.join(self.weights_dir, filename)
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"找不到权重文件 {filename}")
            
        current_path = yolo_manager.current_model_path
        if current_path and os.path.abspath(current_path) == os.path.abspath(file_path):
            raise ValueError("不能删除当前正在使用的活跃模型")
            
        try:
            os.remove(file_path)
            logger.info(f"删除了权重文件: {filename}")
            return {"message": f"成功删除 {filename}"}
        except Exception as e:
            logger.error(f"删除权重文件失败 {filename}: {e}")
            raise RuntimeError(f"删除权重文件失败: {e}")
            
    def switch_active_weight(self, filename: str) -> dict:
        """切换活跃的模型权重"""
        filename = self._resolve_weight_filename(filename)

        file_path = os.path.join(self.weights_dir, filename)
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"找不到权重文件 {filename}，请先上传")
            
        try:
            yolo_manager.update_model(file_path)
            # 在这里我们仅进行内存中的软切换，不会直接写回 .env 文件。
            return {"message": f"成功切换当前活跃模型至 {filename}"}
        except Exception as e:
            logger.error(f"切换模型权重失败 {filename}: {e}")
            raise RuntimeError(f"切换模型权重失败: {e}")

weight_service = WeightService()
