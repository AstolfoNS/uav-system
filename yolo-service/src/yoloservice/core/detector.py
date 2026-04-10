import logging

from ultralytics import YOLO

logger = logging.getLogger("yolo-service")


class YOLOModelManager:
    def __init__(self):
        self.model = None
        self.current_model_path = None
        # 模型推理参数配置 (全局)，默认 conf=0.25, iou=0.7 等
        self.predict_params = {
            "conf": 0.25,
            "iou": 0.7
        }

    def load_model(self, model_path: str):
        if self.model is None:
            self.model = YOLO(model_path)
            self.current_model_path = model_path
            logger.info(f"YOLO 模型已成功加载: {model_path}")

    def update_model(self, model_path: str):
        logger.info(f"即将切换 YOLO 模型: {model_path}")
        self.model = YOLO(model_path)
        self.current_model_path = model_path
        logger.info(f"YOLO 模型已成功切换至: {model_path}")

    def predict(self, source: str, **kwargs):
        if self.model is None:
            raise RuntimeError("模型未加载，请先初始化！")
            
        # 合并全局参数和传入参数，传入参数优先级更高
        final_params = {**self.predict_params, **kwargs}
        return self.model.predict(source, **final_params)

    # ============= 参数 CRUD 方法 =============
    def get_params(self) -> dict:
        return self.predict_params

    def update_param(self, key: str, value: float):
        self.predict_params[key] = value

    def delete_param(self, key: str):
        if key in self.predict_params:
            del self.predict_params[key]

# 导出单例对象
yolo_manager = YOLOModelManager()