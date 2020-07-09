import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screen_recording/flutter_screen_recording.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _time = 0;
  String recordHintText = "Start Record";
  Timer _timer;

  @override
  void initState() {
    FlutterScreenRecording.addRecorderListener((isCompleted, errCode) {
      debugPrint("isCompleted: $isCompleted, errCode: $errCode");
      _stopScreenRecord();
    });
    super.initState();
  }

  @override
  void dispose() {
    _stopTimer();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter Screen Recording'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              SizedBox(height: 50),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Text('Time: '),
                  Text('$_time', style: TextStyle(fontSize: 30)),
                  Text('s'),
                ],
              ),
              SizedBox(height: 50),
              MaterialButton(
                color: Theme.of(context).accentColor,
                onPressed: _toggleScreenRecord,
                child: Text(
                  recordHintText,
                  style: TextStyle(color: Colors.white),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _toggleScreenRecord() async {
    bool isRecording = await FlutterScreenRecording.isRecording;
    debugPrint("isRecording: $isRecording");
    var icBytes = await rootBundle.load("assets/images/ic_notify.png");
    if (isRecording) {
      await _stopScreenRecord();
    } else {
      try {
        await _startScreenRecord(icBytes);
      } on PlatformException catch (e) {
        debugPrint("start error: $e");
      }
    }
  }

  Future _startScreenRecord(ByteData icBytes) async {
    await FlutterScreenRecording.startRecordScreen(
      notificationTitle: "Recording your screen",
      notificationIcon: icBytes.buffer.asUint8List(),
    );
    _startTimer();
    setState(() {
      recordHintText = "Stop";
    });
  }

  Future _stopScreenRecord() async {
    await FlutterScreenRecording.stopRecordScreen();
    _stopTimer();
    setState(() {
      recordHintText = "Start Record";
    });
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(Duration(seconds: 1), (timer) => setState(() => _time = timer.tick));
  }

  void _stopTimer() {
    _timer?.cancel();
    setState(() {
      _time = 0;
    });
  }
}
