# Self-contained skill distribution

The source checkout keeps shared material in repository-level `references/`,
`scripts/`, `examples/`, `templates/` and `fixtures/` directories. Individual
skills also link to sibling skills. A command that copies only
`skills/<name>/` therefore has not preserved the complete local dependency
graph merely because `SKILL.md` itself was installed.

## Verified local bundle path

Run the standard-library builder from the repository root with Python 3.11 or
newer. Choose a new output location; existing outputs are never overwritten.

```powershell
python scripts/build_skill_bundle.py --output-dir dist/skill-release
python scripts/build_skill_bundle.py --check dist/skill-release/gis-development-suite.zip
```

The command produces:

```text
dist/skill-release/
  gis-development-suite/          # inspectable expanded distribution
    SKILL.md                       # suite entrypoint
    bundle-manifest.json           # file sizes and SHA-256 hashes
    skills/                        # core router and specialists
    references/ scripts/ ...       # shared runtime guidance and helpers
  gis-development-suite.zip       # deterministic archive
  gis-development-suite.zip.sha256
```

The build uses an explicit repository allowlist. It rejects symlinks,
sensitive-looking credential/key files, individual files larger than 10 MiB,
expanded bundles larger than 25 MiB, and output paths that would contain the
source repository. It excludes Git internals, `.agent-development-log/`,
virtual environments, caches, package directories and previous build outputs.

The post-build audit verifies:

- required router, security, evidence, journal and plan files;
- every manifest size and SHA-256 digest;
- absence of unexpected or missing files;
- all local Markdown link targets remain inside and present in the bundle;
- skill IDs agree with `references/repository-manifest.json`.

The ZIP uses sorted paths, a fixed timestamp and fixed file permissions so two
builds from the same source bytes and Git commit have the same SHA-256.

## Source checkout versus installable archive

The repository source remains the contributor/development surface. The bundle
is the release-candidate consumption surface for users who need the core router
and its shared dependencies together. Do not point users to a single nested
skill install as a verified complete route until an isolated installation test
shows that its external references are present.

The archive has a root skill named `gis-development-suite`. A compatible
Agent Skills installer may accept a local or release-asset archive, for example:

```powershell
npx skills add path/to/gis-development-suite.zip --skill gis-development-suite
```

That command is a **candidate integration command**, not evidence in this
repository that a particular CLI version or coding-agent client completed the
installation. Before publishing support for a client, record its exact CLI and
agent versions, install into an isolated project, confirm the installed file
manifest, trigger a routing smoke request, and report the observed result.

## Evidence boundary

Passing the builder and audit is repository/offline evidence only. It does not
install QGIS, ArcGIS, GDAL, an AI model or an MCP server; it does not execute an
agent prompt; and it does not establish T2-T4 native GIS evidence. Fill the
[GIS acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md) and apply
[DoneProof](../skills/completion-evidence/SKILL.md) before making broader release
claims.

