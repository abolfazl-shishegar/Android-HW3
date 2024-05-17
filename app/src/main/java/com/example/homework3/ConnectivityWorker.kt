package com.example.homework3

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ConnectivityWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        checkAirplaneMode()
        checkBluetoothStatus()
        return Result.success()
    }

    private fun checkAirplaneMode() {
        val isAirplaneModeOn = Settings.Global.getInt(
            applicationContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
        val status = if (isAirplaneModeOn) "Airplane Mode ON" else "Airplane Mode OFF"
        Log.i("worker_airplane", status)
        logStatus("Airplane Mode", status)
    }

    private fun checkBluetoothStatus() {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val status = if (bluetoothAdapter.isEnabled) "Bluetooth ON" else "Bluetooth OFF"
        Log.i("worker_airplane", status)
        logStatus("Bluetooth", status)
    }

    private fun logStatus(connectionType: String, status: String) {
        val statusLog = JSONObject().apply {
            put("Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("Connection type", connectionType)
            put("Status", status)
        }

        val publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logsFile = File(publicDocsDir, "connection_logs.json")
        if (!logsFile.exists()) {
            logsFile.createNewFile()
        }
        val logs = if (logsFile.length() == 0L) JSONArray() else JSONArray(logsFile.readText())
        logs.put(statusLog)
        FileWriter(logsFile, false).use { it.write(logs.toString()) }
    }
}
