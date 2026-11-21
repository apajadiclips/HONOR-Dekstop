"""Executes shell commands with shell-uid privileges (via Shizuku rish or adb)."""

from __future__ import annotations

import shlex
import subprocess


class RunnerError(RuntimeError):
    pass


def _join(cmd: list[str]) -> str:
    return " ".join(shlex.quote(c) for c in cmd)


class RishRunner:
    """Runs commands through Shizuku's `rish` (on-device, no computer)."""

    def __init__(self, rish: str = "rish", timeout: int = 120):
        self.rish = rish
        self.timeout = timeout

    def run(self, cmd: list[str]) -> str:
        try:
            p = subprocess.run(
                [self.rish, "-c", _join(cmd)],
                capture_output=True, text=True, timeout=self.timeout,
            )
        except FileNotFoundError as e:
            raise RunnerError(
                f"'{self.rish}' tidak dijumpai. Pasang rish daripada aplikasi Shizuku "
                "(Shizuku > Use Shizuku in terminal apps > Export files) ke Termux."
            ) from e
        if p.returncode != 0:
            raise RunnerError(f"Arahan gagal ({p.returncode}): {_join(cmd)}\n{p.stderr.strip()}")
        return p.stdout


class AdbRunner:
    """Optional: same commands over adb, for testing from a computer."""

    def __init__(self, serial: str | None = None, timeout: int = 120):
        self.serial = serial
        self.timeout = timeout

    def run(self, cmd: list[str]) -> str:
        base = ["adb"] + (["-s", self.serial] if self.serial else [])
        p = subprocess.run(base + ["shell", _join(cmd)], capture_output=True, text=True,
                           timeout=self.timeout)
        if p.returncode != 0:
            raise RunnerError(f"adb gagal ({p.returncode}): {_join(cmd)}\n{p.stderr.strip()}")
        return p.stdout
