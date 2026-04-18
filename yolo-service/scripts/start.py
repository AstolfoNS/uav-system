from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

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
        print(f"[ERROR] Cannot find script: {script_path}")
        return 2

    python_exec = _resolve_python_executable()

    return subprocess.run(
        [python_exec, str(script_path)],
        cwd=str(settings.BASE_DIR),
        check=False,
    ).returncode


def start() -> int:
    """
    智能启动入口：自动自检 -> 模式选择 -> 启动
    """
    print("Running pre-flight check...")
    check_rc = _run_script("check")
    if check_rc != 0:
        print("\n[ERROR] Environment check failed. Startup aborted.")
        return check_rc

    is_windows = os.name == "nt"
    mode = "dev" if is_windows or settings.DEBUG else "prod"

    print("-" * 60)
    print(f"STARTING IN {mode.upper()} MODE")
    print(f"Python: {_resolve_python_executable()}")
    print(f"Time: {Path(__file__).stat().st_mtime}")
    print("-" * 60)

    try:
        return _run_script(mode)
    except KeyboardInterrupt:
        print("\n[INFO] Startup sequence interrupted.")
        return 130


if __name__ == "__main__":
    raise SystemExit(start())
