import 'package:flutter_test/flutter_test.dart';
import 'package:geovision_flutter/main.dart';

void main() {
  testWidgets('GeoVision Flutter app Smoke Test', (WidgetTester tester) async {
    await tester.pumpWidget(const GeoVisionFlutterApp());
    expect(find.text('مدير الطبقات الذكي (Flutter Hybrid)'), findsOneWidget);
  });
}
