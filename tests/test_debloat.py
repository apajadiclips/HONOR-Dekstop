import os
import tempfile
import unittest

from debloat.actions import dns, packages
from debloat.backup import store
from debloat.inventory.classifier import Classifier
from debloat.inventory.scanner import dynamic_protected, scan
from tests.fake_device import sample_device


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        os.environ["HDM_HOME"] = self.tmp.name
        self.dev = sample_device()
        self.clf = Classifier(dynamic_protected=dynamic_protected(self.dev))

    def tearDown(self):
        self.tmp.cleanup()
        os.environ.pop("HDM_HOME", None)

    def cat(self, pid):
        return self.clf.classify(scan(self.dev)[pid]).category


class ScanAndClassify(Base):
    def test_states_and_system_flag(self):
        self.dev.pkgs["com.facebook.appmanager"]["state"] = "disabled"
        self.dev.pkgs["com.mystery.preload"]["state"] = "uninstalled"
        pkgs = scan(self.dev)
        self.assertEqual(pkgs["com.facebook.appmanager"].state, "disabled")
        self.assertEqual(pkgs["com.mystery.preload"].state, "uninstalled")
        self.assertFalse(pkgs["com.whatsapp"].system)
        self.assertEqual(pkgs["com.whatsapp"].path, "/data/app/~~x/com.whatsapp/base.apk")

    def test_categories(self):
        self.assertEqual(self.cat("com.android.systemui"), "C")
        self.assertEqual(self.cat("com.hihonor.android.launcher"), "C")
        self.assertEqual(self.cat("com.android.theme.font"), "C")
        self.assertEqual(self.cat("com.vendor.someoverlay"), "C")  # by path
        self.assertEqual(self.cat("com.example.keyboard"), "C")    # current IME
        self.assertEqual(self.cat("com.facebook.appmanager"), "A")
        self.assertEqual(self.cat("com.hihonor.appmarket"), "B")
        self.assertEqual(self.cat("com.mystery.preload"), "B")     # unknown system -> review
        self.assertEqual(self.cat("com.whatsapp"), "user")


class Actions(Base):
    def test_protected_refused_and_untouched(self):
        with self.assertRaises(packages.ActionRefused):
            packages.disable(self.dev, "com.android.systemui", self.clf, reviewed=True)
        self.assertEqual(self.dev.pkgs["com.android.systemui"]["state"], "enabled")
        self.assertFalse(any(c[:2] == ["pm", "disable-user"] for c in self.dev.calls))

    def test_review_needs_flag(self):
        with self.assertRaises(packages.ActionRefused):
            packages.disable(self.dev, "com.hihonor.appmarket", self.clf)
        packages.disable(self.dev, "com.hihonor.appmarket", self.clf, reviewed=True)
        self.assertEqual(self.dev.pkgs["com.hihonor.appmarket"]["state"], "disabled")

    def test_uninstall_requires_disable_first(self):
        with self.assertRaises(packages.ActionRefused):
            packages.uninstall(self.dev, "com.facebook.appmanager", self.clf)
        packages.disable(self.dev, "com.facebook.appmanager", self.clf)
        packages.uninstall(self.dev, "com.facebook.appmanager", self.clf)
        self.assertEqual(self.dev.pkgs["com.facebook.appmanager"]["state"], "uninstalled")

    def test_full_round_trip_restore(self):
        pid = "com.facebook.appmanager"
        packages.disable(self.dev, pid, self.clf)
        packages.uninstall(self.dev, pid, self.clf)
        self.assertEqual(store.original_states(), {pid: "enabled"})
        self.assertEqual(store.restore_manifest(), [
            f"cmd package install-existing --user 0 {pid}",
            f"pm enable --user 0 {pid}",
        ])
        packages.restore(self.dev, pid, target="enabled")
        self.assertEqual(self.dev.pkgs[pid]["state"], "enabled")
        actions = [e["action"] for e in store.read_jsonl("journal.jsonl")]
        self.assertEqual(actions, ["disable", "uninstall", "restore"])

    def test_snapshot_written_atomically(self):
        path = store.save_snapshot({"x": 1}, "t")
        self.assertTrue(path.exists())
        self.assertEqual(list(path.parent.glob("*.tmp")), [])


class Dns(Base):
    def test_on_off_restores_previous(self):
        self.dev.settings.update(private_dns_mode="hostname", private_dns_specifier="one.one.one.one")
        dns.enable(self.dev)
        self.assertEqual(self.dev.settings["private_dns_specifier"], dns.ADGUARD)
        dns.enable(self.dev)  # second "on" must not overwrite the real backup
        dns.disable(self.dev)
        self.assertEqual(self.dev.settings["private_dns_mode"], "hostname")
        self.assertEqual(self.dev.settings["private_dns_specifier"], "one.one.one.one")

    def test_off_without_backup_goes_automatic(self):
        dns.enable(self.dev)
        dns.disable(self.dev)
        self.assertEqual(self.dev.settings["private_dns_mode"], "opportunistic")
        self.assertNotIn("private_dns_specifier", self.dev.settings)


class Cli(Base):
    def test_scan_makes_no_changes(self):
        from debloat.cli import main
        self.assertEqual(main(["scan"], runner=self.dev), 0)
        self.assertFalse(any(c[0] == "pm" and c[1] != "list" for c in self.dev.calls))
        self.assertFalse(any(c[:2] == ["settings", "put"] for c in self.dev.calls))

    def test_disable_protected_via_cli_returns_error(self):
        from debloat.cli import main
        self.assertEqual(main(["disable", "com.android.systemui", "--reviewed"], runner=self.dev), 2)


if __name__ == "__main__":
    unittest.main()


class Doctor(Base):
    def test_read_only_and_reports(self):
        from debloat.actions import doctor
        res = doctor.run_checks(self.dev)
        names = {n: s for s, n, _ in res}
        self.assertEqual(names["Akses Shizuku"], "OK")
        self.assertEqual(names["Bateri"], "OK")          # 33% but charging
        self.assertEqual(names["Storan dalaman"], "OK")  # ~9.5 GB free
        self.assertEqual(names["Backup asal"], "AMARAN")
        mutating = [c for c in self.dev.calls
                    if c[:2] in (["settings", "put"], ["settings", "delete"])
                    or (c[0] == "pm" and c[1] != "list") or c[:3] == ["cmd", "package", "install-existing"]]
        self.assertEqual(mutating, [])

    def test_disabled_protected_is_critical(self):
        from debloat.actions import doctor
        self.dev.pkgs["com.android.systemui"]["state"] = "disabled"
        res = doctor.run_checks(self.dev)
        self.assertTrue(any(s == "KRITIKAL" and "systemui" in d for s, _, d in res))
        self.assertIn("JANGAN", doctor.summarize(res))

    def test_cli_output(self):
        from debloat.cli import main
        self.assertEqual(main(["doctor"], runner=self.dev), 0)
