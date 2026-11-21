"""Read-only pre-flight health check. Never changes anything on the device."""

from __future__ import annotations

import re

from ..backup import store
from ..inventory.classifier import PROTECTED, Classifier
from ..inventory.scanner import ENABLED, device_info, dynamic_protected, scan

OK, WARN, CRIT = "OK", "AMARAN", "KRITIKAL"
EXPECTED_MODEL = "ELN-L09"


def _safe(runner, cmd: list[str]) -> str:
    try:
        return runner.run(cmd)
    except Exception:
        return ""


def run_checks(runner) -> list[tuple[str, str, str]]:
    """Return [(status, check name, detail)]."""
    r: list[tuple[str, str, str]] = []

    uid = _safe(runner, ["id"]).strip()
    if "uid=2000" in uid or "uid=0" in uid:
        r.append((OK, "Akses shell", uid.split()[0] if uid else ""))
    else:
        r.append((CRIT, "Akses shell", "Tidak bersambung ke tablet. Jalankan 'hdm sambung' "
                  "(ADB) atau mulakan Shizuku (rish) dahulu."))
        return r

    info = device_info(runner)
    if info.get("model") == EXPECTED_MODEL:
        r.append((OK, "Model", f"{info['model']} · Android {info.get('android')} · {info.get('build')}"))
    else:
        r.append((WARN, "Model", f"{info.get('model') or '?'} - bukan {EXPECTED_MODEL}. Senarai pakej mungkin tidak tepat."))
    if info.get("android") and info["android"] != "13":
        r.append((WARN, "Versi Android", f"Android {info['android']} (dirancang untuk Android 13)"))

    bat = _safe(runner, ["dumpsys", "battery"])
    m = re.search(r"^\s*level:\s*(\d+)", bat, re.M)
    if m:
        level = int(m.group(1))
        charging = bool(re.search(r"(AC|USB|Wireless) powered:\s*true", bat))
        detail = f"{level}%" + (" · mengecas" if charging else "")
        r.append((OK if level >= 50 or charging else WARN, "Bateri",
                  detail if level >= 50 or charging else detail + " - cas sekurang-kurangnya 50% dahulu"))
    tm = re.search(r"^\s*temperature:\s*(\d+)", bat, re.M)
    if tm:
        t = int(tm.group(1)) / 10
        r.append((OK if t < 42 else WARN, "Suhu bateri", f"{t:.1f}°C" + ("" if t < 42 else " - tablet panas")))

    df = _safe(runner, ["df", "-k", "/data"]).strip().splitlines()
    if len(df) >= 2:
        try:
            free_mb = int(df[-1].split()[3]) // 1024
            used_pct = df[-1].split()[4]
            st = OK if free_mb >= 4096 else WARN if free_mb >= 1024 else CRIT
            r.append((st, "Storan dalaman", f"{free_mb / 1024:.1f} GB kosong ({used_pct} digunakan)"
                      + ("" if st == OK else " - storan hampir penuh, ini punca biasa tablet perlahan")))
        except (IndexError, ValueError):
            pass

    mem = _safe(runner, ["cat", "/proc/meminfo"])
    tot = re.search(r"MemTotal:\s*(\d+)", mem)
    avail = re.search(r"MemAvailable:\s*(\d+)", mem)
    if tot and avail:
        t, a = int(tot.group(1)) / 1048576, int(avail.group(1)) / 1048576
        r.append((OK if a / t >= 0.15 else WARN, "RAM", f"{a:.1f} / {t:.1f} GB tersedia"))
    swap = re.search(r"SwapTotal:\s*(\d+)", mem)
    if swap and int(swap.group(1)) > 0:
        r.append((OK, "RAM Turbo / swap", f"{int(swap.group(1)) / 1048576:.1f} GB"))

    up = _safe(runner, ["cat", "/proc/uptime"]).split()
    if up:
        days = float(up[0]) / 86400
        r.append((OK if days < 7 else WARN, "Masa sejak reboot",
                  f"{days:.1f} hari" + ("" if days < 7 else " - pertimbangkan reboot (Shizuku perlu dihidupkan semula)")))

    mode = _safe(runner, ["settings", "get", "global", "private_dns_mode"]).strip()
    spec = _safe(runner, ["settings", "get", "global", "private_dns_specifier"]).strip()
    if mode == "hostname" and spec and "adguard" not in spec:
        r.append((WARN, "Private DNS", f"Sudah guna '{spec}'. 'hdm dns on' akan menggantikannya (boleh dipulihkan)."))
    else:
        r.append((OK, "Private DNS", f"{mode or 'lalai'}" + (f" · {spec}" if spec and spec != "null" else "")))
    vpn = _safe(runner, ["settings", "get", "secure", "always_on_vpn_app"]).strip()
    if vpn and vpn != "null":
        r.append((WARN, "VPN sentiasa aktif", f"{vpn} - VPN/penyekat lain boleh bertembung dengan Private DNS"))

    pkgs = scan(runner)
    clf = Classifier(dynamic_protected=dynamic_protected(runner))
    broken = [p for p in pkgs.values() if clf.classify(p).category == PROTECTED and p.state != ENABLED]
    for p in broken:
        r.append((CRIT, "Pakej penting tidak aktif", f"{p.id} ({p.state}) - pulihkan: hdm restore {p.id}"))
    changed = [p for p in pkgs.values() if p.system and p.state != ENABLED and p not in broken]
    if changed:
        names = ", ".join(p.id for p in changed[:8]) + (" ..." if len(changed) > 8 else "")
        r.append((WARN, "Pakej sistem sudah diubah sebelum ini", f"{len(changed)}: {names}"))
    else:
        r.append((OK, "Pakej sistem", f"{len(pkgs)} pakej, tiada yang di-disable sebelum ini"))

    snaps = store.list_snapshots()
    r.append((OK if snaps else WARN, "Backup asal",
              f"{len(snaps)} snapshot" if snaps else "Belum ada. Jalankan: hdm backup --label asal"))
    return r


def summarize(results) -> str:
    counts = {s: sum(1 for x in results if x[0] == s) for s in (OK, WARN, CRIT)}
    if counts[CRIT]:
        verdict = "JANGAN teruskan dulu - betulkan isu KRITIKAL."
    elif counts[WARN]:
        verdict = "Boleh teruskan dengan berhati-hati - baca AMARAN di atas."
    else:
        verdict = "Tablet sedia. Mula dengan: hdm backup --label asal"
    return f"{counts[OK]} OK · {counts[WARN]} amaran · {counts[CRIT]} kritikal\n{verdict}"
