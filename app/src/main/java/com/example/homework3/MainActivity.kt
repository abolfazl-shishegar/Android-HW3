package com.example.homework3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var networkChangeReceiver: BroadcastReceiver
    private val isConnected = mutableStateOf(true)
    private val connectionLogs = mutableStateListOf<JSONObject>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNetworkCallback()
        } else {
            networkChangeReceiver = NetworkChangeReceiver()
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).also {
                registerReceiver(networkChangeReceiver, it)
            }
        }

        setContent {
            NetworkStatusScreen(isConnected.value, connectionLogs)
        }
        checkInitialNetworkState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            unregisterReceiver(networkChangeReceiver)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true
                showNotification(isConnected.value)
                logConnectionStatus(isConnected.value)
            }

            override fun onLost(network: Network) {
                isConnected.value = false
                showNotification(isConnected.value)
                logConnectionStatus(isConnected.value)
            }
        })
    }

    private fun checkInitialNetworkState() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isConnected.value = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        showNotification(isConnected.value)
        logConnectionStatus(isConnected.value)
    }

    inner class NetworkChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            isConnected.value = activeNetworkInfo?.isConnected == true
            showNotification(isConnected.value)
            logConnectionStatus(isConnected.value)
        }
    }

    private fun showNotification(isConnected: Boolean) {
        val notificationText = if (isConnected) "Connected to Internet" else "Disconnected"
        val channelId = "network_status_channel"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle("Network Status")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Network Status"
            val descriptionText = "Shows the network connectivity status"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logConnectionStatus(isConnected: Boolean) {
        val connectionStatus = if (isConnected) "Connected" else "Disconnected"
        val statusLog = JSONObject().apply {
            put("Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("Connection type", "Internet")
            put("Status", connectionStatus)
        }

        val logsFile = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "connection_logs.json")
        val logs = if (logsFile.exists() && logsFile.length() != 0L) {
            JSONArray(logsFile.readText())
        } else {
            JSONArray()
        }
        logs.put(statusLog)
        FileWriter(logsFile, false).use { it.write(logs.toString()) }

        connectionLogs.add(0, statusLog)
    }


}

@Composable
fun NetworkStatusScreen(isConnected: Boolean, connectionLogs: List<JSONObject>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (isConnected) "Connected to Internet" else "Disconnected")
        }
    }
}
