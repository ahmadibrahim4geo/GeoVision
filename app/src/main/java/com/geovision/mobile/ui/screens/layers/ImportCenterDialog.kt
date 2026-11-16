package com.geovision.mobile.ui.screens.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class ImportSourceOption(
    val titleAr: String,
    val subtitleAr: String,
    val extensions: String,
    val icon: ImageVector,
    val badgeColor: Color
) {
    EXCEL_CSV(
        titleAr = "جداول الإكسيل والإحداثيات (Excel / CSV)",
        subtitleAr = "استيراد نقاط مساحية مع تحديد حقول X و Y وربط باقي الأعمدة كسمات",
        extensions = ".xlsx, .xls, .csv, .tsv, .txt",
        icon = Icons.Default.TableChart,
        badgeColor = Color(0xFF2E7D32)
    ),
    GEOPACKAGE(
        titleAr = "قواعد بيانات GeoPackage (OGC GPKG)",
        subtitleAr = "استعراض طبقات وجداول GeoPackage المكانية المدمجة",
        extensions = ".gpkg",
        icon = Icons.Default.Storage,
        badgeColor = Color(0xFF1565C0)
    ),
    ESRI_GDB(
        titleAr = "قواعد بيانات Esri (GDB / Geodatabase)",
        subtitleAr = "قراءة File Geodatabase (.gdb مجلد/مضغوط) و Mobile Geodatabase",
        extensions = ".gdb, .geodatabase, .zip",
        icon = Icons.Default.FolderOpen,
        badgeColor = Color(0xFF6A1B9A)
    ),
    SHAPEFILE(
        titleAr = "ملفات Shapefile (ESRI SHP)",
        subtitleAr = "استيراد أرشيف مضغوط (.zip) أو ملف .shp مع ملفات التوابع .dbf و .prj",
        extensions = ".zip, .shp",
        icon = Icons.Default.Layers,
        badgeColor = Color(0xFFE65100)
    ),
    KML_KMZ(
        titleAr = "ملفات Google Earth (KML / KMZ)",
        subtitleAr = "استيراد معالم ونقاط ومسارات ومضلعات بتنسيق Google Earth",
        extensions = ".kml, .kmz",
        icon = Icons.Default.Place,
        badgeColor = Color(0xFFC2185B)
    ),
    GEOJSON(
        titleAr = "بيانات الويب المفتوحة (GeoJSON / JSON)",
        subtitleAr = "المعيار العالمي المفتوح لتبادل البيانات الجغرافية المكانية",
        extensions = ".geojson, .json",
        icon = Icons.Default.Code,
        badgeColor = Color(0xFF00838F)
    ),
    GEO_PHOTOS(
        titleAr = "الصور الجغرافية (GeoTagged Photos)",
        subtitleAr = "استخراج إحداثيات GPS الحقلية من صور الكاميرا وعرضها على الخريطة",
        extensions = ".jpg, .jpeg, .png",
        icon = Icons.Default.PhotoCamera,
        badgeColor = Color(0xFFEF6C00)
    )
}

@Composable
fun ImportCenterDialog(
    onDismiss: () -> Unit,
    onSelectOption: (ImportSourceOption) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                "مركز استيراد البيانات الجغرافية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "اختر نوع الملف أو قاعدة البيانات المراد إضافتها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // List of options
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ImportSourceOption.values()) { option ->
                        ImportOptionCard(
                            option = option,
                            onClick = {
                                onSelectOption(option)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Footer button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    option: ImportSourceOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(option.badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    option.icon,
                    contentDescription = null,
                    tint = option.badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        option.titleAr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    option.subtitleAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = option.badgeColor.copy(alpha = 0.08f)
                ) {
                    Text(
                        option.extensions,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = option.badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronLeft, // In RTL it points forwards to the right/left
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
