from pathlib import Path

BASE_ROOT: Path = Path(__file__).resolve().parents[2]

SRC_ROOT: Path = BASE_ROOT / "src"

PROJECT_ROOT: Path = Path(__file__).resolve().parent