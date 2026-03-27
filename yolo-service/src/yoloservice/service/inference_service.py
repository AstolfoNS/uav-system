import os
import time
import cv2
import numpy as np

from yoloservice.core.detector import yolo_manager
from yoloservice.config.settings import settings
from yoloservice.common.utils.file_utils import generate_uuid_name
import cv2
import numpy as np

from yoloservice.core.detector import yolo_manager
from yoloservice.config.settings import settings

class InferenceService:
    def __init__(self):
        # 确保基础目录存在
        os.makedirs(settings.OUTPUT_DIR, exist_ok=True)
        self.image_out_dir = os.path.join(settings.OUTPUT_DIR, "images")
        os.makedirs(self.image_out_dir, exist_ok=True)

    def process_image(self, image_bytes: bytes, request_host_url: str) -> dict:
        """
        处理图像推理并保存绘制了边界框的图像
        """
        np_arr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError("图像解码失败，文件格式可能不正确或已损坏")

        results = yolo_manager.predict(img)

        detections = []
        self.image_out_dir = os.path.join(os.path.abspath(settings.OUTPUT_DIR), "images")
        os.makedirs(self.image_out_dir, exist_ok=True)
        
        uuid_str = generate_uuid_name()
        filename = f"image_{uuid_str}.jpg"
        save_path = os.path.join(self.image_out_dir, filename)
        image_url = ""

        if results and len(results) > 0:
            result = results[0]

            # 解析并提取识别框
            for box in result.boxes:
                class_id = int(box.cls[0])
                class_name = result.names[class_id] if result.names else str(class_id)
                detections.append({
                    "class_id": class_id,
                    "class_name": class_name,
                    "confidence": round(float(box.conf[0]), 4),
                    "bbox": [round(x, 2) for x in box.xyxy[0].tolist()]
                })

            # 用 plot() 函数在图片上画框
            annotated_img = result.plot()
            cv2.imwrite(save_path, annotated_img)
            
            # 构建可访问的 URL: host/static/images/xxx.jpg
            # request_host_url e.g. "http://127.0.0.1:8000"
            base_static_url = str(request_host_url).rstrip("/") + settings.STATIC_URL
            image_url = f"{base_static_url}/images/{filename}"

        return {
            "count": len(detections),
            "detections": detections,
            "image_url": image_url
        }

    def process_video(self, temp_video_path: str, request_host_url: str) -> dict:
        """
        处理视频推理并保存带有识别框的视频结果
        """
        cap = cv2.VideoCapture(temp_video_path)
        if not cap.isOpened():
            raise ValueError("无法读取视频文件，视频可能已损坏。")
        cap.release()

        # 生成 UUID 用于文件命名
        uuid_str = generate_uuid_name()
        job_name = f"temp_{uuid_str}"
        
        # 强制使用绝对路径，统一放到 videos 目录下。
        project_abs_dir = os.path.join(os.path.abspath(settings.OUTPUT_DIR), "videos")
        os.makedirs(project_abs_dir, exist_ok=True)

        # YOLO 将会自动在这个 temp 子目录下保存渲染好的视频
        results = yolo_manager.predict(
            source=temp_video_path,
            save=True,               
            project=project_abs_dir,      
            name=job_name,    
            exist_ok=True            
        )

        original_filename = os.path.basename(temp_video_path)
        
        # yolo 保存结果通常和源文件名字相同，除非有特殊前缀
        save_dir = results[0].save_dir if (results and len(results) > 0) else os.path.join(project_abs_dir, job_name)
        
        generated_files = os.listdir(save_dir) if os.path.exists(save_dir) else []
        video_filename = original_filename
        
        for f in generated_files:
            if f.endswith(('.mp4', '.avi', '.mov', '.mkv')):
                video_filename = f
                break

        final_video_name = ""
        # 提取扩展名并重命名/移动到外层
        import shutil
        from yoloservice.common.utils.file_utils import get_ext
        ext = get_ext(original_filename)
        
        if os.path.exists(os.path.join(save_dir, video_filename)):
            final_video_name = f"video_{uuid_str}{ext}"
            final_path = os.path.join(project_abs_dir, final_video_name)
            # 移动并重命名文件
            shutil.move(os.path.join(save_dir, video_filename), final_path)
            # 删除临时目录
            shutil.rmtree(save_dir, ignore_errors=True)
        else:
            final_video_name = original_filename

        # 构建可访问的 URL: host/static/videos/video_xxx.mp4
        base_static_url = str(request_host_url).rstrip("/") + settings.STATIC_URL
        video_url = f"{base_static_url}/videos/{final_video_name}"

        return {
            "video_url": video_url,
            "message": "视频推理及渲染已完成"
        }

inference_service = InferenceService()
