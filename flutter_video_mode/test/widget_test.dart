import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_video_mode/main.dart';

void main() {
  testWidgets('Video app smoke test', (WidgetTester tester) async {
    // Ignore network image errors during test
    final originalOnError = FlutterError.onError;
    FlutterError.onError = (FlutterErrorDetails details) {
      if (details.exception is Exception && details.exception.toString().contains('HTTP request failed')) {
        return;
      }
      originalOnError?.call(details);
    };

    await tester.pumpWidget(const CinematicVideoApp());

    // Verify that our app name is present.
    expect(find.text('STREAMIFY'), findsOneWidget);

    // Verify that hero section has the play button.
    expect(find.text('Play'), findsOneWidget);

    FlutterError.onError = originalOnError;
  });
}
