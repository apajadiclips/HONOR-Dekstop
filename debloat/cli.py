"""hdm - HONOR Debloat Manager command line (run inside Termux on the tablet)."""

from __future__ import annotations

import argparse
import json
import sys

from . import __version__
from .actions import connect, dns, doctor, measure, packages
from .backup import store
from .inventory.classifier import Classifier
from .inventory.scanner import ENABLED, device_info, dynamic_protected, scan
from .runner import AdbRunner, RishRunner, RunnerError

CATEGORY_NAMES = {"A": "Calon selamat", "B": "Perlu semakan", "C": "Dilindungi",
                  "user": "Aplikasi pengguna"}


def _classifier(runner) -> Classifier:
    return Classifier(dynamic_protected=dynamic_protected(runner))


def build_snapshot(runner) -> dict:
    clf = _classifier(runner)
    pkgs = scan(runner)
    rows = []
    for p in pkgs.values():
        c = clf.classify(p)
        rows.append({**p.to_dict(), "category": c.category, "label": c.label})
    try:
        dns_state = dns.status(runner)
    except RunnerError as e:
        dns_state = {"error": str(e)}
    return {"tool_version": __version__, "device": device_info(runner),
            "dns": dns_state, "packages": rows}


def take_snapshot(runner, label: str) -> str:
    path = store.save_snapshot(build_snapshot(runner), label)
    print(f"Backup disimpan: {path}")
    return str(path)


def _confirm(prompt: str, expected: str) -> bool:
    if not sys.stdin.isatty():
        print("Pengesahan memerlukan terminal interaktif. Tiada perubahan dibuat.")
        return False
    try:
        answer = input(prompt).strip()
    except EOFError:
        return False
    return answer == expected


# ---- commands -------------------------------------------------------------

def cmd_check(runner, args) -> int:
    uid = runner.run(["id"]).strip()
    print(uid)
    if "uid=2000" not in uid and "uid=0" not in uid:
        print("Amaran: bukan shell uid. Pastikan Shizuku sedang berjalan.")
        return 1
    print("Shizuku/rish OK.")
    print(json.dumps(device_info(runner), indent=2))
    return 0


def cmd_doctor(runner, args) -> int:
    print("Pemeriksaan kesihatan (baca sahaja, tiada perubahan)\n")
    results = doctor.run_checks(runner)
    for status, name, detail in results:
        print(f"[{status:<8}] {name}: {detail}")
    print("\n" + doctor.summarize(results))
    return 1 if any(s == doctor.CRIT for s, _, _ in results) else 0


def cmd_sambung(runner, args) -> int:
    print("HONOR Debloat Manager - sambung ke tablet melalui Wireless debugging")
    serial = connect.run()
    if not serial:
        return 1
    if args.no_check:
        return 0
    print("\nLANGKAH 3 - Pemeriksaan kesihatan\n")
    return cmd_doctor(AdbRunner(serial), args)


def cmd_scan(runner, args) -> int:
    snap = build_snapshot(runner)
    rows = [r for r in snap["packages"] if not args.category or r["category"] == args.category]
    if args.json:
        print(json.dumps(rows, indent=2, ensure_ascii=False))
        return 0
    for cat in ("A", "B", "C", "user"):
        group = [r for r in rows if r["category"] == cat]
        if not group:
            continue
        print(f"\n== Kategori {cat} - {CATEGORY_NAMES[cat]} ({len(group)}) ==")
        for r in group:
            print(f"  [{r['state']:<11}] {r['id']:<55} {r['label']}")
    counts = {c: sum(1 for r in snap["packages"] if r["category"] == c) for c in CATEGORY_NAMES}
    print(f"\nJumlah: {len(snap['packages'])} pakej  " +
          "  ".join(f"{k}={v}" for k, v in counts.items()))
    print("Tiada perubahan dibuat. Jalankan 'hdm backup' untuk simpan rekod.")
    return 0


def cmd_backup(runner, args) -> int:
    take_snapshot(runner, args.label)
    return 0


def _change(runner, args, action) -> int:
    pkg = args.package
    clf = _classifier(runner)
    current = scan(runner).get(pkg)
    if current is None:
        print(f"Pakej '{pkg}' tiada pada peranti ini.")
        return 1
    packages.guard(current, clf, args.reviewed)  # refuse early, before any prompt
    c = clf.classify(current)
    print(f"Pakej    : {pkg}\nKategori : {c.category} - {c.label}\n"
          f"Status   : {current.state}\nTindakan : {action}")
    if not _confirm(f"Taip nama pakej sekali lagi untuk sahkan: ", pkg):
        print("Dibatalkan. Tiada perubahan dibuat.")
        return 1
    snap = take_snapshot(runner, f"before-{action}-{pkg}")
    fn = packages.disable if action == "disable" else packages.uninstall
    after = fn(runner, pkg, clf, reviewed=args.reviewed, snapshot_path=snap)
    print(f"Selesai: {pkg} kini '{after.state}'.")
    print(f"Uji tablet sekarang. Untuk undur: hdm restore {pkg}")
    return 0


def cmd_disable(runner, args) -> int:
    return _change(runner, args, "disable")


def cmd_uninstall(runner, args) -> int:
    return _change(runner, args, "uninstall")


