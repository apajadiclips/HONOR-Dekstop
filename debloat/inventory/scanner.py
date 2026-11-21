"""Reads the real package list and states from the device."""

from __future__ import annotations

from dataclasses import asdict, dataclass

ENABLED, DISABLED, UNINSTALLED = "enabled", "disabled", "uninstalled"


@dataclass
class Package:
    id: str
    system: bool
    state: str          # enabled | disabled | uninstalled (for user 0)
    path: str | None = None

    def to_dict(self) -> dict:
        return asdict(self)


def _parse_list(out: str) -> dict[str, str | None]:
    """Parse `pm list packages [-f]` output into {package_id: apk_path}."""
    result: dict[str, str | None] = {}
    for line in out.splitlines():
        line = line.strip()
        if not line.startswith("package:"):
            continue
        body = line[len("package:"):]
        if "=" in body:  # -f form: package:/path/base.apk=com.example
            path, pkg = body.rsplit("=", 1)
            result[pkg] = path
        else:
            result[body] = None
    return result


def scan(runner, user: int = 0) -> dict[str, Package]:
    u = ["--user", str(user)]
    everything = _parse_list(runner.run(["pm", "list", "packages", "-u", "-f", *u]))
    installed = set(_parse_list(runner.run(["pm", "list", "packages", *u])))
    disabled = set(_parse_list(runner.run(["pm", "list", "packages", "-d", *u])))
    system = set(_parse_list(runner.run(["pm", "list", "packages", "-s", "-u", *u])))

    pkgs: dict[str, Package] = {}
    for pkg, path in sorted(everything.items()):
        if pkg not in installed:
            state = UNINSTALLED
        elif pkg in disabled:
            state = DISABLED
        else:
            state = ENABLED
        pkgs[pkg] = Package(id=pkg, system=pkg in system, state=state, path=path)
    return pkgs


def _getprop(runner, key: str) -> str:
    try:
        return runner.run(["getprop", key]).strip()
    except Exception:
        return ""


def device_info(runner) -> dict:
    return {
        "model": _getprop(runner, "ro.product.model"),
        "build": _getprop(runner, "ro.build.display.id"),
        "android": _getprop(runner, "ro.build.version.release"),
        "soc": _getprop(runner, "ro.soc.model") or _getprop(runner, "ro.board.platform"),
    }


def dynamic_protected(runner) -> dict[str, str]:
    """Packages that must never be touched because the device is using them right now."""
    found: dict[str, str] = {}
    try:
        ime = runner.run(["settings", "get", "secure", "default_input_method"]).strip()
        if ime and ime != "null":
            found[ime.split("/", 1)[0]] = "Papan kekunci semasa"
    except Exception:
        pass
    try:
        out = runner.run(["cmd", "package", "resolve-activity", "--brief", "--user", "0",
                          "-a", "android.intent.action.MAIN",
                          "-c", "android.intent.category.HOME"])
        last = out.strip().splitlines()[-1] if out.strip() else ""
        if "/" in last:
            found[last.split("/", 1)[0]] = "Launcher semasa"
    except Exception:
        pass
    return found
