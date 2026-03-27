from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

# 确保这里的 yoloservice 是已经安装或者在路径中的
from yoloservice import BASE_ROOT


def _script_dir() -> Path:
    return BASE_ROOT / "scripts"


def available_scripts() -> list[str]:
    d = _script_dir()
    if not d.exists():
        return []
    # 过滤掉 __init__.py 并只匹配 .py 文件
    return sorted(p.stem for p in d.glob("*.py") if p.is_file() and p.stem != "__init__")


def _run_script(script_name: str, extra_args: list[str]) -> int:
    root = BASE_ROOT
    script_path = root / "scripts" / f"{script_name}.py"

    if not script_path.exists():
        print(f"[ERROR] 找不到脚本: {script_path}")
        return 2

    env = os.environ.copy()

    # --- 核心修正：src-layout 下的导入路径 ---
    # 为了让 scripts 里的脚本能成功 `import yoloservice`
    # 我们需要把 src 目录和根目录都塞进去
    src_dir = str(root / "src")
    env["PYTHONPATH"] = os.pathsep.join([src_dir, str(root), env.get("PYTHONPATH", "")])

    try:
        # 使用 sys.executable 确保使用当前虚拟环境的 Python
        result = subprocess.run(
            [sys.executable, str(script_path), *extra_args],
            cwd=str(root),
            env=env,
            check=False,
        )
        return result.returncode
    except KeyboardInterrupt:
        print("\n[INFO] 用户中断执行。")
        return 130


def _build_parser() -> argparse.ArgumentParser:
    scripts = available_scripts()

    parser = argparse.ArgumentParser(
        prog="ys",
        description="YOLO SERVICE 管理工具 (运行 scripts/*.py)",
        formatter_class=argparse.RawTextHelpFormatter,
    )

    parser.add_argument(
        "--list",
        action="store_true",
        help="列出所有可用脚本",
    )

    # 修正 dest 为 "script"，保持单数统一
    subparsers = parser.add_subparsers(dest="script", metavar="<script>")

    for s in scripts:
        sp = subparsers.add_parser(
            s,
            help=f"运行 scripts/{s}.py",
            add_help=True,
        )
        # 允许透传任何参数
        sp.add_argument(
            "args",
            nargs=argparse.REMAINDER,
            help="透传参数，建议格式: ys <script> -- [args...]",
        )

    return parser


def main() -> int:
    parser = _build_parser()
    argv = sys.argv[1:]

    if not argv:
        parser.print_help()
        return 0

    ns = parser.parse_args(argv)

    if ns.list:
        scripts = available_scripts()
        if not scripts:
            print("没有找到可用脚本。")
        else:
            print("可用脚本列表:")
            for s in scripts:
                print(f"  - {s}")
        return 0

    # 检查是否选择了子命令
    if not ns.script:
        parser.print_help()
        return 0

    # 处理透传参数
    extra = getattr(ns, "args", []) or []
    if extra and extra[0] == "--":
        extra = extra[1:]

    return _run_script(ns.script, extra)


if __name__ == "__main__":
    # 规范的退出方式
    sys.exit(main())
