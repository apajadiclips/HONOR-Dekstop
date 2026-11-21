"""Before/after performance readings: RAM, storage and battery."""

from __future__ import annotations

import re


def _meminfo(runner) -> dict:
    out = runner.run(["dumpsys", "meminfo"])
    result = {}
    for key in ("Total RAM", "Free RAM", "Used RAM", "Lost RAM"):
        m = re.search(rf"^\s*{key}:\s*([\d,]+)K", out, re.M)
        if m:
            result[key.lower().replace(" ", "_") + "_mb"] = int(m.group(1).replace(",", "")) // 1024
    return result


def _storage(runner) -> dict:
    out = runner.run(["df", "-k", "/data"]).strip().splitlines()
    if len(out) < 2:
        return {}
    parts = out[-1].split()
    try:
        return {"data_used_mb": int(parts[2]) // 1024, "data_free_mb": int(parts[3]) // 1024}
    except (IndexError, ValueError):
        return {}


def _battery(runner) -> dict:
    out = runner.run(["dumpsys", "battery"])
    m = re.search(r"^\s*level:\s*(\d+)", out, re.M)
    return {"battery_level": int(m.group(1))} if m else {}


def _package_counts(runner) -> dict:
    out = runner.run(["pm", "list", "packages", "-e", "--user", "0"])
    return {"enabled_packages": sum(1 for l in out.splitlines() if l.startswith("package:"))}


def measure(runner) -> dict:
    data = {}
    for fn in (_meminfo, _storage, _battery, _package_counts):
        try:
            data.update(fn(runner))
        except Exception as e:  # one failing reading must not lose the rest
            data[f"{fn.__name__.strip('_')}_error"] = str(e)
    return data
