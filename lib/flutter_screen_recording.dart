import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';

typedef OnRecorderListener = Function(bool isCompled, int errCode);

class FlutterScreenRecording {
  static const ERROR_CODE_PERMISSION_DENIED = "ERROR_CODE_PERMISSION_DENIED";
  static const ERROR_CODE_CANCEL = "ERROR_CODE_CANCEL";
  static const ERROR_CODE_PLATFORM_NOT_SUPPORT = "ERROR_CODE_PLATFORM_NOT_SUPPORT";

  static const MethodChannel _channel = const MethodChannel('com.isvisoft/flutter_screen_recording');
  static FlutterScreenRecording _instance = FlutterScreenRecording._();

  final List<OnRecorderListener> recorderListeners = [];

  FlutterScreenRecording._() {
    _channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case "onRecorderListener":
          recorderListeners.forEach(
                (element) => element?.call(call.arguments["isCompleted"] as bool, call.arguments["errCode"] as int),
          );
          break;
      }
    });
  }

  /// Start record screen
  static Future<String> startRecordScreen({
    String outputPath,
    String fileName,
    bool isAudioEnabled,
    bool isRecordHDVideo,
    int audioBitrate,
    int audioSamplingRate,
    int videoBitrate,
    int videoFrameRate,
    Uint8List notificationIcon,
    String notificationTitle,
    String notificationButtonText,
    String notificationDescription,
  }) async {
    var args = {
      "outputPath": outputPath,
      "fileName": fileName,
      "isAudioEnabled": isAudioEnabled,
      "isRecordHDVideo": isRecordHDVideo,
      "audioBitrate": audioBitrate,
      "audioSamplingRate": audioSamplingRate,
      "videoBitrate": videoBitrate,
      "videoFrameRate": videoFrameRate,
      "notificationIcon": notificationIcon,
      "notificationTitle": notificationTitle,
      "notificationButtonText": notificationButtonText,
      "notificationDescription": notificationDescription,
    };
    return await _channel.invokeMethod('startRecordScreen', args);
  }

  /// Stop record screen
  static Future<void> stopRecordScreen() async {
    return await _channel.invokeMethod('stopRecordScreen');
  }

  /// Whether is recording screen
  static Future<bool> get isRecording async {
    return await _channel.invokeMethod('isRecording');
  }

  /// Add recorder listener
  static addRecorderListener(OnRecorderListener onRecorderListener) {
    _instance.recorderListeners.add(onRecorderListener);
  }

  /// Remove recorder listener
  static removeRecorderListener(OnRecorderListener onRecorderListener) {
    _instance.recorderListeners.remove(onRecorderListener);
  }
}