import uuid
import os

def generate_uuid_name() -> str:
    """
    生成一个基于 UUID4 的随机字符串（无中划线），用于作为文件名或目录名。
    """
    return uuid.uuid4().hex

def get_ext(filename: str) -> str:
    """
    获取安全的小写文件扩展名（带点）。如果无扩展名则返回空字符串。
    """
    _, ext = os.path.splitext(filename)
    return ext.lower()
