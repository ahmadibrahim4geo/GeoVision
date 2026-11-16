import 'package:flutter/material.dart';
import 'screens/flutter_layer_manager_screen.dart';

void main() => runApp(const GeoVisionFlutterApp());

class GeoVisionFlutterApp extends StatelessWidget {
  const GeoVisionFlutterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'GeoVision Flutter Hybrid',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.orange,
        scaffoldBackgroundColor: const Color(0xFF10141C),
        useMaterial3: true,
      ),
      home: const FlutterLayerManagerScreen(),
    );
  }
}
