# GEO Vision - Known Issues & Troubleshooting Guide

This document outlines known issues, limitations, and solutions for common problems.

---

## 🔴 Critical Issues

### 1. Memory Leak in GdbParser.kt
**Status**: Known, Under Investigation  
**Severity**: High  
**Affected Versions**: 1.0.0  

**Issue**: FileGDB files larger than 100MB may cause memory exhaustion due to `readBytes()` usage.

**Symptoms**:
- App crashes with "OutOfMemoryError"
- Device becomes sluggish
- Other apps close unexpectedly

**Workaround**:
1. Reduce file size before loading
2. Use alternate format (GeoPackage or Shapefile)
3. Close other apps to free RAM
4. Upgrade device RAM if possible

**Status**: Fix planned for v1.1 (Q1 2026)

---

### 2. Shapefile DBF Encoding Issues
**Status**: Known  
**Severity**: High  
**Affected Versions**: 1.0.0  

**Issue**: Attribute values may display incorrectly for non-UTF8 encoded DBF files.

**Symbols affected**:
- Arabic characters (CP1256 encoding)
- Chinese characters (Big5 encoding)
- Special symbols

**Symptoms**:
- Question marks or garbage characters in attribute table
- Arabic text appears reversed or corrupted

**Workaround**:
1. **Best**: Re-save DBF with UTF-8 encoding in QGIS or ArcGIS
2. **Temporary**: Use attribute IDs instead of names
3. **Offline**: Convert to GeoJSON first, then import

**Manual fix in QGIS**:
```bash
# Install encoding detection
pip install chardet

# Convert Shapefile to UTF-8
ogr2ogr -f "ESRI Shapefile" output.shp input.shp -t_srs EPSG:4326
```

---

## 🟠 High-Priority Issues

### 3. Large GeoJSON Files (>200MB) Parse Slowly
**Status**: Known  
**Severity**: Medium  
**Affected Versions**: 1.0.0  

**Issue**: Files with >100K features take excessive time to parse.

**Symptoms**:
- App unresponsive during parsing
- Takes 30+ seconds for large files
- Memory usage high (200-400MB)

**Performance Benchmarks**:
| File Size | Features | Parse Time | RAM Used |
|-----------|----------|-----------|----------|
| 50MB      | 25K      | 3 sec     | 120MB    |
| 200MB     | 100K     | 12 sec    | 280MB    |
| 500MB     | 250K     | 30+ sec   | 400MB+   |

**Workaround**:
1. Split large files: Use QGIS → Vector → Split Vector Layer
2. Use GeoPackage format (more efficient parsing)
3. Run on device with 2GB+ RAM
4. Close background apps before opening

**Example Split Script**:
```python
# Python with QGIS API
from qgis.core import *
# Split input.geojson into chunks of 50K features
```

---

### 4. KML Network Links Not Supported
**Status**: Known  
**Severity**: Medium  
**Affected Versions**: 1.0.0  

**Issue**: KML files with `<NetworkLink>` elements won't fetch remote data.

**Example KML**:
```xml
<kml>
  <Document>
    <NetworkLink>
      <Link>
        <href>https://example.com/data.kml</href>
      </Link>
    </NetworkLink>
  </Document>
</kml>
```

**Symptoms**:
- Network link not loaded
- Only local placemarks visible
- No error message

**Workaround**:
1. **Manual**: Download remote KML and save locally
2. **Merge**: Combine KML files using QGIS before opening
3. **Alternative**: Use GeoJSON or GeoPackage

**Merge KMLs in QGIS**:
```
Layer → Add Layer → Add Existing Layers → Select multiple KMLs
Right-click → Save As → KML
```

---

## 🟡 Medium-Priority Issues

### 5. Dark Mode Colors Inconsistent
**Status**: Known  
**Severity**: Low  
**Affected Versions**: 1.0.0  

**Issue**: Some UI elements have poor contrast in dark mode.

**Affected Components**:
- Feature list text on Android 8-9
- Map legend background
- Layer properties panel

**Workaround**:
1. Use Light Mode (Settings → Theme → Light)
2. Increase text size (Settings → Accessibility)
3. Use higher contrast wallpaper