def cmd_restore(runner, args) -> int:
    originals = store.original_states()
    if args.all:
        if not originals:
            print("Tiada perubahan dalam jurnal untuk dipulihkan.")
            return 0
        print("Akan pulihkan pakej berikut ke status asal:")
        for p, s in originals.items():
            print(f"  {p} -> {s}")
        if not _confirm("Taip YA untuk teruskan: ", "YA"):
            print("Dibatalkan.")
            return 1
        targets = originals
    else:
        if not args.package:
            print("Nyatakan nama pakej atau --all.")
            return 1
        targets = {args.package: originals.get(args.package, ENABLED)}
    snap = take_snapshot(runner, "before-restore")
    failed = 0
    for p, state in targets.items():
        try:
            after = packages.restore(runner, p, target=state, snapshot_path=snap)
            print(f"OK   {p} -> {after.state}")
        except (packages.ActionRefused, RunnerError) as e:
            failed += 1
            print(f"GAGAL {p}: {e}")
    return 1 if failed else 0


def cmd_journal(runner, args) -> int:
    for e in store.read_jsonl("journal.jsonl"):
        print(json.dumps(e, ensure_ascii=False))
    return 0


def cmd_manifest(runner, args) -> int:
    cmds = store.restore_manifest()
    if not cmds:
        print("# Tiada perubahan untuk dipulihkan.")
    print("# Restore manifest - boleh dijalankan terus dalam rish jika tool ini rosak.")
    for c in cmds:
        print(c)
    return 0


def cmd_dns(runner, args) -> int:
    if args.mode == "on":
        print("Sebelum:", dns.status(runner))
        print("Selepas:", dns.enable(runner, args.hostname))
        print("Jika ada aplikasi gagal memuatkan kandungan: hdm dns off")
    elif args.mode == "off":
        print("Dipulihkan:", dns.disable(runner))
    else:
        print(dns.status(runner))
    return 0


def cmd_measure(runner, args) -> int:
    data = measure.measure(runner)
    store.append_jsonl("measurements.jsonl", {"label": args.label, **data})
    print(json.dumps({"label": args.label, **data}, indent=2))
    rows = [m for m in store.read_jsonl("measurements.jsonl")]
    if len(rows) >= 2:
        a, b = rows[-2], rows[-1]
        print(f"\nBanding '{a['label']}' -> '{b['label']}':")
        for k, v in b.items():
            if isinstance(v, int) and isinstance(a.get(k), int):
                print(f"  {k:<18} {a[k]:>8} -> {v:>8} ({v - a[k]:+})")
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="hdm", description="HONOR Debloat Manager (tanpa root)")
    p.add_argument("--version", action="version", version=__version__)
    p.add_argument("--adb", action="store_true", help="guna adb dan bukan rish (ujian dari komputer)")
    p.add_argument("--rish", default="rish", help="laluan ke rish")
    sub = p.add_subparsers(dest="command", required=True)

    sub.add_parser("check", help="semak sambungan Shizuku").set_defaults(fn=cmd_check)
    s = sub.add_parser("sambung", aliases=["connect"], help="sambung ke tablet langkah demi langkah, kemudian semak kesihatan")
    s.add_argument("--no-check", action="store_true", help="sambung sahaja, jangan jalankan doctor")
    s.set_defaults(fn=cmd_sambung)
    sub.add_parser("doctor", help="semak kesihatan tablet sebelum mula (baca sahaja)").set_defaults(fn=cmd_doctor)

    s = sub.add_parser("scan", help="senarai dan klasifikasi semua pakej (tiada perubahan)")
    s.add_argument("--category", choices=list(CATEGORY_NAMES))
    s.add_argument("--json", action="store_true")
    s.set_defaults(fn=cmd_scan)

    s = sub.add_parser("backup", help="simpan rekod penuh status pakej dan DNS")
    s.add_argument("--label", default="manual")
    s.set_defaults(fn=cmd_backup)

    for name, fn, hlp in (("disable", cmd_disable, "disable satu pakej (boleh diundur)"),
                          ("uninstall", cmd_uninstall, "uninstall untuk pengguna 0 (mesti disable dahulu)")):
        s = sub.add_parser(name, help=hlp)
        s.add_argument("package")
        s.add_argument("--reviewed", action="store_true", help="wajib untuk Kategori B")
        s.set_defaults(fn=fn)

    s = sub.add_parser("restore", help="pulihkan pakej ke status asal")
    s.add_argument("package", nargs="?")
    s.add_argument("--all", action="store_true")
    s.set_defaults(fn=cmd_restore)

    sub.add_parser("journal", help="papar semua perubahan").set_defaults(fn=cmd_journal)
    sub.add_parser("restore-manifest", help="arahan pemulihan manual").set_defaults(fn=cmd_manifest)

    s = sub.add_parser("dns", help="Private DNS sekatan iklan")
    s.add_argument("mode", choices=["status", "on", "off"])
    s.add_argument("--hostname", default=dns.ADGUARD)
    s.set_defaults(fn=cmd_dns)

    s = sub.add_parser("measure", help="rekod RAM, storan dan bateri")
    s.add_argument("label", help="cth: sebelum / selepas")
    s.set_defaults(fn=cmd_measure)
    return p


def main(argv: list[str] | None = None, runner=None) -> int:
    args = build_parser().parse_args(argv)
    if runner is None:
        if args.adb:
            serial = connect.saved_serial()
            if serial and serial not in connect.connected_devices():
                serial = None  # port changes after reboot; let adb pick the only device
            runner = AdbRunner(serial)
        elif args.command in ("sambung", "connect"):
            runner = None
        else:
            runner = RishRunner(args.rish)
    try:
        return args.fn(runner, args)
    except (RunnerError, packages.ActionRefused) as e:
        print(f"Ralat: {e}", file=sys.stderr)
        return 2
