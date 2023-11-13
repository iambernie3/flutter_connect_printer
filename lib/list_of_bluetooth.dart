import 'dart:async';
import 'dart:io';

import 'package:bluetooth_connector/bluetooth_connector.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

class ListOfBL extends StatefulWidget {
  const ListOfBL({super.key});

  @override
  State<ListOfBL> createState() => _ListOfBLState();
}

class _ListOfBLState extends State<ListOfBL> {
  BluetoothConnector flutterbluetoothconnector = BluetoothConnector();
  List<BtDevice> devices = [];
  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    // _adapterStateStateSubscription.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    scanBl(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('List of BL'),
      ),
      body: SafeArea(
        child: ListView.builder(
          itemBuilder: (context, index) {
            final data = devices[index];
            return InkWell(
              child: ListTile(
                title: Text(data.address.toString()),
                subtitle: Text(data.name.toString()),
              ),
              onTap: () async {
                const platform = MethodChannel('do/printer_channel');
                final printStatus =
                    await platform.invokeMethod('register_mac', {
                  'mac': data.address.toString(),
                });
                if (printStatus) {
                  await platform.invokeMethod('print', {
                    'text': '  Central Negros Electric Coop. Inc.',
                  });
                } else {
                  const snackBar =
                      SnackBar(content: Text("Printer is not connected..."));
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(snackBar);
                  }
                }
              },
            );
          },
          itemCount: devices.length,
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
          onPressed: () async {
            devices = await flutterbluetoothconnector.getDevices();
            setState(() {});
          },
          label: const Text('Scan devices')),
    );
  }

  void scanBl(BuildContext context) async {
    if (await FlutterBluePlus.isSupported == false) {
      AlertDialog alert = AlertDialog(
        title: const Text("Opss!"),
        content: const Text("The device is not bluetooth supperted"),
        actions: [
          TextButton(
            child: const Text("Close"),
            onPressed: () {},
          ),
        ],
      );

      // show the dialog
      if (context.mounted) {
        showDialog(
          context: context,
          builder: (BuildContext context) {
            return alert;
          },
        );
      }
      return;
    }

    if (Platform.isAndroid) {
      await FlutterBluePlus.turnOn();
    }
  }
}