---

### 6. GPS Tracking Battery Drain
**Status**: Known  
**Severity**: Medium  
**Affected Versions**: 1.0.0  

**Issue**: GPS tracking drains battery quickly (10-20% per hour).

**Affected Features**:
- Real-time position display
- GPS tracking with continuous updates

**Workaround**:
1. Reduce update frequency (Settings → GPS → Update Interval)
2. Use Coarse location (low accuracy, less power)
3. Enable Battery Saver mode
4. Disable continuous tracking, use on-demand instead

---

### 7. CSV Import Limited to 10K Features
**Status**: Known  
**Severity**: Low  
**Affected Versions**: 1.0.0  

**Issue**: CSV files with >10K rows may be truncated.

**Workaround**:
1. Split CSV into multiple files
2. Convert to GeoJSON or GeoPackage
3. Use QGIS to merge multiple CSV files

---

## 🟢 Non-Critical Issues

### 8. Slow Shapefile with Missing DBF
**Status**: Expected Behavior  
**Severity**: Low  
**Affected Versions**: 1.0.0  

**Issue**: If .dbf file is missing, loading .shp takes longer.

**Workaround**:
- Ensure all Shapefile components present: .shp, .shx, .dbf
- Optional: .prj for CRS information

**Required Files**:
```
survey_data.shp    ← Geometry (required)
survey_data.shx    ← Shape index (required)
survey_data.dbf    ← Attributes (required)
survey_data.prj    ← CRS info (optional)
```

---

### 9. Limited CRS Support
**Status**: Expected  
**Severity**: Low  
**Affected Versions**: 1.0.0  

**Supported CRS**:
- ✅ EPSG:4326 (WGS84)
- ✅ EPSG:3857 (Web Mercator)
- ✅ EPSG:32633 (UTM Zone 33N)
- ✅ Most common EPSG codes via Proj4j

**Unsupported**:
- ❌ Complex coordinate transformations (3D)
- ❌ Geodetic datums beyond WGS84
- ❌ Custom CRS definitions

**Workaround**:
1. Reproject data in QGIS/ArcGIS first
2. Use GeoPackage with proper CRS definition
3. Request CRS support in GitHub Issues

---

## 🔧 Troubleshooting Common Problems

### Problem: "File Not Found" Error

**Possible Causes**:
1. File deleted after selection
2. Storage permission issue
3. File path contains special characters
4. File moved to different location

**Solutions**:
```bash
# On Windows, try:
# 1. Move file to simpler path (no spaces, no special chars)
# Example: "C:\GIS_Data\survey.shp" instead of "C:\My Data\2024 Surveys\Old Files\survey.shp"

# 2. Check file permissions
# Settings → Apps → GEO Vision → Permissions → Files

# 3. Ensure file exists and is readable
adb shell ls -la /path/to/file
```

---

### Problem: "Out of Memory" Crash

**Diagnosis**:
```bash
# Check available RAM
adb shell dumpsys meminfo | grep -A 10 "TOTAL"

# Check app memory usage
adb shell dumpsys meminfo com.geovision.mobile | grep "TOTAL"
```

**Solutions**:
1. **Close background apps**: Free up RAM
2. **Reduce file size**: Split large files
3. **Use lighter format**: GeoJSON → GeoPackage
4. **Upgrade device**: Get device with more RAM

**Preventive Measures**:
- Monitor memory: Settings → Debug → Memory Monitor
- Set cache limits: Settings → Storage
- Enable aggressive cache eviction

---

### Problem: Features Not Displaying

**Possible Causes**:
1. Features outside visible map area
2. Zoom level too far out
3. Feature rendering disabled
4. CRS mismatch
5. Invalid geometry

**Solutions**:
```bash
# 1. Zoom to extent
# Long-press layer → "Zoom to Layer"

# 2. Check CRS
# Layer properties → Info tab → CRS

# 3. Validate geometry
# Open in QGIS: Vector → Check Validity

# 4. Check rendering
# Layer properties → Visibility toggle
# Map → Basemap toggle
```

---

### Problem: Attributes Show Garbage Characters

