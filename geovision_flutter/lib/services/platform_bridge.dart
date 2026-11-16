import 'dart:async';
import 'package:flutter/services.dart';

/// جسر الاتصال الهجين بين واجهة فلاتر ومحللات أندرويد الأساسية (Kotlin Native Parsers).
class PlatformBridge {
  static const MethodChannel _parserChannel = MethodChannel('com.geovision.mobile/parsers');
  static const EventChannel _gpsChannel = EventChannel('com.geovision.mobile/gps');

  /// استدعاء محلل الملفات الثنائية في أندرويد (Shapefile, GeoPackage, FileGDB, KML, GPX)
  static Future<Map<String, dynamic>> parseFileNative({
    required String filePath,
    required String fileType,
    Map<String, dynamic>? options,
  }) async {
    try {
      final result = await _parserChannel.invokeMethod<Map<dynamic, dynamic>>('parseFile', {
        'filePath': filePath,
        'fileType': fileType,
        'options': options ?? {},
      });
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException catch (e) {
      return {
        'error': e.message ?? 'Unknown native error',
        'success': false,
      };
    }
  }

  /// الاستماع لتحديثات دقة موقع الـ GPS المباشرة من أندرويد
  static Stream<Map<String, dynamic>> get gpsLocationStream {
    return _gpsChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }
}
