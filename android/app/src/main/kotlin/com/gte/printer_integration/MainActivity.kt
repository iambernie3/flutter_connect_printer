package com.gte.printer_integration

import android.os.Bundle
import android.os.Looper
import android.util.Log
import honeywell.connection.ConnectionBase
import honeywell.connection.Connection_Bluetooth
import honeywell.printer.DocumentExPCL_LP
import honeywell.printer.ParametersExPCL_LP
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity: FlutterActivity() {
    private val CHANNEL = "do/printer_channel"
    var conn: ConnectionBase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provideFlutterEngine(this)?.let { GeneratedPluginRegistrant.registerWith(it) }
        flutterEngine?.dartExecutor?.let {
            MethodChannel(it, CHANNEL).setMethodCallHandler{ call, result ->
                if(call.method.equals("print")){
                    val textToPrint = call.argument<String>("text").toString()
                    startPrinting(textToPrint)
                } else if(call.method.equals("register_mac")){
                    val macAddress = call.argument<String>("mac").toString()
                    val connect = registerDeviceMacToConnectionBase(macAddress)
                    Log.e("MainActivity","macAddress : $macAddress")
                    Log.e("MainActivity","connect : $connect")
                    result.success(connect)
                }else{
                    result.notImplemented()
                }
            }
        }
    }

    private fun registerDeviceMacToConnectionBase(macAddress: String): Boolean {
        android.os.Handler(Looper.getMainLooper()).run {
            try {
                conn?.let {
                    if(it.isActive){
                        if (!it.isOpen) {
                            it.open()
                        }

                        return@let true
                    } else {
                        return false
                    }
                }

                conn = Connection_Bluetooth.createClient(macAddress, false)
                //Open bluetooth socket
                conn?.let {
                    if (!it.isOpen) {
                        it.open()
                    }

                    return true
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

        return false

    }

    private fun startPrinting(text:String) {
        val docExPCL_LP = DocumentExPCL_LP(3) //Line Print mode. “3” is the font index.
        val fontStyleParam = ParametersExPCL_LP()
        fontStyleParam.fontIndex = 1
        fontStyleParam.isBold = true
        docExPCL_LP.writeText("  $text", fontStyleParam)
//        docExPCL_LP.writeText("  Central Negros Electric Coop. Inc.", fontStyleParam)
//        fontStyleParam.isBold = false
//        docExPCL_LP.writeText("       Cor. Mabini-Gonzaga St., Bacolod City 6100", fontStyleParam)
//        docExPCL_LP.writeText("            458-6777 local 1101,1101,1103", fontStyleParam)
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
        runBlocking {
            launch {
                var bytesWritten = 0
                var bytesToWrite = 1024
                val totalBytes: Int = docExPCL_LP.documentData.size
                var remainingBytes = totalBytes
                while (bytesWritten < totalBytes) {
                    if (remainingBytes < bytesToWrite) bytesToWrite = remainingBytes

                    //Send data, 1024 bytes at a time until all data sent
                    conn!!.write(docExPCL_LP.documentData, bytesWritten, bytesToWrite)
                    bytesWritten += bytesToWrite
                    remainingBytes -= bytesToWrite
                }
            }
        }
    }
}
