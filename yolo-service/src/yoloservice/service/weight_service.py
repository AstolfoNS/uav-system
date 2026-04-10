import os
import shutil
from fastapi import UploadFile

from yoloservice import BASE_ROOT
from yoloservice.core.detector import yolo_manager
import logging

logger = logging.getLogger("yolo-service")

class WeightService:
    def __init__(self):
        self.weights_dir = os.path.join(BASE_ROOT, "weights")
        os.makedirs(self.weights_dir, exist_ok=True)

    def list_weights(self) -> dict:
        """列出所有可用的权重文件，并标识出当前激活的权重"""
        files = []
        if os.path.exists(self.weights_dir):
            for file_name in os.listdir(self.weights_dir):
                if file_name.endswith(".pt"):
                    files.append(file_name)
        
        current_path = yolo_manager.current_model_path
        active_weight = os.path.basename(current_path) if current_path else None
        
        return {
            "active": active_weight,
            "available": files
        }

    def upload_weight(self, file: UploadFile) -> dict:
        """上传新的权重文件"""
        if not file.filename.endswith(".pt"):
            raise ValueError("只允许上传 .pt 格式的权重文件")
            
        save_path = os.path.join(self.weights_dir, file.filename)
        
        try:
            with open(save_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)
            logger.info(f"成功上传并保存权重文件: {file.filename}")
            return {"message": "权重文件上传成功", "filename": file.filename}
        except Exception as e:
            logger.error(f"保存权重文件失败: {e}")
            raise RuntimeError(f"保存权重文件失败: {e}")

    def delete_weight(self, filename: str) -> dict:
        """删除指定的权重文件"""
        if not filename.endswith(".pt"):
            filename += ".pt"
            
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
        if not filename.endswith(".pt"):
            filename += ".pt"
            
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
