import yaml
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

from yoloservice import SRC_ROOT, BASE_ROOT


LOG_TEMPLATE_PATH = SRC_ROOT / "yoloservice" / "config" / "log" / "logging.yaml"


class LoggingSettings(BaseSettings):
    """日志配置类：基于 YAML 模板动态生成"""

    model_config = SettingsConfigDict(extra="ignore")

    # --- 可配置参数 (支持 .env 覆盖) ---
    LOG_LEVEL: str = "INFO"
    LOG_BACKUP_COUNT: int = 7
    LOG_FILENAME: str = "app.log"

    @property
    def _TEMPLATE_PATH(self) -> Path:
        """YAML 模板的绝对路径"""
        return LOG_TEMPLATE_PATH

    @property
    def LOG_DIR(self) -> Path:
        """确保日志目录存在 (位于项目根目录/logs)"""
        _path = BASE_ROOT / "logs"
        _path.mkdir(parents=True, exist_ok=True)
        return _path

    @property
    def LOGGING_CONFIG(self) -> dict:
        """读取 YAML 模板并填充变量"""
        if not self._TEMPLATE_PATH.exists():
            raise FileNotFoundError(f"未找到日志模板文件: {self._TEMPLATE_PATH}")

        with open(self._TEMPLATE_PATH, "r", encoding="utf-8") as f:
            template_content = f.read()

        # 准备填充数据
        log_file_path = self.LOG_DIR / self.LOG_FILENAME

        # 填充 YAML 占位符
        try:
            formatted_yaml = template_content.format(
                LOG_LEVEL=self.LOG_LEVEL.upper(),
                LOG_FILE_PATH=str(log_file_path).replace("\\", "/"), # 兼容 Windows
                LOG_BACKUP_COUNT=self.LOG_BACKUP_COUNT
            )
            return yaml.safe_load(formatted_yaml)
        except KeyError as e:
            raise ValueError(f"日志模板格式化失败，缺少占位符或存在冲突: {e}")
