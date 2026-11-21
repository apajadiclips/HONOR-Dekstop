"""Private DNS ad blocking (AdGuard Public DNS) with backup and an off switch."""

from __future__ import annotations

import json

from ..backup import store

ADGUARD = "dns.adguard-dns.com"


def status(runner) -> dict:
    def get(key):
        v = runner.run(["settings", "get", "global", key]).strip()
        return None if v in ("", "null") else v
    return {"mode": get("private_dns_mode"), "specifier": get("private_dns_specifier")}


def _put(runner, key: str, value: str | None) -> None:
    if value is None:
        runner.run(["settings", "delete", "global", key])
    else:
        runner.run(["settings", "put", "global", key, value])


def enable(runner, hostname: str = ADGUARD) -> dict:
    current = status(runner)
    backup = store.data_dir() / "dns-backup.json"
    # Keep the oldest backup: don't overwrite the user's real settings with ours.
    if not backup.exists() and current.get("specifier") != hostname:
        store.write_json(backup, current)
    _put(runner, "private_dns_mode", "hostname")
    _put(runner, "private_dns_specifier", hostname)
    store.append_jsonl("journal.jsonl", {"dns": "on", "before": current, "hostname": hostname})
    return status(runner)


def disable(runner) -> dict:
    """Restore the DNS settings saved before ad blocking (default: automatic)."""
    backup = store.data_dir() / "dns-backup.json"
    if backup.exists():
        with open(backup, encoding="utf-8") as f:
            prev = json.load(f)
    else:
        prev = {"mode": "opportunistic", "specifier": None}
    before = status(runner)
    _put(runner, "private_dns_mode", prev.get("mode") or "opportunistic")
    _put(runner, "private_dns_specifier", prev.get("specifier"))
    store.append_jsonl("journal.jsonl", {"dns": "off", "before": before, "restored": prev})
    if backup.exists():
        backup.unlink()
    return status(runner)
