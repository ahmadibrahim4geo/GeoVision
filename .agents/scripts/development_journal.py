#!/usr/bin/env python3
"""Local, numbered append-only project journal; no AI, Git mutations or command execution."""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import sys
import uuid

FOLDER = ".agent-development-log"
KINDS = {"request", "plan", "implementation", "revision", "cancellation",
         "rollback", "verification", "decision", "blocker", "handoff"}
STATUSES = {"proposed", "approved", "in_progress", "completed", "failed",
            "blocked", "cancelled", "reverted", "not_run"}
LINKED_KINDS = {"revision", "cancellation", "rollback"}
REC_RX = re.compile(r"^[0-9]{4,}\.json$")


def _canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False,
                      separators=(",", ":")).encode("utf-8")


def digest(record):
    return hashlib.sha256(_canonical({k: v for k, v in record.items()
                                      if k != "hash"})).hexdigest()


def _text(value, field):
    if not isinstance(value, str) or not value.strip() or "\x00" in value or len(value) > 10000:
        raise ValueError(field + " must be nonempty text (max 10000)")
    return value.strip()


def _relative(value):
    value = _text(value, "relative file path")
    if "\\" in value or value.startswith("/") or re.match(r"^[A-Za-z]:", value):
        raise ValueError("changed source paths must be project-relative POSIX paths")
    parsed = PurePosixPath(value)
    if ".." in parsed.parts or "." in parsed.parts or parsed.as_posix() != value:
        raise ValueError("unsafe or noncanonical file path: " + value)
    if parsed.parts[0] == FOLDER:
        raise ValueError("cannot treat journal files as project source changes")
    return value


def location(project):
    root = Path(project).expanduser().resolve(strict=True)
    if not root.is_dir():
        raise ValueError("project must be an existing directory")
    journal = root / FOLDER
    if journal.is_symlink() or (journal.exists() and not journal.is_dir()):
        raise ValueError("journal directory cannot be a symlink")
    for name in ("records", "reports"):
        if (journal / name).is_symlink():
            raise ValueError("journal subdirectory cannot be a symlink")
    return journal


def _initialized(journal):
    if not (journal / "records").is_dir() or not (journal / "reports").is_dir():
        raise ValueError("run init first to create the project journal")
    for name in ("INDEX.md", "STATE.md", ".write.lock"):
        if (journal / name).is_symlink():
            raise ValueError("journal file cannot be a symlink: " + name)


