# Synthetic GIS UI Command Inventory

Use a **read-only static audit** against simple local HTML + WPF XAML fixtures:

```bash
python scripts/gis_ui_command_audit.py --root examples/ui-command-audit/fixture --format json
```

This fixtures-only example contains eight declared controls: four WPF and four HTML controls. Of those, three XAML controls and three HTML controls declare a command or click handler; one XAML control declares no binding; one HTML control is reserved for a potential dynamically attached listener. All runtime statuses remain **NOT RUN**.

Expected counts:
- `DECLARED_BINDING: 6`
- `NO_BINDING: 1`
- `UNINSPECTED: 1`

`PlaceholderMenu` has no declared XAML binding; `dynamic-layer-toggle` has no inline handler but may be wired through delegated JavaScript, so **must not** be called broken on static evidence alone. `onRefresh()` is a textual reference, not an executed function. The script does not launch Windows/UIA, search C# or JS listener implementations, verify icon files, or report actual GIS capability. Route candidates to [native E2E tests](../desktop-e2e/README.md) and [Data Flow](../data-flow-diagnostics/README.md) for proof.
