package com.maps.custommarker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.random.Random

data class MarkerData(val position: LatLng, val title: String, val iconRes: Int)

class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            }
            else -> {
                // No location access granted.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        setContent {
            MapScreen()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var markers by remember { mutableStateOf<List<MarkerData>>(emptyList()) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                userLocation = latLng
                markers = generateRandomMarkers(latLng)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userLocation == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            userLocation?.let { location ->
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(location, 12f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )

                    markers.forEach { markerData ->
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            icon = bitmapDescriptorFromVector(context, markerData.iconRes)
                        )
                    }
                }
            }
        }
    }
}

private fun generateRandomMarkers(center: LatLng): List<MarkerData> {
    val markerCategories = listOf(
        "Tire Shop" to R.drawable.ic_tire_shop,
        "Pharmacy/Capsule" to R.drawable.ic_pharmacy,
        "General Store" to R.drawable.ic_general_store,
        "Restaurant" to R.drawable.ic_restaurant,
        "Meat Shop" to R.drawable.ic_meat_shop
    )

    return markerCategories.map { (title, iconRes) ->
        val randomLatLng = center.let {
            val radius = 1000 + Random.nextDouble(0.0, 1000.0) // 1-2 km
            val angle = Random.nextDouble(0.0, 360.0)
            val x = radius * kotlin.math.cos(Math.toRadians(angle))
            val y = radius * kotlin.math.sin(Math.toRadians(angle))
            val earthRadius = 6371000.0
            val newLat = it.latitude + (y / earthRadius) * (180 / Math.PI)
            val newLng = it.longitude + (x / earthRadius) * (180 / Math.PI) / kotlin.math.cos(it.latitude * Math.PI / 180)
            LatLng(newLat, newLng)
        }
        MarkerData(randomLatLng, title, iconRes)
    }
}

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
