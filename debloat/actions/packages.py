"""Disable / uninstall / restore exactly one package at a time, with journal + verification."""

from __future__ import annotations

from ..backup import store
from ..inventory.classifier import PROTECTED, REVIEW, USER, Classifier
from ..inventory.scanner import DISABLED, ENABLED, UNINSTALLED, Package, scan


class ActionRefused(RuntimeError):
    pass


def _check_output(out: str) -> None:
    low = out.lower()
    if "failure" in low or "exception" in low or "error" in low:
        raise ActionRefused(f"Sistem menolak arahan: {out.strip()}")


def _lookup(runner, pkg_id: str) -> Package:
    pkgs = scan(runner)
    if pkg_id not in pkgs:
        raise ActionRefused(f"Pakej '{pkg_id}' tiada pada peranti ini.")
    return pkgs[pkg_id]


def guard(pkg: Package, classifier: Classifier, reviewed: bool) -> None:
    c = classifier.classify(pkg)
    if c.category == PROTECTED:
        raise ActionRefused(f"{pkg.id} DILINDUNGI (Kategori C: {c.label}). Tindakan ditolak.")
    if c.category == USER:
        raise ActionRefused(f"{pkg.id} ialah aplikasi pengguna. Buang melalui Settings seperti biasa.")
    if c.category == REVIEW and not reviewed:
        raise ActionRefused(
            f"{pkg.id} ialah Kategori B ({c.label}).\n"
            "Semak fungsinya dahulu, kemudian ulang dengan --reviewed."
        )


def _do(runner, pkg_id: str, action: str, cmds: list[list[str]], expect: str,
        snapshot_path: str | None) -> Package:
    before = _lookup(runner, pkg_id)
    out = ""
    try:
        for cmd in cmds:
            out = runner.run(cmd)
            _check_output(out)
        after = _lookup(runner, pkg_id)
        ok = after.state == expect
    except Exception as e:
        store.append_jsonl("journal.jsonl", {"package": pkg_id, "action": action,
                                             "before": before.state, "after": None,
                                             "ok": False, "error": str(e),
                                             "snapshot": snapshot_path})
        raise
    store.append_jsonl("journal.jsonl", {"package": pkg_id, "action": action,
                                         "before": before.state, "after": after.state,
                                         "ok": ok, "output": out.strip(),
                                         "snapshot": snapshot_path})
    if not ok:
        raise ActionRefused(f"Selepas {action}, status {pkg_id} ialah '{after.state}', "
                            f"bukan '{expect}'.")
    return after


def disable(runner, pkg_id: str, classifier: Classifier, reviewed: bool = False,
            snapshot_path: str | None = None) -> Package:
    pkg = _lookup(runner, pkg_id)
    guard(pkg, classifier, reviewed)
    if pkg.state != ENABLED:
        raise ActionRefused(f"{pkg_id} sudah '{pkg.state}'.")
    return _do(runner, pkg_id, "disable",
               [["pm", "disable-user", "--user", "0", pkg_id]], DISABLED, snapshot_path)


def uninstall(runner, pkg_id: str, classifier: Classifier, reviewed: bool = False,
              snapshot_path: str | None = None) -> Package:
    pkg = _lookup(runner, pkg_id)
    guard(pkg, classifier, reviewed)
    if pkg.state != DISABLED:
        raise ActionRefused(
            f"{pkg_id} mesti di-disable dan diuji dahulu sebelum uninstall "
            f"(status semasa: '{pkg.state}'). Jalankan: hdm disable {pkg_id}"
        )
    return _do(runner, pkg_id, "uninstall",
               [["pm", "uninstall", "-k", "--user", "0", pkg_id]], UNINSTALLED, snapshot_path)


def restore(runner, pkg_id: str, target: str = ENABLED,
            snapshot_path: str | None = None) -> Package:
    """Bring a package back. Restore is always allowed (it only undoes changes)."""
    pkg = _lookup(runner, pkg_id)
    if pkg.state == target:
        return pkg
    cmds: list[list[str]] = []
    if pkg.state == UNINSTALLED:
        cmds.append(["cmd", "package", "install-existing", "--user", "0", pkg_id])
    if target == ENABLED:
        cmds.append(["pm", "enable", "--user", "0", pkg_id])
    elif target == DISABLED and pkg.state != DISABLED:
        cmds.append(["pm", "disable-user", "--user", "0", pkg_id])
    return _do(runner, pkg_id, "restore", cmds, target, snapshot_path)
