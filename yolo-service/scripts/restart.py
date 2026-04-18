from __future__ import annotations

import subprocess
import sys
import time
import os

from yoloservice.config.settings import settings


def _resolve_python_executable() -> str:
    """优先使用项目内虚拟环境解释器，避免外部环境污染。"""
    if os.name == "nt":
        venv_python = settings.BASE_DIR / ".venv" / "Scripts" / "python.exe"
    else:
        venv_python = settings.BASE_DIR / ".venv" / "bin" / "python"

    if venv_python.exists():
        return str(venv_python)
    return sys.executable


def _run_script(script_name: str) -> int:
    script_path = settings.BASE_DIR / "scripts" / f"{script_name}.py"
    if not script_path.exists():
        print(f"[FAIL] Script not found: {script_path}")
        return 2

    python_exec = _resolve_python_executable()

    return subprocess.run(
        [python_exec, str(script_path)],
        cwd=str(settings.BASE_DIR),
    ).returncode


def restart() -> int:
    """
    一键重启服务：先停后启
    """
    print("-" * 60)
    print("SERVICE RESTART SEQUENCE")
    print("-" * 60)

    print("[1/2] Stopping existing processes...")
    rc_stop = _run_script("stop")
    if rc_stop != 0:
        print(f"[WARN] stop exited with code {rc_stop} (continue restarting)")

    time.sleep(1)

    print("[2/2] Starting service...")
    rc_start = _run_script("start")
    if rc_start != 0:
        print(f"\n[FAIL] Failed to start service (exit code {rc_start})")
        return rc_start

    print("[OK] Restart completed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(restart())
    except KeyboardInterrupt:
        print("\n[INFO] Restart sequence aborted by user.")
        raise SystemExit(130)