@contextmanager
def _locked(journal):
    lock = journal / ".write.lock"
    try:
        fd = os.open(str(lock), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as exc:
        raise ValueError("journal is locked; investigate other writers before removing lock") from exc
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(str(os.getpid()) + "\n")
        yield
    finally:
        lock.unlink(missing_ok=True)


def _write(path, content, overwrite=False):
    if path.is_symlink():
        raise ValueError("refusing symlink output " + path.name)
    if path.exists() and not overwrite:
        raise ValueError("refusing overwrite of canonical record " + path.name)
    temporary = path.with_name(".tmp-" + uuid.uuid4().hex)
    try:
        with temporary.open("x", encoding="utf-8", newline="\n") as f:
            f.write(content)
            f.flush()
            os.fsync(f.fileno())
        if overwrite:
            os.replace(temporary, path)
        else:
            os.link(temporary, path)  # Exclusive: never replace canonical record.
            temporary.unlink()
    finally:
        temporary.unlink(missing_ok=True)


def records(journal):
    _initialized(journal)
    entries = sorted(p for p in (journal / "records").iterdir()
                     if not p.name.startswith("."))
    if any(p.is_symlink() or not p.is_file() or not REC_RX.fullmatch(p.name)
           for p in entries):
        raise ValueError("unexpected, unsafe record file")
    prior = None
    result = []
    for number, path in enumerate(entries, 1):
        if path.name != f"{number:04d}.json":
            raise ValueError("nonconsecutive record numbering: " + path.name)
        row = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(row, dict) or row.get("sequence") != number:
            raise ValueError("incorrect record sequence")
        if row.get("previous_hash") != prior or row.get("hash") != digest(row):
            raise ValueError("journal hash-chain mismatch: " + path.name)
        if row.get("kind") not in KINDS or row.get("status") not in STATUSES:
            raise ValueError("invalid stored kind/status")
        refs = row.get("references")
        if not isinstance(refs, list) or any(type(r) is not int or r < 1 or r >= number
                                              for r in refs):
            raise ValueError("invalid previous-step links")
        if row["kind"] in LINKED_KINDS and not refs:
            raise ValueError("a revision, cancellation or rollback must link an earlier step")
        prior = row["hash"]
        result.append(row)
    return result


def _report(row):
    references = ", ".join(f"#{n:04d}" for n in row["references"]) or "—"
    files = "\n".join("- " + file for file in row["changed_files"]) or "- None"
    tests = "\n".join("- " + test for test in row["tests"]) or "- Not recorded"
    details = "\n".join("> " + line for line in row["details"].splitlines())
    return (f'# Step {row["sequence"]:04d} — {row["summary"]}\n\n'
            f'- UTC: {row["timestamp_utc"]}\n- Event: {row["kind"]}\n'
            f'- Status: {row["status"]}\n- Task: {row["task_id"] or "—"}\n'
            f'- Plan revision: {row["plan_revision"] or "—"}\n'
            f'- Related steps: {references}\n'
            f'- Commit: {row["commit"] or "—"}\n'
            f'- Test result: {row["test_result"]}\n\n'
            f'## Request / work record\n\n{details}\n\n'
            f'## Changed files\n\n{files}\n\n'
            f'## Test commands or evidence\n\n{tests}\n\n'
            '> This is a recorded claim, not independent verification of the actual project.\n')


def _outputs(rows):
    lines = ["# Development Journal — numbered append-only records", "",
             "Authoritative records are in records/*.json. Do not rewrite older records;",
             "create a linked revision or cancellation. This local hash chain is not a signature.", "",
             "| Step | UTC | Event | Status | Linked to | Summary |",
             "| --- | --- | --- | --- | --- | --- |"]
    for r in rows:
        ref = ", ".join("#" + str(n).zfill(4) for n in r["references"]) or "—"
        summary = str(r["summary"]).replace("|", "\\|").replace("\n", " ")
        lines.append(f'| [#{r["sequence"]:04d}](reports/{r["sequence"]:04d}.md) | '
                     f'{r["timestamp_utc"]} | {r["kind"]} | {r["status"]} | {ref} | {summary} |')
    state = ["# Project development state — agent handoff", "",
             "Read with INDEX.md and the linked reports. Independently inspect code and tests.",
             f"Recorded steps: {len(rows)}", ""]
    if rows:
        state += ["## Latest records", ""]
        for row in rows[-10:]:
            refs = " ".join("#" + str(n).zfill(4) for n in row["references"])
            state.append(f'- #{row["sequence"]:04d} [{row["status"]}] {row["summary"]}' +
                         (f" (related: {refs})" if refs else ""))
        state += ["", "## Open/blocked records (confirm whether still applicable)", ""]
        pending = [x for x in rows if x["status"] in
                   ("proposed", "in_progress", "blocked", "failed")]
        state += [f'- #{x["sequence"]:04d}: {x["status"]} — {x["summary"]}'
                  for x in pending[-20:]] or ["- None recorded"]
    return "\n".join(lines) + "\n", "\n".join(state) + "\n"


def _render(journal, rows, overwrite=False):
    for row in rows:
        path = journal / "reports" / f'{row["sequence"]:04d}.md'
        content = _report(row)
        if path.exists() and not overwrite:
            if path.is_symlink() or path.read_text(encoding="utf-8") != content:
                raise ValueError("report drift; run repair: " + path.name)
        else:
            _write(path, content, overwrite=overwrite)
    index, state = _outputs(rows)
    _write(journal / "INDEX.md", index, overwrite=True)
    _write(journal / "STATE.md", state, overwrite=True)


def init(project):
    journal = location(project)
    journal.mkdir(exist_ok=True)
    for name in ("records", "reports"):
        (journal / name).mkdir(exist_ok=True)
    _initialized(journal)
    with _locked(journal):
        _render(journal, records(journal))
    return journal


def verify(project):
    journal = location(project)
    rows = records(journal)
    for row in rows:
        path = journal / "reports" / f'{row["sequence"]:04d}.md'
        if path.is_symlink() or not path.is_file() or path.read_text(encoding="utf-8") != _report(row):
            raise ValueError("missing or changed human-readable report: " + path.name)
    index, state = _outputs(rows)
    for name, expected in (("INDEX.md", index), ("STATE.md", state)):
        path = journal / name
        if path.is_symlink() or not path.is_file() or path.read_text(encoding="utf-8") != expected:
            raise ValueError("generated journal file missing/changed: " + name)
    return rows


def add(project, *, kind, status, summary, details, references=None,
        task_id="", plan_revision="", changed_files=None, tests=None,
        test_result="not_run", commit=""):
    journal = location(project)
    _initialized(journal)
    if kind not in KINDS or status not in STATUSES:
        raise ValueError("invalid kind or status")
    summary, details = _text(summary, "summary"), _text(details, "details")
    if len(summary) > 180 or "\n" in summary:
        raise ValueError("summary must be single-line and <=180 characters")
    if test_result not in ("passed", "failed", "blocked", "not_run"):
        raise ValueError("invalid test_result")
    paths = [_relative(p) for p in (changed_files or [])]
    evidence = [_text(t, "test") for t in (tests or [])]
    refs = references or []
    if any(type(x) is not int for x in refs) or len(refs) != len(set(refs)):
        raise ValueError("references must be unique integer steps")
    if kind in LINKED_KINDS and not refs:
        raise ValueError("revision, cancellation and rollback must link a previous step")
    if status == "completed" and kind in ("implementation", "revision", "rollback", "verification"):
        if not (paths or commit or (evidence and test_result == "passed")):
            raise ValueError("completed technical work must include verifiable evidence references")
    for name, value in (("task_id", task_id), ("plan_revision", plan_revision), ("commit", commit)):
        if not isinstance(value, str) or "\x00" in value or len(value) > 180:
            raise ValueError("invalid " + name)
    with _locked(journal):
        rows = verify(project)
        number = len(rows) + 1
        if any(x < 1 or x >= number for x in refs):
            raise ValueError("references must identify earlier steps")
        row = {
            "schema_version": "1.0", "sequence": number,
            "timestamp_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "kind": kind, "status": status, "summary": summary,
            "details": details, "references": refs, "task_id": task_id,
            "plan_revision": plan_revision, "changed_files": paths,
            "tests": evidence, "test_result": test_result, "commit": commit,
            "previous_hash": rows[-1]["hash"] if rows else None
        }
        row["hash"] = digest(row)
        _write(journal / "records" / f"{number:04d}.json",
               json.dumps(row, indent=2, ensure_ascii=False) + "\n")
        _render(journal, rows + [row])
    return row


def repair(project):
    journal = location(project)
    _initialized(journal)
    with _locked(journal):
        rows = records(journal)
        _render(journal, rows, overwrite=True)
    return len(rows)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("init", "add", "status", "verify", "repair"):
        p = commands.add_parser(name)
        p.add_argument("--project", required=True, type=Path)
        if name == "add":
            p.add_argument("--kind", choices=sorted(KINDS), required=True)
            p.add_argument("--status", choices=sorted(STATUSES), required=True)
            p.add_argument("--summary", required=True)
            p.add_argument("--details", required=True)
            p.add_argument("--ref", type=int, action="append", default=[])
            p.add_argument("--task-id", default="")
            p.add_argument("--plan-revision", default="")
            p.add_argument("--file", action="append", default=[])
            p.add_argument("--test", action="append", default=[])
            p.add_argument("--test-result", choices=["passed", "failed", "blocked", "not_run"],
                           default="not_run")
            p.add_argument("--commit", default="")
    args = parser.parse_args(argv)
    try:
        if args.command == "init":
            print("Journal initialized: " + str(init(args.project)))
        elif args.command == "add":
            row = add(args.project, kind=args.kind, status=args.status,
                      summary=args.summary, details=args.details,
                      references=args.ref, task_id=args.task_id,
                      plan_revision=args.plan_revision,
                      changed_files=args.file, tests=args.test,
                      test_result=args.test_result, commit=args.commit)
            print(f'Journal step #{row["sequence"]:04d} recorded')
        elif args.command == "status":
            verify(args.project)
            print((location(args.project) / "STATE.md").read_text(encoding="utf-8"))
        elif args.command == "verify":
            print(f'Journal valid: {len(verify(args.project))} records')
        else:
            print(f'Rebuilt derived reports for {repair(args.project)} records')
        return 0
    except (ValueError, OSError, KeyError, TypeError, json.JSONDecodeError) as exc:
        print("ERROR: " + str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
