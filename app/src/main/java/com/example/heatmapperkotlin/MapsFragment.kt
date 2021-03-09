package com.example.heatmapperkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng

class MapsFragment : Fragment() {

    private lateinit var mMap : GoogleMap
    private lateinit var heatmapTileProvider: HeatmapTileProvider
    private lateinit var tileOverlay : TileOverlay
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        val fusetech = LatLng(46.378136, 17.826268)
        val myPosition = LatLng(46.378479,17.826337)
        googleMap.addMarker(MarkerOptions().position(myPosition))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fusetech,18.5f))
        googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18.5f), 2000, null)
        val boundaries = LatLngBounds(
            LatLng(46.377455,17.825488),
            LatLng(46.378870,17.827027)
        )
        val fuseMap = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.fuse2))
            .positionFromBounds(boundaries)
            .transparency(0.7f)
        googleMap.addGroundOverlay(fuseMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
    fun putPos(lat : Double, lon : Double)
    {
        val point = LatLng(lat,lon)
        mMap.addMarker(MarkerOptions().position(point)).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point,18.5f))
        mMap.animateCamera(CameraUpdateFactory.zoomIn())
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18.5f), 2000, null)
    }
    fun AddHeatMap( heatMapList : ArrayList<WeightedLatLng>)
    {
        heatmapTileProvider = HeatmapTileProvider.Builder()
            .weightedData(heatMapList)
            .build()
        tileOverlay = mMap.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(heatmapTileProvider))

    }
    fun RemoveHeatMap()
    {
        tileOverlay.remove()
    }
    fun FusetechOverlay()
    {
        val boundaries = LatLngBounds(
            LatLng(46.377629,17.825485),
            LatLng(46.378701,17.827028)
        )
        val fuseMap = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.fusetech))
            .positionFromBounds(boundaries)
        mMap.addGroundOverlay(fuseMap)
    }
}