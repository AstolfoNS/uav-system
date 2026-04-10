import io
import os
import uuid
from typing import Union, BinaryIO

from minio import Minio
from minio.error import S3Error

from yoloservice.config.settings import settings

import logging
logger = logging.getLogger("yolo-service")

class MinIOClient:
    def __init__(self):
        try:
            self.client = Minio(
                endpoint=settings.MINIO_ENDPOINT,
                access_key=settings.MINIO_ACCESS_KEY,
                secret_key=settings.MINIO_SECRET_KEY,
                secure=settings.MINIO_SECURE,
            )
            self.bucket_name = settings.MINIO_BUCKET_NAME
            self._ensure_bucket_exists()
            logger.info(f"MinIO client initialized successfully. Connected to {settings.MINIO_ENDPOINT}")
        except Exception as e:
            logger.error(f"Failed to initialize MinIO client: {e}")
            self.client = None

    def _ensure_bucket_exists(self):
        if self.client is None:
            return
        
        try:
            found = self.client.bucket_exists(self.bucket_name)
            if not found:
                self.client.make_bucket(self.bucket_name)
                # optionally set bucket policy for public access if needed
                logger.info(f"Created MinIO bucket: {self.bucket_name}")
            else:
                logger.info(f"MinIO bucket {self.bucket_name} already exists.")
        except S3Error as e:
            logger.error(f"Error checking/creating bucket {self.bucket_name}: {e}")

    def upload_file(self, object_name: str, file_path: str) -> str:
        """
        上传本地文件到 MinIO
        """
        if self.client is None:
            logger.error("MinIO client is not initialized, cannot upload file.")
            raise Exception("MinIO client is not initialized.")
            
        try:
            self.client.fput_object(
                bucket_name=self.bucket_name,
                object_name=object_name,
                file_path=file_path,
            )
            # return object url
            schema = "https" if settings.MINIO_SECURE else "http"
            url = f"{schema}://{settings.MINIO_ENDPOINT}/{self.bucket_name}/{object_name}"
            return url
        except S3Error as e:
            logger.error(f"Failed to upload file {file_path} to {object_name}: {e}")
            raise e

    def upload_bytes(self, object_name: str, data: bytes, content_type: str = "application/octet-stream") -> str:
        """
        上传字节数据到 MinIO
        """
        if self.client is None:
            logger.error("MinIO client is not initialized, cannot upload data.")
            raise Exception("MinIO client is not initialized.")
            
        try:
            data_stream = io.BytesIO(data)
            self.client.put_object(
                bucket_name=self.bucket_name,
                object_name=object_name,
                data=data_stream,
                length=len(data),
                content_type=content_type,
            )
            schema = "https" if settings.MINIO_SECURE else "http"
            url = f"{schema}://{settings.MINIO_ENDPOINT}/{self.bucket_name}/{object_name}"
            return url
        except S3Error as e:
            logger.error(f"Failed to upload bytes to {object_name}: {e}")
            raise e

minio_client = MinIOClient()
