"""In-memory fake of the `pm` / `settings` shell commands used by the tool."""


class FakeDevice:
    def __init__(self, packages):
        # packages: {id: {"system": bool, "state": str, "path": str}}
        self.pkgs = {k: dict(v) for k, v in packages.items()}
        self.settings = {"private_dns_mode": "opportunistic"}
        self.calls = []

    def run(self, cmd):
        self.calls.append(cmd)
        if cmd[:3] == ["pm", "list", "packages"]:
            flags = set(cmd[3:])
            out = []
            for pid, p in sorted(self.pkgs.items()):
                if "-u" not in flags and p["state"] == "uninstalled":
                    continue
                if "-d" in flags and p["state"] != "disabled":
                    continue
                if "-e" in flags and p["state"] != "enabled":
                    continue
                if "-s" in flags and not p["system"]:
                    continue
                out.append(f"package:{p['path']}={pid}" if "-f" in flags else f"package:{pid}")
            return "\n".join(out) + "\n"
        if cmd[:2] == ["pm", "disable-user"]:
            self.pkgs[cmd[-1]]["state"] = "disabled"
            return f"Package {cmd[-1]} new state: disabled-user\n"
        if cmd[:2] == ["pm", "enable"]:
            self.pkgs[cmd[-1]]["state"] = "enabled"
            return f"Package {cmd[-1]} new state: enabled\n"
        if cmd[:2] == ["pm", "uninstall"]:
            self.pkgs[cmd[-1]]["state"] = "uninstalled"
            return "Success\n"
        if cmd[:3] == ["cmd", "package", "install-existing"]:
            p = self.pkgs[cmd[-1]]
            if p["state"] == "uninstalled":
                p["state"] = "disabled"  # comes back with its old enabled-state
            return f"Package {cmd[-1]} installed for user: 0\n"
        if cmd[:3] == ["cmd", "package", "resolve-activity"]:
            return "priority=0 preferredOrder=0\ncom.hihonor.android.launcher/.Launcher\n"
        if cmd[:2] == ["settings", "get"]:
            if cmd[3] == "default_input_method":
                return "com.example.keyboard/.IME\n"
            return f"{self.settings.get(cmd[3], 'null')}\n"
        if cmd[:2] == ["settings", "put"]:
            self.settings[cmd[3]] = cmd[4]
            return ""
        if cmd[:2] == ["settings", "delete"]:
            self.settings.pop(cmd[3], None)
            return ""
        if cmd[:2] == ["dumpsys", "battery"]:
            return "  AC powered: false\n  USB powered: true\n  level: 33\n  temperature: 310\n"
        if cmd[:3] == ["df", "-k", "/data"]:
            return "Filesystem 1K-blocks Used Available Use% Mounted\n/dev/block/dm-5 110000000 100000000 10000000 91% /data\n"
        if cmd[:2] == ["cat", "/proc/meminfo"]:
            return "MemTotal: 3800000 kB\nMemAvailable: 1200000 kB\nSwapTotal: 2097152 kB\n"
        if cmd[:2] == ["cat", "/proc/uptime"]:
            return "90000.5 120000.0\n"
        if cmd[0] == "getprop":
            return {"ro.product.model": "ELN-L09", "ro.build.version.release": "13"}.get(cmd[1], "") + "\n"
        if cmd[0] == "id":
            return "uid=2000(shell) gid=2000(shell)\n"
        raise AssertionError(f"unexpected command {cmd}")


def sample_device():
    return FakeDevice({
        "com.android.systemui": {"system": True, "state": "enabled", "path": "/system_ext/priv-app/SystemUI/SystemUI.apk"},
        "com.hihonor.android.launcher": {"system": True, "state": "enabled", "path": "/system/priv-app/Launcher/Launcher.apk"},
        "com.facebook.appmanager": {"system": True, "state": "enabled", "path": "/product/app/FBAppManager/FBAppManager.apk"},
        "com.hihonor.appmarket": {"system": True, "state": "enabled", "path": "/system/app/AppMarket/AppMarket.apk"},
        "com.android.theme.font": {"system": True, "state": "enabled", "path": "/product/overlay/Font.apk"},
        "com.vendor.someoverlay": {"system": True, "state": "enabled", "path": "/vendor/overlay/X.apk"},
        "com.example.keyboard": {"system": True, "state": "enabled", "path": "/system/app/Kb/Kb.apk"},
        "com.whatsapp": {"system": False, "state": "enabled", "path": "/data/app/~~x/com.whatsapp/base.apk"},
        "com.mystery.preload": {"system": True, "state": "enabled", "path": "/preload/app/M/M.apk"},
    })
