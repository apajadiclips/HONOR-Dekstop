"""Puts each package into category A (safe candidate), B (review), C (protected) or user."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from .scanner import Package

POLICY_DIR = Path(__file__).resolve().parent.parent / "policies"

SAFE, REVIEW, PROTECTED, USER = "A", "B", "C", "user"


@dataclass
class Classification:
    category: str
    label: str


def _load(name: str, policy_dir: Path) -> dict:
    with open(policy_dir / name, encoding="utf-8") as f:
        return json.load(f)


class Classifier:
    def __init__(self, policy_dir: Path = POLICY_DIR, dynamic_protected: dict[str, str] | None = None):
        self.protected = _load("protected-packages.json", policy_dir)
        self.review = _load("review-required.json", policy_dir)
        self.safe = _load("safe-candidates.json", policy_dir)
        self.dynamic = dynamic_protected or {}

    @staticmethod
    def _exact(policy: dict, pkg: str) -> str | None:
        for e in policy.get("packages", []):
            if e["id"] == pkg:
                return e.get("label", "")
        return None

    @staticmethod
    def _prefix(policy: dict, pkg: str) -> str | None:
        best = None
        for e in policy.get("prefixes", []):
            if pkg.startswith(e["prefix"]) and (best is None or len(e["prefix"]) > len(best["prefix"])):
                best = e
        return best.get("label", "") if best else None

    def classify(self, pkg: Package) -> Classification:
        pid = pkg.id
        # C - protected always wins.
        label = self._exact(self.protected, pid)
        if label is not None:
            return Classification(PROTECTED, label)
        if pid in self.dynamic:
            return Classification(PROTECTED, self.dynamic[pid])
        for m in self.protected.get("path_markers", []):
            if pkg.path and m["marker"] in pkg.path:
                return Classification(PROTECTED, m["label"])
        label = self._prefix(self.protected, pid)
        if label is not None:
            return Classification(PROTECTED, label)

        if not pkg.system:
            return Classification(USER, "Aplikasi pengguna - buang secara biasa jika mahu")

        # A - explicit safe candidates (exact IDs only).
        label = self._exact(self.safe, pid)
        if label is not None:
            return Classification(SAFE, label)
        label = self._prefix(self.safe, pid)
        if label is not None:
            return Classification(SAFE, label)

        # B - everything else that is a system package.
        label = self._exact(self.review, pid)
        if label is None:
            label = self._prefix(self.review, pid)
        return Classification(REVIEW, label or "Pakej sistem tidak dikenali - semak dahulu")
