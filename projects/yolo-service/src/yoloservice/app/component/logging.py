import logging.config
from yoloservice.config.settings import settings


def setup_logging():
    if getattr(setup_logging, "_configured", False):
        return
    setup_logging._configured = True

    logging.config.dictConfig(settings.LOG.LOGGING_CONFIG)
