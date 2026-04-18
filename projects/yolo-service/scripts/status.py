from __future__ import annotations

import argparse
import os
import subprocess

from yoloservice.config.settings import settings


def get_process_info_nt(pid: str) -> str:
    """Windows 下根据 PID 获取进程名称"""
    try:
        output = subprocess.check_output(
            f'tasklist /FI "PID eq {pid}" /NH',
            shell=True,
            text=True,
        )
        if "No tasks" in output or not output.strip():
            return "Unknown"
        return output.strip().split()[0]
    except Exception as e:
        print(f"Error: {e}")
        return "Unknown"


def get_processes_by_port(port: int) -> list[str]:
    """根据端口号查找进程信息（跨平台）"""
    if os.name == "nt":
        try:
            cmd = f'netstat -aon | findstr /R /C:".*:{port} .*LISTENING"'
            output = subprocess.check_output(cmd, shell=True, text=True)

            pids: set[str] = set()
            for line in output.strip().splitlines():
                parts = line.split()
                if len(parts) >= 5 and f":{port}" in parts[1]:
                    pids.add(parts[-1])
            return sorted(pids)
        except subprocess.CalledProcessError:
            return []
    else:
        try:
            output = subprocess.check_output(["lsof", "-t", "-i", f":{port}"], text=True)
            return [x for x in output.strip().splitlines() if x.strip()]
        except (subprocess.CalledProcessError, FileNotFoundError):
            return []


def status(port: int) -> None:
    print("=" * 60)
    print(f"Service Status Explorer (Port: {port})")
    print("=" * 60)

    pids = get_processes_by_port(port)

    if not pids:
        print(f"STATUS: [\033[31mSTOPPED\033[0m] No active process listening on port {port}")
    else:
        print(f"STATUS: [\033[32mRUNNING\033[0m] Found {len(pids)} process(es) holding the port")
        print("-" * 60)
        print(f"{'PID':<10} {'Process Name':<25} {'Action'}")

        for pid in pids:
            if os.name == "nt":
                name = get_process_info_nt(pid)
                print(f"{pid:<10} {name:<25} taskkill /F /T /PID {pid}")
            else:
                try:
                    name = subprocess.check_output(["ps", "-p", pid, "-o", "comm="], text=True).strip()
                except Exception as e:
                    print(f"Error: {e}")
                    name = "Unknown"
                print(f"{pid:<10} {name:<25} kill -9 {pid}")

    print("=" * 60)
    if pids and os.name == "nt":
        print("[NOTE] On Windows, killing the Master PID usually stops all workers.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Show service status by port.")
    parser.add_argument("--port", type=int, default=settings.API_PORT, help="Port to inspect (default: settings.API_PORT)")
    args = parser.parse_args()
    status(args.port)


if __name__ == "__main__":
    main()
