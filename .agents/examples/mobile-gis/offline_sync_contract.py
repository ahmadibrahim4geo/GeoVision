"""Deterministic in-memory GIS field edit/sync fixture; NOT a mobile app or auth server."""
from copy import deepcopy
from dataclasses import dataclass, field
from typing import Any, Dict, Optional


class SyncBlocked(Exception):
    """No authorized network session: retain local outbox without dropping edits."""


class RevisionConflict(Exception):
    """Client revision differs from server: the original edit remains in outbox."""


@dataclass(frozen=True)
class Edit:
    op_id: str
    feature_id: str
    action: str
    base_revision: Optional[int]
    fields: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self):
        if not isinstance(self.op_id, str) or not self.op_id.strip():
            raise ValueError("op_id required")
        if not isinstance(self.feature_id, str) or not self.feature_id.strip():
            raise ValueError("feature_id required")
        if self.action not in ("create", "update", "delete"):
            raise ValueError("invalid edit action")
        if self.base_revision is not None and (type(self.base_revision) is not int or self.base_revision < 1):
            raise ValueError("base_revision must be positive integer or None")
        if not isinstance(self.fields, dict):
            raise ValueError("fields must be a dictionary")


class MemoryServer:
    """Small stand-in for server-side idempotent revision checks."""

    def __init__(self):
        self.features = {}
        self.acknowledged = {}

    def apply(self, edit, *, connected, authenticated):
        if not connected or not authenticated:
            raise SyncBlocked("network and verified sign-in required for remote writes")
        if not isinstance(edit, Edit):
            raise TypeError("expected Edit")
        if edit.op_id in self.acknowledged:
            previous_edit, revision = self.acknowledged[edit.op_id]
            if previous_edit != edit:
                raise ValueError("op_id cannot be reused for a different operation")
            return revision

        current = self.features.get(edit.feature_id)
        current_revision = current["revision"] if current else None
        if edit.action == "create":
            if edit.base_revision is not None or current is not None:
                raise RevisionConflict("feature already exists or stale create")
            revision = 1
        else:
            if current is None or edit.base_revision != current_revision:
                raise RevisionConflict("stale or absent feature revision")
            revision = current_revision + 1
        self.features[edit.feature_id] = {
            "revision": revision,
            "deleted": edit.action == "delete",
            "fields": {} if edit.action == "delete" else deepcopy(edit.fields),
        }
        # Retain original payload snapshot: later edits of the caller's dict cannot rewrite audit history.
        self.acknowledged[edit.op_id] = (deepcopy(edit), revision)
        return revision


class Outbox:
    """Reference queue only, IN MEMORY. A native adapter MUST persist this atomically."""

    def __init__(self):
        self.pending = {}
        self.conflicts = {}

    def queue(self, edit):
        if not isinstance(edit, Edit):
            raise TypeError("expected Edit")
        if edit.op_id in self.pending or edit.op_id in self.conflicts:
            raise ValueError("duplicate local op_id")
        self.pending[edit.op_id] = deepcopy(edit)

    def flush(self, server, *, connected, authenticated):
        acknowledgments = {}
        for op_id, edit in list(self.pending.items()):
            try:
                revision = server.apply(edit, connected=connected, authenticated=authenticated)
            except SyncBlocked:
                break  # Preserve all pending edits across offline, logout and expiry.
            except RevisionConflict:
                self.conflicts[op_id] = self.pending.pop(op_id)
            else:
                acknowledgments[op_id] = revision
                del self.pending[op_id]
        return acknowledgments
