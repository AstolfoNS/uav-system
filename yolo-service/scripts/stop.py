from __future__ import annotations

import argparse
import os
import subprocess

from yoloservice.config.settings import settings


def _pids_by_port_windows(port: int) -> list[str]:
    # 精确匹配 LISTENING 且本地地址包含 :port 的行
    cmd = f'netstat -aon | findstr /R /C:".*:{port} .*LISTENING"'
    try:
        output = subprocess.check_output(cmd, shell=True, text=True)
    except subprocess.CalledProcessError:
        return []

    pids: set[str] = set()
    for line in output.strip().splitlines():
        parts = line.split()
        # 常见格式：Proto LocalAddress ForeignAddress State PID
        if len(parts) >= 5 and f":{port}" in parts[1]:
            pids.add(parts[-1])
    return sorted(pids)


def _pids_by_port_unix(port: int) -> list[str]:
    try:
        output = subprocess.check_output(["lsof", "-t", "-i", f":{port}"], text=True)
        return [x for x in output.split() if x.strip()]
    except (subprocess.CalledProcessError, FileNotFoundError):
        return []


def stop(port: int) -> int:
    print(f"Stopping service on port {port}...")

    if os.name == "nt":
        pids = _pids_by_port_windows(port)
        if not pids:
            print(f"[INFO] No LISTENING process found on port {port}")
            return 0

        for pid in pids:
            # /T: kill child processes, /F: force
            subprocess.run(
                ["taskkill", "/F", "/T", "/PID", pid],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        print(f"[OK] Killed {len(pids)} process(es) on port {port}")
        return 0

    # Unix
    pids = _pids_by_port_unix(port)
    if pids:
        subprocess.run(["kill", "-9", *pids], check=False)
        print(f"[OK] Killed {len(pids)} process(es) on port {port}")
        return 0

    # 兜底：有些环境没有 lsof，用 pkill 尝试（可选）
    subprocess.run(["pkill", "-f", f"gunicorn.*{port}"], check=False)
    print(f"[OK] Cleanup finished for port {port}")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Stop service by port.")
    parser.add_argument("--port", type=int, default=settings.API_PORT, help="Port to stop (default: settings.API_PORT)")
    args = parser.parse_args()
    raise SystemExit(stop(args.port))


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[INFO] Aborted.")
        raise SystemExit(130)
