# ArcGIS Pro batch layout export to PDF

**Native ArcGIS Pro / ArcPy example, not executed in generic Linux CI.** Use a copy of your project and write into a new output directory:

```powershell
python export_layouts_arcgis_pro.py --project "C:\GIS\Project.aprx" --output "C:\GIS\PDFs" --dry-run
python export_layouts_arcgis_pro.py --project "C:\GIS\Project.aprx" --output "C:\GIS\PDFs"
```

The example exports every project layout, sanitizes filenames, rejects file overwrite by default, and never edits the `.aprx`. Verify export APIs for your ArcGIS Pro version (`exportToPDF` versus `PDFFormat`/`Layout.export`). For actual acceptance open the PDFs and verify page count, labels, scale, layers, font support and output read-back. Neither general Python syntax checks nor ArcPy mocks prove native export success.
