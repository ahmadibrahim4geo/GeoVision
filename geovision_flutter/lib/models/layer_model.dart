import 'package:flutter/material.dart';

enum FileType {
  shapefile('Shapefile'),
  geoJson('GeoJSON'),
  kml('KML'),
  gpx('GPX'),
  photo('Photo'),
  geoPackage('GeoPackage'),
  geoDatabase('GeoDatabase');

  final String label;
  const FileType(this.label);
}

class LayerStyle {
  final Color fillColor;
  final Color strokeColor;
  final double strokeWidth;
  final double fillAlpha;
  final double pointSize;
  final double lineWidth;

  const LayerStyle({
    this.fillColor = const Color(0xFFE65100),
    this.strokeColor = const Color(0xFFE65100),
    this.strokeWidth = 2.0,
    this.fillAlpha = 0.3,
    this.pointSize = 8.0,
    this.lineWidth = 4.0,
  });
}

class LayerModel {
  final String id;
  final String name;
  final FileType fileType;
  final String? geomType;
  final bool isVisible;
  final Color color;
  final double transparency;
  final double pointSize;
  final double lineWidth;
  final int featureCount;
  final double progressPercent;
  final String? filePath;
  final String? groupName;
  final int minZoom;
  final int maxZoom;
  final LayerStyle style;
  final String crs;

  const LayerModel({
    required this.id,
    required this.name,
    required this.fileType,
    this.geomType,
    this.isVisible = true,
    this.color = const Color(0xFFE65100),
    this.transparency = 1.0,
    this.pointSize = 8.0,
    this.lineWidth = 4.0,
    this.featureCount = 0,
    this.progressPercent = 1.0,
    this.filePath,
    this.groupName,
    this.minZoom = 1,
    this.maxZoom = 22,
    this.style = const LayerStyle(),
    this.crs = 'EPSG:4326',
  });

  LayerModel copyWith({
    String? id,
    String? name,
    FileType? fileType,
    String? geomType,
    bool? isVisible,
    Color? color,
    double? transparency,
    double? pointSize,
    double? lineWidth,
    int? featureCount,
    double? progressPercent,
    String? filePath,
    String? groupName,
    int? minZoom,
    int? maxZoom,
    LayerStyle? style,
    String? crs,
  }) {
    return LayerModel(
      id: id ?? this.id,
      name: name ?? this.name,
      fileType: fileType ?? this.fileType,
      geomType: geomType ?? this.geomType,
      isVisible: isVisible ?? this.isVisible,
      color: color ?? this.color,
      transparency: transparency ?? this.transparency,
      pointSize: pointSize ?? this.pointSize,
      lineWidth: lineWidth ?? this.lineWidth,
      featureCount: featureCount ?? this.featureCount,
      progressPercent: progressPercent ?? this.progressPercent,
      filePath: filePath ?? this.filePath,
      groupName: groupName ?? this.groupName,
      minZoom: minZoom ?? this.minZoom,
      maxZoom: maxZoom ?? this.maxZoom,
      style: style ?? this.style,
      crs: crs ?? this.crs,
    );
  }
}
