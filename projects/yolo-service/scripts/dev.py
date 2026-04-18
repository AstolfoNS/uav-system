from __future__ import annotations

import os
import subprocess
import sys
import argparse

from yoloservice.config.settings import settings


def main() -> int:
    parser = argparse.ArgumentParser(description="Run DEV server (uvicorn).")
    parser.add_argument("--host", default=settings.API_HOST)
    parser.add_argument("--port", type=int, default=settings.API_PORT)
    parser.add_argument("--reload", action="store_true", help="Force reload mode")
    parser.add_argument("--workers", type=int, default=settings.WORKERS, help="Force workers mode (reload disabled)")
    args = parser.parse_args()

    target_app = "yoloservice.main:app"

    cmd = [
        sys.executable, "-m", "uvicorn",
        target_app,
        "--host", args.host,
        "--port", str(args.port),
        "--app-dir", str(settings.SRC_DIR),
    ]

    # reload/workers 互斥：优先级：显式参数 > settings.DEBUG 默认策略
    if args.reload:
        cmd.append("--reload")
        worker_info = "Reload Enabled (Single Worker)"
        mode = "DEBUG/RELOAD"
    else:
        if settings.DEBUG:
            cmd.append("--reload")
            worker_info = "Reload Enabled (Single Worker)"
            mode = "DEBUG/RELOAD"
        else:
            cmd.extend(["--workers", str(args.workers)])
            worker_info = f"Workers: {args.workers}"
            mode = "PRODUCTION/WORKERS"

    env = os.environ.copy()
    # 统一只加 BASE_ROOT，避免 root/src 双来源混乱
    env["PYTHONPATH"] = str(settings.BASE_DIR) + os.pathsep + env.get("PYTHONPATH", "")
    env["DEBUG"] = str(settings.DEBUG)

    print("=" * 60)
    print(f"Starting {settings.PROJECT_NAME} [DEV] Server")
    print("-" * 60)
    print(f"URL      : http://{args.host}:{args.port}")
    print(f"Mode     : {mode}")
    print(f"Process  : {worker_info}")
    print(f"App Dir  : {settings.SRC_DIR}")
    print(f"Log File : {settings.LOG.LOG_DIR / settings.LOG.LOG_FILENAME}")
    print("=" * 60)

    try:
        return subprocess.run(cmd, env=env, cwd=str(settings.BASE_DIR)).returncode
    except KeyboardInterrupt:
        print("\n[INFO] Development server stopped by user.")
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
