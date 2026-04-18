from __future__ import annotations

import os
import subprocess
import sys
import argparse

from yoloservice.config.settings import settings


def main() -> int:
    if os.name == "nt":
        print("[ERROR] Gunicorn is not supported on Windows. Please use `dev.py`.")
        return 1

    if settings.DEBUG is True:
        print("=" * 60)
        print("[CRITICAL ERROR] Production server cannot start with DEBUG=True!")
        print("Please set DEBUG=False in .env")
        print("=" * 60)
        return 1

    parser = argparse.ArgumentParser(description="Run PROD server (gunicorn + uvicorn worker).")
    parser.add_argument("--workers", type=int, default=settings.WORKERS)
    parser.add_argument("--bind", default=f"{settings.API_HOST}:{settings.API_PORT}")
    args = parser.parse_args()

    print("[INFO] Security check passed: DEBUG is False.")

    settings.LOG.LOG_DIR.mkdir(parents=True, exist_ok=True)
    pid_file = settings.LOG.LOG_DIR / "gunicorn.pid"

    cmd = [
        sys.executable, "-m", "gunicorn",
        "yoloservice.main:app",
        "--workers", str(args.workers),
        "--worker-class", "uvicorn.workers.UvicornWorker",
        "--bind", args.bind,
        "--pid", str(pid_file),
        "--daemon",
        "--timeout", "120",
        "--max-requests", "2000",
        "--max-requests-jitter", "200",
        "--access-logfile", "-",
        "--error-logfile", str(settings.LOG.LOG_DIR / "gunicorn_error.log"),
        "--log-level", "info",
        "--chdir", str(settings.SRC_DIR),
    ]

    env = os.environ.copy()
    env["PYTHONPATH"] = str(settings.BASE_DIR) + os.pathsep + env.get("PYTHONPATH", "")
    env["DEBUG"] = "False"

    print("-" * 60)
    print("Starting PROD Server (Gunicorn + UvicornWorker)")
    print(f"Bind     : http://{args.bind}")
    print(f"Workers  : {args.workers}")
    print(f"PID File : {pid_file}")
    print("-" * 60)

    try:
        subprocess.run(cmd, check=True, env=env, cwd=str(settings.BASE_DIR))
        print("[OK] Server started in daemon mode.")
        return 0
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] Gunicorn failed to start. Check {settings.LOG.LOG_DIR}/gunicorn_error.log")
        return e.returncode


if __name__ == "__main__":
    raise SystemExit(main())
