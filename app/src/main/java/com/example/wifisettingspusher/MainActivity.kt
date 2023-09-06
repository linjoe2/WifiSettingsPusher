package com.example.wifisettingspusher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wifisettingspusher.ui.theme.WifiSettingsPusherTheme
import com.google.gson.Gson
import com.hmdm.HeadwindMDM
import com.hmdm.HeadwindMDM.EventHandler
import com.hmdm.MDMService


data class Wifi(
    val ssid: String,
    val securityType: String,
    val password: String,
    val location: String,
)


class MainActivity : AppCompatActivity(), EventHandler {
    private lateinit var headwindMDM: HeadwindMDM
    private lateinit var suggestionPostConnectionReceiver: BroadcastReceiver
    private lateinit var wifiManager: WifiManager

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 999
        private const val SECURITY_TYPE_WPA3 = "WPA3"
        private const val SECURITY_TYPE_WPA2 = "WPA2"
        private const val SECURITY_TYPE_WPA = "WPA"
        private const val SECURITY_TYPE_NA = "N/A"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headwindMDM = HeadwindMDM.getInstance()
//        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
//        setContent {
//            WifiSettingsPusherTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting("test")
//                }
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        if (!headwindMDM.isConnected) {
            if (!headwindMDM.connect(this, this)) {
                Log.d("error", "not connected")
            }
        } else {
            loadSettings()
        }
    }

    override fun onDestroy() {
        headwindMDM.disconnect(this)
        super.onDestroy()
    }

    override fun onHeadwindMDMConnected() {
        loadSettings()
    }

    override fun onHeadwindMDMDisconnected() {
    }

    override fun onHeadwindMDMConfigChanged() {
        loadSettings()
    }

    private fun loadSettings() {
        val wifiSettings = MDMService.Preferences.get("wifiList", "[]")
//        val wifiSettings = """
//[
//    {"location":"BlauweLijn","ssid":"OuterBassShip3","password":"WithBassToSpace", "securityType":"WPA2"},
//    {"location":"Bonkelaar","ssid":"OuterBassShip4","password":"WithBassToSpace", "securityType":"WPA2"}
//]
//"""
// Create a Gson instance
        val gson = Gson()

// Parse the JSON string into an array of objects
        val arrayOfObjects = gson.fromJson(wifiSettings, Array<Wifi>::class.java)
        val ssidList = arrayOfObjects.map { it.location }
        setContentView(R.layout.activity_main)
// Now, arrayOfObjects contains the parsed JSON data as an array of Wifi objects
        val listView = findViewById<ListView>(R.id.listView)
//
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(INSTALL_FAILED_VERIFICATION_FAILURE
            this,
            android.R.layout.simple_list_item_1, // Layout for each item
            ssidList       // Your array of objects
        )

        listView.adapter = arrayAdapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedSsid = arrayOfObjects[position].ssid
            val selectedItem = arrayOfObjects[position]
            val selectedPass = arrayOfObjects[position].password
            // Implement your logic here based on the selected ssid
            connectByWifiNetworkSuggestion(selectedItem, selectedPass)

                Log.d("selected", "loadSettings: $selectedSsid")
        }
    }


    private fun connectByWifiNetworkSuggestion(wifi: Wifi, pass: String) {
    Log.d(TAG, "connectByWifiNetworkSuggestion: wifi=$wifi, pass=$pass")
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
        when (wifi.securityType) {
            SECURITY_TYPE_WPA3 -> suggestion.setWpa3Passphrase(pass)
            SECURITY_TYPE_WPA2 -> suggestion.setWpa2Passphrase(pass)
            SECURITY_TYPE_WPA -> suggestion.setWpa2Passphrase(pass)
            SECURITY_TYPE_NA -> suggestion.setWpa2Passphrase(pass)
            else -> suggestion.setWpa2Passphrase(pass)
        }
        val suggestionsList = listOf(suggestion.build())
        val resultValue = wifiManager.addNetworkSuggestions(suggestionsList)
        val resultKey = when (resultValue) {
            WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> "STATUS_NETWORK_SUGGESTIONS_SUCCESS"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL -> "STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED"
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID"
            else -> ""
        }
        Log.d(TAG, "connectByWifiNetworkSuggestion: result: $resultValue: $resultKey")
        Toast.makeText(this, "result: $resultValue: $resultKey", Toast.LENGTH_SHORT).show()


        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
        suggestionPostConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                Log.d(TAG, "connectByWifiNetworkSuggestion: onReceive: ")
                // do post connect processing here
            }
        }
        registerReceiver(suggestionPostConnectionReceiver, intentFilter)
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WifiSettingsPusherTheme {
        Greeting("Android")
    }
}


