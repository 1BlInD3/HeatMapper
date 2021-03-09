package com.example.heatmapperkotlin

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.WifiRttManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.maps.android.PolyUtil
import com.google.maps.android.heatmaps.WeightedLatLng
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var request : LocationRequest
    private lateinit var locationCallback : LocationCallback
    private lateinit var wifiManager : WifiManager
    private lateinit var wifiRttManager : WifiRttManager
    private var classification: ArrayList<kotlin.Int> = ArrayList()
    private var dBmValue: ArrayList<kotlin.Int> = ArrayList()
    private var latlong : ArrayList<LatLng> = ArrayList()
    private var lat : ArrayList<Double> = ArrayList()
    private var lon : ArrayList<Double> = ArrayList()
    private var weightedList : ArrayList<WeightedLatLng> = ArrayList()
    private val mapsFragment = MapsFragment()
    private var poly : ArrayList<LatLng> = ArrayList()
    private val TAG = "MainActivity"
    private var clicked = 0
    private lateinit var req : RangingRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wifiRttManager = applicationContext.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager

        val StartPositionUpdates = findViewById<Button>(R.id.startGps)
        val ShowHeatMap = findViewById<Button>(R.id.heatMap)
        ShowHeatMap.isEnabled = false
        StartPositionUpdates.setOnClickListener{
            filterRttAps()
            if(clicked == 0)
            {
               // mapsFragment.FusetechOverlay()
                startLocationUpdates()
                clicked = 1
                StartPositionUpdates.setText("Mérés folyamatban")
                ShowHeatMap.isEnabled = true
            }
            else
            {
                StartPositionUpdates.setText("GPS")
                clicked = 0
                Logger(weightedList)
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
        ShowHeatMap.setOnClickListener{

            mapsFragment.AddHeatMap(weightedList)//ide egy wighted latlong
        }
        supportFragmentManager.beginTransaction().replace(R.id.frame_container,mapsFragment).commit()

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        request = LocationRequest.create().apply {
            interval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
        val result : Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        poly.add(LatLng(46.378603, 17.825902))
        poly.add(LatLng(46.377766, 17.825649))
        poly.add(LatLng(46.377635, 17.826645))
        poly.add(LatLng(46.378460, 17.826862))

        locationCallback =object:LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations)
                {
                    if(PolyUtil.containsLocation(location.latitude,location.longitude,poly,false))
                    {
                        Log.d(TAG, "onLocationResult: "+ location.latitude)
                        Log.d(TAG, "onLocationResult: "+ location.longitude)
                        mapsFragment.putPos(location.latitude,location.longitude)
                        lat.add(location.latitude)
                        lon.add(location.longitude)
                        CheckdBm()
                        latlong.add(LatLng(location.latitude,location.longitude))
                        var weightedLatLng : WeightedLatLng = WeightedLatLng(latlong.last(),classification.last().toDouble())
                        weightedList.add(weightedLatLng)
                    }
                }
            }
        }

        result.addOnFailureListener{exception ->
            if(exception is ResolvableApiException)
            {
                try {
                    exception.startResolutionForResult(this@MainActivity,1)
                }catch (sendEx : IntentSender.SendIntentException){
                    Log.d(TAG, "onCreate: Catch"+sendEx)
                }
            }
        }

        val packageManager = this.packageManager
        val rttSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        Log.d(TAG, "onCreate: isSupported $rttSupported")


    }

    fun startLocationUpdates()
    {
        if(fusedLocationClient != null)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {                //Permission Not Granted
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
            }
            else
            {
                fusedLocationClient.requestLocationUpdates(request,locationCallback, Looper.getMainLooper())
            }
        }
    }
    fun filterRttAps()
    {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Permission Not Granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        } else {
            val wifimanage = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults : ArrayList<ScanResult> = wifimanage.scanResults as ArrayList<ScanResult>
            for(scanresult in scanResults)
            {
                if (scanresult.is80211mcResponder)
                {
                    Log.d(TAG, "filterRttAps: true")
                    
                }
                else
                {
                    Log.d(TAG, "filterRttAps: false")
                    var a = scanresult.BSSID
                    Log.d(TAG, "filterRttAps: $a")
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    fun CheckdBm(){
        try {
            var wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.rssi > -50) {
                classification.add(5)
            } else if (wifiInfo.rssi < -50 && wifiInfo.rssi > -60) {
                classification.add(4)
            } else if (wifiInfo.rssi < -60 && wifiInfo.rssi > -70) {
                classification.add(3)
            } else if (wifiInfo.rssi < -70 && wifiInfo.rssi > -80) {
                classification.add(2)
            } else {
                classification.add(1)
            }
            dBmValue.add(wifiInfo.rssi)
        }catch (e: IntentSender.SendIntentException){
            Log.d(TAG, "CheckdBm: $e")
            dBmValue.add(0)
        }
    }

    fun Logger(myList : ArrayList<WeightedLatLng>)
    {
        val path : File = this.getExternalFilesDir(null) as File
        val file = File(path,"coordinates.txt")
        var data : String
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val stream : FileOutputStream = FileOutputStream(file,true);
                for (i in weightedList.indices)
                {
                    data = lat[i].toString()+","+lon[i].toString()+","+classification[i]+"\n"
                    stream.write(data.toByteArray())
                }
                stream.close()
            }catch (e : IntentSender.SendIntentException){
                Log.d(TAG, "Logger: $e")
            }
        }
    }
}