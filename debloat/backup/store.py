"""Snapshots (inventory export), change journal and restore manifest.

Layout under the data directory (default ~/honor-debloat, override with HDM_HOME):
  snapshots/<timestamp>-<label>.json   full device inventory + DNS settings
  journal.jsonl                        one line per change made by this tool
  dns-backup.json                      DNS settings before ad blocking was turned on
  measurements.jsonl                   RAM / storage / battery readings
"""

from __future__ import annotations

import json
import os
import time
from pathlib import Path


def data_dir() -> Path:
    d = Path(os.environ.get("HDM_HOME", Path.home() / "honor-debloat"))
    (d / "snapshots").mkdir(parents=True, exist_ok=True)
    return d


def _stamp() -> str:
    return time.strftime("%Y%m%d-%H%M%S")


def write_json(path: Path, obj) -> Path:
    tmp = path.with_suffix(path.suffix + ".tmp")
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.flush()
        os.fsync(f.fileno())
    os.replace(tmp, path)
    return path


def save_snapshot(snapshot: dict, label: str) -> Path:
    d = data_dir() / "snapshots"
    path = d / f"{_stamp()}-{label}.json"
    n = 1
    while path.exists():
        n += 1
        path = d / f"{_stamp()}-{label}-{n}.json"
    return write_json(path, snapshot)


def list_snapshots() -> list[Path]:
    return sorted((data_dir() / "snapshots").glob("*.json"))


def append_jsonl(name: str, entry: dict) -> None:
    entry = {"time": time.strftime("%Y-%m-%dT%H:%M:%S"), **entry}
    with open(data_dir() / name, "a", encoding="utf-8") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")
        f.flush()
        os.fsync(f.fileno())


def read_jsonl(name: str) -> list[dict]:
    path = data_dir() / name
    if not path.exists():
        return []
    with open(path, encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def original_states() -> dict[str, str]:
    """State of each package before this tool first touched it."""
    states: dict[str, str] = {}
    for e in read_jsonl("journal.jsonl"):
        if "package" in e and e.get("ok"):
            states.setdefault(e["package"], e["before"])
    return states


def restore_manifest() -> list[str]:
    """Shell commands (for rish) that put every touched package back, newest change first."""
    cmds: list[str] = []
    seen: set[str] = set()
    originals = original_states()
    for e in reversed(read_jsonl("journal.jsonl")):
        pkg = e.get("package")
        if not pkg or not e.get("ok") or pkg in seen:
            continue
        seen.add(pkg)
        if e["after"] == "uninstalled":
            cmds.append(f"cmd package install-existing --user 0 {pkg}")
        if originals.get(pkg) == "enabled":
            cmds.append(f"pm enable --user 0 {pkg}")
    return cmds
