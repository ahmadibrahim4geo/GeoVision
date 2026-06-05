# GEO Vision - Installation & Quick Start Guide

Complete step-by-step guide to install and start using GEO Vision.

---

## 📥 Installation Methods

### Method 1: Download APK (Easiest)

1. **Download latest APK**
   - Go to [Releases](https://github.com/YOUR-USERNAME/GEO-Vision/releases)
   - Download `geovision-v1.0.0-core.apk` or `geovision-v1.0.0-esri.apk`

2. **Enable Installation from Unknown Sources**
   - Settings → Security → Unknown Sources → Toggle ON
   - (Or allow when prompted during install)

3. **Install APK**
   - Open file manager
   - Locate downloaded APK
   - Tap to install
   - Grant permissions when prompted

4. **Launch App**
   - Find "GEO Vision" in app drawer
   - Tap to open

---

### Method 2: Android Studio (For Developers)

**Prerequisites**:
- Android Studio 2024.1+
- Android SDK 26+
- Java JDK 17+

**Installation Steps**:

```bash
# 1. Clone repository
git clone https://github.com/YOUR-USERNAME/GEO-Vision.git
cd GEO-Vision

# 2. Open in Android Studio
# File → Open → Select GEO-Vision folder

# 3. Wait for Gradle sync to complete

# 4. Configure Android SDK
# File → Project Structure → SDK Location

# 5. Select device/emulator
# Device dropdown → Select your device

# 6. Build and run
# Shift + F10 (Run) or Build → Run 'app'
```

---

### Method 3: Gradle Command Line

```bash
# Prerequisites: Android SDK set in ANDROID_HOME

# 1. Clone and enter directory
git clone https://github.com/YOUR-USERNAME/GEO-Vision.git
cd GEO-Vision

# 2. Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 3. Build APK
./gradlew build

# 4. Install on device
./gradlew installDebug

# 5. Launch on device
adb shell am start -n com.geovision.mobile/.MainActivity
```

---

## ⚙️ Initial Setup

### First Launch

1. **Accept Permissions**
   - Files: Required for reading GIS files
   - Location: Optional, for GPS tracking
   - Camera: Optional, for photo georeferencing

2. **Select Language**
   - Arabic (العربية) - Default
   - English

3. **Choose Theme**
   - Light Mode (default)
   - Dark Mode

4. **View Onboarding** (Optional)
   - Introduction to features
   - Tutorial for basic operations

---

### Permissions Explained

| Permission | Purpose | Required? |
|-----------|---------|-----------|
| **Files** | Read GIS files from storage | ✅ Yes |
| **Location** | GPS tracking, location services | ⚠️ Optional |
| **Camera** | Extract GPS from photos (EXIF) | ⚠️ Optional |
| **Internet** | Not used (fully offline) | ❌ No |

**Grant Permissions**:
```
Settings → Apps → GEO Vision → Permissions
├── Files: Allow
├── Location: Allow (optional)
└── Camera: Allow (optional)
```

---

## 🚀 Quick Start Workflow

### Opening Your First GIS File

#### Step 1: Prepare File
- Download or transfer GIS file to device
- Supported: .geojson, .shp, .kml, .gpkg, etc.
- For Shapefiles: Include .shp, .shx, .dbf files

#### Step 2: Open File in GEO Vision
1. Launch GEO Vision
2. Tap "+" button (New Layer)
3. Select file from storage
4. Tap "Open"

#### Step 3: View Map
- Pan: Drag with two fingers
- Zoom: Pinch to zoom
- Zoom in: Double-tap
- Zoom to layer: Long-press layer → "Zoom to Bounds"

#### Step 4: Inspect Features
1. Tap feature on map
2. View properties in info panel
3. Tap again to close

---

## 📱 Basic Operations

### Managing Layers

**Add Layer**
```
+ button → Select file → Open
```

**Remove Layer**
```
Long-press layer → Remove
```

**Toggle Visibility**
```
Eye icon next to layer name
```

**View Layer Properties**
```
Tap layer name → Properties tab
```

**Zoom to Layer**
```
Long-press layer → Zoom to Bounds
```

---

### Viewing Features

**See Feature Details**
1. Tap feature on map
2. Panel shows:
   - Feature ID
   - Geometry type
   - All attributes
   - Coordinates

**Search Features**
```
Layers tab → Search icon
Type attribute value to find
```

**Filter by Attribute**
```
Layer → Properties → Filters
Set conditions, apply
```

---

### Map Controls

| Control | Action |
|---------|--------|
| **Pan** | Drag map |
| **Zoom In** | Pinch, double-tap, "+" button |
| **Zoom Out** | Pinch, "-" button |
| **Rotate** | Two-finger rotation |
| **Tilt** | Two-finger vertical drag |
| **GPS** | Tap GPS icon (blue location) |
| **Basemap** | Map → Change basemap |

---

### Settings

**Access Settings**
```
☰ Menu → Settings
```

**Available Options**
- Language (العربية / English)
- Theme (Light / Dark)
- Basemap selection
- GPS update frequency
- Memory warnings
- Cache management

---

## 📂 File Management

### Supported File Formats

| Format | File Extension | Example |
|--------|---|---|
| GeoJSON | .geojson, .json | survey_results.geojson |
| Shapefile | .shp (+ .dbf, .shx) | boundaries.shp |
| KML | .kml, .kmz | map_data.kml |
| GeoPackage | .gpkg | project_data.gpkg |
| GeoTIFF | .tif, .tiff | orthophoto.tif |
| FileGDB | .gdb | database.gdb |
| GPX | .gpx | track.gpx |
| CSV | .csv | points.csv |

### Where to Find Files

**Android File Locations**:
```
/sdcard/                           # Internal storage root
├── Downloads/                     # Downloaded files
├── Documents/                     # Document files
├── Pictures/                      # Photo files
└── GEO_Vision/                    # App-specific folder (auto-created)
    ├── Layers/                    # Saved layers
    ├── Projects/                  # Saved projects
    └── Cache/                     # Temporary files
```

### Transferring Files to Device

**Method 1: Android File Transfer (Easiest)**
1. Connect device to computer via USB
2. Run Android File Transfer app
3. Drag-and-drop files to device
4. Open in GEO Vision

**Method 2: USB Cable (Advanced)**
```bash
adb push /path/to/file.geojson /sdcard/Documents/
```

**Method 3: Cloud/Email**
1. Download file from cloud (Google Drive, Dropbox)
2. File saved to Downloads
3. Open in GEO Vision

**Method 4: Web Download**
1. Open Chrome browser
2. Download GIS file
3. File saved to Downloads
4. Open in GEO Vision

---

## 💡 Tips & Tricks

### Optimize Performance

**Large Files?**
1. Split into regions
2. Use GeoPackage format
3. Remove unnecessary attributes
4. Simplify geometries in QGIS first

**Low Device Memory?**
1. Close other apps
2. Disable GPS tracking
3. Use Light Mode
4. Clear cache regularly

**Battery Drain?**
1. Disable GPS tracking (use only when needed)
2. Reduce screen brightness
3. Lower basemap zoom level
4. Use offline mode

---

### Using GPS Features

**Enable Location Tracking**
1. Tap GPS icon (⊙)
2. Grant location permission
3. Blue circle shows current position
4. Yellow circle shows accuracy

**Record Track**
1. GPS icon → Start Recording
2. Navigate to survey area
3. GPS icon → Stop Recording
4. Track saved to project

**Find Coordinates**
1. Tap location on map
2. Coordinates shown in popup
3. Tap coordinates to copy

---

### Exporting Results

**Export Layer**
```
Layer → Export
Format options:
- GeoJSON (.geojson)
- GeoPackage (.gpkg)
- Shapefile (.shp)
```

**Save Project**
```
File → Save Project
All layers saved together
Can be reopened later
```

**Share via Email/Cloud**
```
File → Share
Choose sharing method
(Google Drive, OneDrive, Email, etc.)
```

---

## 🔧 Troubleshooting Quick Fixes

### App Won't Start
```bash
# Clear app cache
Settings → Apps → GEO Vision → Storage → Clear Cache

# Uninstall and reinstall
Settings → Apps → GEO Vision → Uninstall
Then reinstall from Play Store/GitHub Releases
```

### File Won't Open
```
Possible causes:
1. File format not supported
2. File corrupted
3. Missing file permissions
4. Incompatible format variant

Solutions:
- Check file extension
- Open in QGIS to verify
- Grant file permissions
- Convert to GeoPackage in QGIS
```

### Memory Error
```
Too many features loaded?
Solution:
1. Close other layers
2. Reduce visible area (zoom out)
3. Use smaller file
4. Restart app
```

### GPS Not Working
```
Not getting location?
1. Enable Location services
   Settings → Location → On

2. Grant permission
   Settings → Apps → GEO Vision → Permissions → Location

3. Enable high accuracy
   Settings → Location → Mode → High Accuracy

4. Wait 30 seconds for GPS fix
```

---

## 📚 Learning Resources

### In-App Help
- Help icon (?) in each screen
- Tooltips on long-press
- Settings → User Guide

### Online Resources
- [README.md](README.md) - Full documentation
- [GitHub Discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions) - Q&A
- [YouTube Tutorials](https://youtube.com/@geovision) - Video guides

### External GIS Resources
- [OpenGeoSpatial.org](https://www.ogc.org/) - Standards
- [QGIS Tutorials](https://docs.qgis.org/) - GIS concepts
- [ArcGIS Online](https://www.arcgis.com/) - Sample data

---

## 🐛 Still Having Issues?

1. **Check [ISSUES.md](ISSUES.md)** - Known issues and workarounds
2. **Search [GitHub Issues](https://github.com/YOUR-USERNAME/GEO-Vision/issues)** - Similar problems
3. **Ask on [Discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions)** - Community help
4. **Report [New Issue](https://github.com/YOUR-USERNAME/GEO-Vision/issues/new)** - Include logs and details

---

## 📊 Frequently Asked Questions

**Q: Can I edit data in GEO Vision?**  
A: Not in v1.0. This feature is planned for v2.0. Currently, the app is read-only.

**Q: Does the app require internet?**  
A: No! The app works fully offline. Internet is never required.

**Q: What's the maximum file size?**  
A: Tested up to 500MB on devices with 2GB+ RAM. Depends on your device.

**Q: Can I use this for production work?**  
A: Yes, but it's recommended for review/inspection. For critical analysis, use desktop GIS.

**Q: How do I update the app?**  
A: Check Play Store or GitHub Releases for updates. Manual updates available on GitHub.

**Q: Is my data secure?**  
A: Yes. No data is uploaded to cloud. All processing is local on your device.

---

## 🎓 Next Steps

1. ✅ Install GEO Vision
2. ✅ Grant permissions
3. ✅ Load first GIS file
4. ✅ Explore features
5. 📖 Read full [README.md](README.md)
6. 💬 Join [discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions)
7. 🐛 Report issues if found
8. 🌟 Star the project on GitHub!

---

**Enjoy mapping with GEO Vision! 🗺️**

*Last Updated: January 2026*