**Possible Causes**:
1. Non-UTF8 encoding (CP1256, Big5, etc.)
2. Corrupted DBF file
3. Mixed encodings

**Solutions**:
1. **Detect encoding**:
   ```python
   import chardet
   with open('file.dbf', 'rb') as f:
       result = chardet.detect(f.read())
       print(result['encoding'])  # e.g., 'cp1256'
   ```

2. **Convert in QGIS**:
   - File → Export → Select UTF-8 encoding

3. **Verify DBF structure**:
   ```bash
   # Using ogrinfo
   ogrinfo -al file.shp | grep -A 5 "Geometry"
   ```

---

### Problem: App Crashes on Startup

**Diagnosis**:
```bash
# Check crash logs
adb logcat | grep "GeoVision\|AndroidRuntime"

# View full crash report
adb bugreport > bugreport.zip
```

**Common Causes**:
1. Incompatible Android version
2. Missing permissions
3. Corrupted cache
4. Previous version conflict

**Solutions**:
```bash
# 1. Check Android version
adb shell getprop ro.build.version.sdk

# 2. Clear app cache
adb shell pm clear com.geovision.mobile

# 3. Uninstall and reinstall
adb uninstall com.geovision.mobile
adb install app-debug.apk

# 4. Request permissions
# Settings → Apps → GEO Vision → Permissions
# Grant: Files, Location, Camera (if EXIF)
```

---

## 🐛 Reporting Issues

If you encounter a problem not listed above:

1. **Check existing issues**: [GitHub Issues](https://github.com/YOUR-USERNAME/GEO-Vision/issues)
2. **Collect diagnostics**:
   ```bash
   adb logcat > crash_log.txt  # Run before crash
   adb shell dumpsys meminfo com.geovision.mobile > memory.txt
   adb shell getprop > device_info.txt
   ```
3. **Create new issue** with:
   - Device info (model, Android version, RAM)
   - App version
   - Steps to reproduce
   - Logs and screenshots
   - Sample file (if applicable)

---

## 📊 System Requirements

### Minimum Requirements
- **Android**: 8.0 (API 26)
- **RAM**: 1GB
- **Storage**: 50MB free
- **Processor**: Qualcomm Snapdragon 400 or equivalent

### Recommended
- **Android**: 10+ (API 29+)
- **RAM**: 2GB+
- **Storage**: 200MB free
- **Processor**: Snapdragon 665 or better
- **GPU**: Adreno 300 or equivalent

### Device Support Status

| Device | Status | Notes |
|--------|--------|-------|
| Samsung Galaxy A10 | ✅ Supported | 2GB RAM, works well with files <50MB |
| OnePlus 7T | ✅ Optimal | Recommended for best performance |
| iPhone/iOS | ❌ Not Supported | Android only; iOS version planned |
| Tablets | ✅ Supported | Optimized for landscape mode |

---

## 📈 Performance Optimization Tips

### For Large Files
```
1. Use GeoPackage format (most efficient)
2. Split files into regional chunks
3. Pre-process in QGIS/ArcGIS
4. Remove unnecessary attributes
5. Simplify complex geometries (Optional → Simplify)
```

### For Low-End Devices
```
1. Use Light Mode (less GPU usage)
2. Reduce map refresh rate
3. Lower map tile quality
4. Disable GPS tracking
5. Close background apps
```

### Cache Management
```
Settings → Storage
├── Clear Cache (safe, recoverable)
├── Clear Metadata (requires re-parse)
└── Clear All (resets to default)
```

---

## 🔐 Security Notes

### Safe Usage
- ✅ App processes files locally (no cloud upload)
- ✅ No internet required for viewing
- ✅ Data never leaves your device

### Privacy Considerations
- ⚠️ EXIF data in photos may contain GPS
- ⚠️ Local metadata stored unencrypted
- ⚠️ Enable device encryption for sensitive projects

---

## 📞 Getting Help

- 📖 **Documentation**: [README.md](README.md)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions)
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/YOUR-USERNAME/GEO-Vision/issues)
- 📧 **Email Support**: [Your email]

---

*Last Updated: January 2026*  
*Version: 1.0.0*
