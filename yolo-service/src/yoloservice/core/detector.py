import logging

from ultralytics import YOLO

logger = logging.getLogger("yolo-service")


class YOLOModelManager:
    def __init__(self):
        self.model = None

    def load_model(self, model_path: str):
        if self.model is None:
            self.model = YOLO(model_path)
            logger.info(f"YOLO 模型已成功加载: {model_path}")

    def predict(self, source: str, **kwargs):
        if self.model is None:
            raise RuntimeError("模型未加载，请先初始化！")
        return self.model.predict(source, **kwargs)

# 导出单例对象
yolo_manager = YOLOModelManager()