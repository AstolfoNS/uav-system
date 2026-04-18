from __future__ import annotations

from pathlib import Path
import argparse

import pathspec

from yoloservice.config.settings import settings


# 基础忽略清单（即使没有 .gitignore 也会生效）
DEFAULT_IGNORES = {".git", ".venv", ".idea", "__pycache__", ".vscode", ".pytest_cache", ".DS_Store"}


def load_gitignore(root: Path) -> pathspec.PathSpec:
    """加载并解析 .gitignore 规则"""
    ignore_file = root / ".gitignore"
    lines: list[str] = []
    if ignore_file.exists():
        with ignore_file.open("r", encoding="utf-8") as f:
            lines = [line.strip() for line in f if line.strip() and not line.startswith("#")]
    return pathspec.PathSpec.from_lines("gitwildmatch", lines)


class TreeGenerator:
    def __init__(self, root_path: Path):
        self.root = root_path
        self.spec = load_gitignore(root_path)
        self.dir_count = 0
        self.file_count = 0

    def is_ignored(self, path: Path) -> bool:
        """判断路径是否应被忽略"""
        if path.name in DEFAULT_IGNORES:
            return True
        try:
            rel_path = path.relative_to(self.root)
            match_str = str(rel_path) + ("/" if path.is_dir() else "")
            return self.spec.match_file(match_str)
        except ValueError:
            return False

    def print_tree(self, current_path: Path, prefix: str = "", is_last: bool = True, is_root: bool = True):
        """递归打印目录树"""
        if is_root:
            print(f"{current_path.name}/")
        else:
            connector = "└── " if is_last else "├── "
            print(f"{prefix}{connector}{current_path.name}{'/' if current_path.is_dir() else ''}")

        if not current_path.is_dir():
            self.file_count += 1
            return

        if not is_root:
            self.dir_count += 1

        try:
            entries = sorted(
                [e for e in current_path.iterdir() if not self.is_ignored(e)],
                key=lambda e: (e.is_file(), e.name.lower())
            )
        except PermissionError:
            return

        count = len(entries)
        for i, entry in enumerate(entries):
            new_last = (i == count - 1)
            new_prefix = prefix if is_root else prefix + ("    " if is_last else "│   ")
            self.print_tree(entry, new_prefix, new_last, is_root=False)


def main() -> None:
    parser = argparse.ArgumentParser(description="Print project tree.")
    parser.add_argument("--root", type=Path, default=settings.BASE_DIR, help="Root directory to print (default: project root)")
    args = parser.parse_args()

    root = args.root.resolve()

    print("-" * 60)
    print(f"Project Structure: {root.name}")
    print("-" * 60)

    gen = TreeGenerator(root)
    gen.print_tree(root)

    print("-" * 60)
    print(f"Summary: {gen.dir_count} directories, {gen.file_count} files")
    print("-" * 60)


if __name__ == "__main__":
    main()
