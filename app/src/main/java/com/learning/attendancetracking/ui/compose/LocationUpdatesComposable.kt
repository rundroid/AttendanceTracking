package com.learning.attendancetracking.ui.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
@Composable
fun LocationUpdatesComposable(
    locationRequest: LocationRequest = remember { getLocationRequest() },
    onLocationChange: (Location, Address) -> Unit
) {
    val currentLocationChange = rememberUpdatedState(onLocationChange).value
    val context = LocalContext.current
    var isDisposed by remember { mutableStateOf(false) }
    val locationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val geocoder = remember { Geocoder(context) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.Default }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (isDisposed) {
                    return
                }
                locationResult.locations.getOrNull(0)?.let { location ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) { addresses ->
                            if (isDisposed.not()) {
                                addresses.getOrNull(0)?.let { address: Address ->
                                    currentLocationChange.invoke(location, address)
                                }
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                ?.getOrNull(0)?.let { address ->
                                    if (isDisposed.not()) {
                                        currentLocationChange.invoke(location, address)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (isDisposed.not() && activityResult.resultCode == Activity.RESULT_OK) {
                locationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            }
        }
    DisposableEffect(locationRequest) {
        val executor = Executors.newSingleThreadExecutor()
        LocationServices.getSettingsClient(context).checkLocationSettings(
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        ).addOnCompleteListener(executor) { task ->
            if (isDisposed) {
                return@addOnCompleteListener
            }
            if (task.isSuccessful) {
                locationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            } else {
                (task.exception as? ResolvableApiException)?.resolution?.let {
                    launcher.launch(IntentSenderRequest.Builder(it).build())
                }
            }
        }
        onDispose {
            executor.shutdownNow()
            isDisposed = true
            locationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
}

private fun getLocationRequest(): LocationRequest {
    return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL).setWaitForAccurateLocation(true).build()
}