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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

data class MarkerData(val position: LatLng, val title: String, val iconRes: Int)

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                viewModel.onPermissionGranted()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                viewModel.onPermissionGranted()
            }
            else -> {
                viewModel.onPermissionDenied()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        setContent {
            MapScreen(viewModel)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val mapState = state) {
            is MapState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MapState.Success -> {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(mapState.userLocation, 12f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    Marker(
                        state = MarkerState(position = mapState.userLocation),
                        title = "You are here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )

                    mapState.markers.forEach { markerData ->
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            icon = bitmapDescriptorFromVector(context, markerData.iconRes)
                        )
                    }
                }
            }
            is MapState.Error -> {
                Text(text = "Location permission denied. Please enable it in settings.", modifier = Modifier.align(Alignment.Center))
            }
        }
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
