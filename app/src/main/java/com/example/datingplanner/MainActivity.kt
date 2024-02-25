package com.example.datingplanner

import android.content.pm.PackageManager

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest

// The geographical location where the device is currently located. That is, the last-known
// location retrieved by the Fused Location Provider.
private var lastKnownLocation: Location? = null
private var placesClient: PlacesClient? = null;

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var locationPermissionGranted = false
    val defaultLocation = LatLng(-34.0, 151.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // construct places client
        Places.initialize(applicationContext, BuildConfig.MAP_API_KEY);
        placesClient = Places.createClient(this);


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        searchPlaces("uc berkeley")
    }

    @SuppressLint("MissingPermission")
    private fun searchPlaces(query: String) {
        Log.e(TAG,"this thing on?")
        val filters = mutableListOf<String>()
        filters.addAll(listOf(PlaceTypes.CLOTHING_STORE))
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setTypesFilter(filters)
            .setLocationBias(RectangularBounds.newInstance(
                LatLng(37.8637, -122.2517),
                LatLng(37.8675, -122.2671)  // Example: New York City coordinates

            ))
            .build()
        placesClient?.findAutocompletePredictions(request)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.e(TAG,"got a successful response")
                var result = task.result
                for (prediction in result.autocompletePredictions) {
                    Log.e(TAG, "Place: ${prediction.placeId}, Name: ${prediction.getPrimaryText(null)}")
                    // Process each search result here
                }
            } else {
                val exception = task.exception
                Log.e("NearbyClothingStores", "Failed to fetch nearby clothing stores: $exception")
            }
        }
    }

    private fun processFindPlaceResponse(response: FindCurrentPlaceResponse, query: String) {
        for (placeLikelihood in response.placeLikelihoods) {
            val place = placeLikelihood.place
            Log.i("FindPlace", "Found place: ${place.name}, LatLng: ${place.latLng}")

            // Check if the found place matches the query
            if (place.name == query) {
                fetchPlaceDetails(place.id)
                return
            }
        }
        Log.e("FindPlace", "No matching place found for query: $query   ")
    }

    private fun fetchPlaceDetails(placeId: String) {
        val request = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.NAME, Place.Field.LAT_LNG))

        placesClient?.fetchPlace(request)?.addOnSuccessListener { response: FetchPlaceResponse ->
            val place = response.place
            Log.i("FetchPlaceDetails", "Place details - Name: ${place.name}, LatLng: ${place.latLng}")
            // Handle the place details here
        }?.addOnFailureListener { exception: Exception ->
            Log.e("FetchPlaceDetails", "Failed to fetch place details: $exception")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        getLocationPermission()
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
        searchPlaces("clothing")
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }



    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }
}

