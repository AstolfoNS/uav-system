import logging
from pathlib import Path
from typing import Any, Tuple

from ultralytics import YOLO

logger = logging.getLogger("yolo-service")


SUPPORTED_WEIGHT_EXTENSIONS = {".pt", ".onnx"}
DEFAULT_ONNX_IMGSZ: Tuple[int, int] = (640, 640)


class YOLOModelManager:
    def __init__(self):
        self.model = None
        self.current_model_path = None
        self.onnx_input_size = None
        # 模型推理参数配置 (全局)，默认 conf=0.25, iou=0.7 等
        self.predict_params = {
            "conf": 0.25,
            "iou": 0.7
        }

    @staticmethod
    def _get_extension(model_path: str) -> str:
        return Path(model_path).suffix.lower()

    @staticmethod
    def _ensure_runtime_dependency(extension: str):
        if extension == ".onnx":
            try:
                import onnxruntime  # noqa: F401
            except Exception as e:
                raise RuntimeError(
                    "当前环境缺少 onnxruntime，无法加载 .onnx 权重。"
                    "请先安装依赖：uv add onnx \"onnxruntime<1.24\"（Python 3.10）"
                ) from e

    @staticmethod
    def _resolve_onnx_input_size(model_path: str) -> Tuple[int, int]:
        """读取 ONNX 输入 shape；若是动态 shape 则回退为默认 640x640。"""
        try:
            import onnxruntime

            session = onnxruntime.InferenceSession(model_path, providers=["CPUExecutionProvider"])
            input_shape = session.get_inputs()[0].shape

            if len(input_shape) >= 4:
                h = input_shape[2]
                w = input_shape[3]
                if isinstance(h, int) and isinstance(w, int) and h > 0 and w > 0:
                    return int(h), int(w)
        except Exception as e:
            logger.warning(f"读取 ONNX 输入尺寸失败，将使用默认尺寸 {DEFAULT_ONNX_IMGSZ}: {e}")

        return DEFAULT_ONNX_IMGSZ

    def _refresh_runtime_hints(self, model_path: str):
        extension = self._get_extension(model_path)
        if extension == ".onnx":
            self.onnx_input_size = self._resolve_onnx_input_size(model_path)
            logger.info(f"ONNX 推理输入尺寸: {self.onnx_input_size}")
        else:
            self.onnx_input_size = None

    def _validate_model_for_loading(self, model_path: str):
        if not model_path:
            raise RuntimeError("模型路径为空，无法加载权重")

        extension = self._get_extension(model_path)
        if extension not in SUPPORTED_WEIGHT_EXTENSIONS:
            supported = ", ".join(sorted(SUPPORTED_WEIGHT_EXTENSIONS))
            raise RuntimeError(
                f"不支持的权重格式: {extension or '无扩展名'}，仅支持 {supported}"
            )

        self._ensure_runtime_dependency(extension)

    def _format_model_load_error(self, model_path: str, error: Exception) -> RuntimeError:
        message = str(error)
        return RuntimeError(f"模型加载失败: {message}")

    def load_model(self, model_path: str):
        if self.model is None:
            self._validate_model_for_loading(model_path)
            try:
                self.model = YOLO(model_path)
                self.current_model_path = model_path
                self._refresh_runtime_hints(model_path)
                logger.info(f"YOLO 模型已成功加载: {model_path}")
            except Exception as e:
                logger.error(f"YOLO 模型加载失败: {model_path} -> {e}")
                raise self._format_model_load_error(model_path, e) from e

    def update_model(self, model_path: str):
        logger.info(f"即将切换 YOLO 模型: {model_path}")
        self._validate_model_for_loading(model_path)
        try:
            self.model = YOLO(model_path)
            self.current_model_path = model_path
            self._refresh_runtime_hints(model_path)
            logger.info(f"YOLO 模型已成功切换至: {model_path}")
        except Exception as e:
            logger.error(f"YOLO 模型切换失败: {model_path} -> {e}")
            raise self._format_model_load_error(model_path, e) from e

    def _is_current_model_onnx(self) -> bool:
        if not self.current_model_path:
            return False
        return self._get_extension(self.current_model_path) == ".onnx"

    def _format_predict_error(self, error: Exception) -> RuntimeError:
        message = str(error)
        if self._is_current_model_onnx() and "Non concat axis dimensions must match" in message:
            return RuntimeError(
                "模型推理异常: 当前 ONNX 模型输入尺寸与前处理尺寸不匹配。"
                "请重新导出 ONNX（建议 imgsz=640 且与训练配置一致），"
                "或在参数中显式设置合法 imgsz（建议 32 的倍数，如 640/960/1280）。"
            )
        return RuntimeError(f"模型推理异常: {message}")

    def predict(self, source: Any, **kwargs):
        if self.model is None:
            raise RuntimeError("模型未加载，请先初始化！")

        # 合并全局参数和传入参数，传入参数优先级更高
        final_params = {**self.predict_params, **kwargs}
        if self._is_current_model_onnx():
            final_params.setdefault("rect", False)
            final_params.setdefault("imgsz", list(self.onnx_input_size or DEFAULT_ONNX_IMGSZ))

        try:
            return self.model.predict(source, **final_params)
        except Exception as e:
            raise self._format_predict_error(e) from e

    # ============= 参数 CRUD 方法 =============
    def get_params(self) -> dict:
        return self.predict_params

    def update_param(self, key: str, value: float):
        if key == "imgsz":
            if value <= 0:
                raise ValueError("imgsz 必须大于 0")
            self.predict_params[key] = int(value)
            return
        self.predict_params[key] = value

    def delete_param(self, key: str):
        if key in self.predict_params:
            del self.predict_params[key]

# 导出单例对象
yolo_manager = YOLOModelManager()