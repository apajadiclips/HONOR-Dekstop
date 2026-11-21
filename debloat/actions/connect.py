"""`hdm sambung`: guided Wireless debugging pairing + connect, no adb typing needed."""

from __future__ import annotations

import re
import shutil
import subprocess

from ..backup import store


def _sh(cmd: list[str], timeout: int = 60) -> str:
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return (p.stdout or "") + (p.stderr or "")
    except subprocess.TimeoutExpired:
        return "timeout"


def parse_target(text: str, default_host: str = "localhost") -> tuple[str, int] | None:
    """Accept '41235', ':41235', '192.168.1.4:41235' or 'IP address & Port 192.168.1.4:41235'."""
    text = text.strip()
    m = re.search(r"(\d{1,3}(?:\.\d{1,3}){3}|localhost)?\s*:?\s*(\d{2,5})\s*$", text)
    if not m:
        return None
    port = int(m.group(2))
    if not 1 <= port <= 65535:
        return None
    return (m.group(1) or default_host, port)


def connected_devices(sh=_sh) -> list[str]:
    out = sh(["adb", "devices"])
    return [l.split()[0] for l in out.splitlines()[1:] if l.strip().endswith("device")]


def discover_connect_port(sh=_sh) -> tuple[str, int] | None:
    """After pairing, Android advertises the connect port over mDNS; use it if visible."""
    out = sh(["adb", "mdns", "services"], timeout=15)
    m = re.search(r"_adb-tls-connect\._tcp\.?\s+(\d{1,3}(?:\.\d{1,3}){3}):(\d+)", out)
    return (m.group(1), int(m.group(2))) if m else None


def ensure_adb(say=print, sh=_sh) -> bool:
    if shutil.which("adb"):
        return True
    say("adb belum dipasang. Memasang android-tools (sekali sahaja)...")
    sh(["pkg", "install", "-y", "android-tools"], timeout=600)
    return shutil.which("adb") is not None


def run(ask=input, say=print, sh=_sh, which_ok=None) -> str | None:
    """Interactive flow. Returns the adb serial (host:port) on success, else None."""
    if which_ok is None:
        if not ensure_adb(say, sh):
            say("Gagal memasang adb. Jalankan sendiri: pkg install android-tools")
            return None
    elif not which_ok:
        return None

    existing = connected_devices(sh)
    if existing:
        say(f"Sudah bersambung: {existing[0]}")
        return existing[0]

    say("\nLANGKAH 1 - Pairing")
    say("Buka Settings > System & updates > Developer options > Wireless debugging")
    say("Tekan 'Pair device with pairing code'. Biarkan skrin itu terbuka.")
    say("(Petua: belah skrin - Termux di sebelah, Settings di sebelah lagi.)\n")

    host = "localhost"
    for attempt in range(3):
        target = None
        while target is None:
            target = parse_target(ask("Port pairing (nombor selepas ':' pada skrin pairing): "))
            if target is None:
                say("  Masukkan nombor port sahaja, contoh: 41235")
        host, pport = target
        code = re.sub(r"\D", "", ask("Kod pairing 6 digit: "))
        if len(code) != 6:
            say("  Kod mesti 6 digit. Cuba lagi.")
            continue
        out = sh(["adb", "pair", f"{host}:{pport}", code])
        if "Successfully paired" in out:
            say("  ✔ Pairing berjaya.")
            break
        say(f"  ✘ Pairing gagal: {out.strip().splitlines()[-1] if out.strip() else 'tiada jawapan'}")
        say("  Tutup dialog pairing, tekan 'Pair device with pairing code' semula untuk kod baru.")
    else:
        say("Pairing gagal 3 kali. Pastikan Wi-Fi bersambung dan cuba lagi nanti.")
        return None

    say("\nLANGKAH 2 - Sambung")
    found = discover_connect_port(sh)
    targets = []
    if found:
        targets.append((host if host == "localhost" else found[0], found[1]))
        say(f"  Port utama dikesan secara automatik: {found[1]}")
    for attempt in range(3):
        if not targets:
            say("Tutup dialog pairing. Lihat skrin utama Wireless debugging, bahagian 'IP address & Port'.")
            t = parse_target(ask("Port utama: "), default_host=host)
            if t is None:
                say("  Masukkan nombor port sahaja, contoh: 38757")
                continue
            targets.append(t)
        h, p = targets.pop(0)
        serial = f"{h}:{p}"
        out = sh(["adb", "connect", serial])
        if "connected to" in out and "unable" not in out and "failed" not in out:
            if serial in connected_devices(sh):
                say(f"  ✔ Bersambung ke {serial}")
                store.write_json(store.data_dir() / "adb-target.json", {"serial": serial})
                return serial
            say("  Tablet minta kebenaran. Tekan 'Allow' pada pop-up, kemudian cuba lagi.")
        else:
            say(f"  ✘ Gagal sambung: {out.strip().splitlines()[-1] if out.strip() else 'tiada jawapan'}")
    say("Tidak dapat bersambung. Semak port utama dan cuba 'hdm sambung' sekali lagi.")
    return None


def saved_serial() -> str | None:
    import json
    path = store.data_dir() / "adb-target.json"
    if path.exists():
        try:
            return json.loads(path.read_text()).get("serial")
        except (ValueError, OSError):
            return None
    return None
