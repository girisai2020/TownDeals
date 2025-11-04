package com.maps.custommarker

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

sealed class MapState {
    object Loading : MapState()
    data class Success(
        val userLocation: LatLng,
        val markers: List<MarkerData>,
        val selectedMarker: MarkerData? = null
    ) : MapState()
    object Error : MapState()
}

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<MapState>(MapState.Loading)
    val state: StateFlow<MapState> = _state

    @SuppressLint("MissingPermission")
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun onPermissionGranted() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                generateRandomMarkers(latLng)
            } else {
                _state.value = MapState.Error
            }
        }.addOnFailureListener {
            _state.value = MapState.Error
        }
    }

    fun onPermissionDenied() {
        _state.value = MapState.Error
    }

    fun onMarkerSelected(marker: MarkerData) {
        val currentState = _state.value
        if (currentState is MapState.Success) {
            _state.value = currentState.copy(selectedMarker = marker)
        }
    }

    fun onBottomSheetDismissed() {
        val currentState = _state.value
        if (currentState is MapState.Success) {
            _state.value = currentState.copy(selectedMarker = null)
        }
    }

    private fun generateRandomMarkers(center: LatLng) {
        val markerCategories = listOf(
            "Tire Shop" to R.drawable.ic_tire_shop,
            "Pharmacy/Capsule" to R.drawable.ic_pharmacy,
            "Gas Station" to R.drawable.ic_gas_station,
            "Restaurant" to R.drawable.ic_restaurant,
            "Food Truck" to R.drawable.ic_food_truck
        )

        val markers = markerCategories.map { (title, iconRes) ->
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
            val distance = calculateDistance(center, randomLatLng)
            MarkerData(randomLatLng, title, iconRes, "10% off after 9:00 PM", distance)
        }
        _state.value = MapState.Success(center, markers)
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0] / 1609.34f // convert to miles
    }
}