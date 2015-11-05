package com.daisy.geofencingabilities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.daisy.geofencingabilities.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, LocationListener {

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private Marker mCurrentPositionMarker;
    private List<Circle> mZones;

    // Constant radius for each geofence zone
    private final float mRadius = 500.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get LocationManager for determining current user location
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, this);

        mZones = new ArrayList<>();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        setCurrentPositionMarker(location);
    }

    /**
     * Action for adding new zone
     * @param latLng -- position of zone center
     */
    @Override
    public void onMapClick(LatLng latLng) {
        mZones.add(mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(mRadius)
                .strokeColor(getResources().getColor(R.color.colorMapCirleStroke))
                .fillColor(getResources().getColor(R.color.colorMapCirleFill))));

        Intent geoIntent = new Intent(this, GeofencingService.class);
        geoIntent.setAction(Consts.ADD_GEOFENCE_ACTION);
        geoIntent.putExtra(Consts.LATITUDE, latLng.latitude);
        geoIntent.putExtra(Consts.LONGITUDE, latLng.longitude);
        geoIntent.putExtra(Consts.RADIUS, mRadius);
        startService(geoIntent);
    }

    /**
     * Action for removing existing zones
     * @param latLng -- position on user tap
     */
    @Override
    public void onMapLongClick(LatLng latLng) {
        Intent geoIntent = new Intent(this, GeofencingService.class);
        geoIntent.setAction(Consts.REMOVE_GEOFENCE_ACTION);
        geoIntent.putExtra(Consts.LATITUDE, latLng.latitude);
        geoIntent.putExtra(Consts.LONGITUDE, latLng.longitude);
        startService(geoIntent);

        // Removing zones out of map
        Iterator geoIterator = mZones.iterator();
        while (geoIterator.hasNext()) {
            Circle zone = (Circle) geoIterator.next();
            float[] results = new float[1];
            Location.distanceBetween(zone.getCenter().latitude, zone.getCenter().longitude, latLng.latitude, latLng.longitude, results);
            if (zone.getRadius() >= results[0]) {
                geoIterator.remove();
            }
        }

        updateMapZones();
    }

    private void updateMapZones() {
        mMap.clear();

        // Add geo zones that left
        for (Circle zone : mZones) {
            mMap.addCircle(new CircleOptions()
                    .center(zone.getCenter())
                    .radius(zone.getRadius())
                    .strokeColor(zone.getStrokeColor())
                    .fillColor(zone.getFillColor()));
        }

        // Add current user position marker on map
        if (mCurrentPositionMarker != null) {
            MarkerOptions options = new MarkerOptions();
            options.anchor(0.5f, 0.5f).position(mCurrentPositionMarker.getPosition());
            mCurrentPositionMarker = mMap.addMarker(options);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        setCurrentPositionMarker(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { /* do nothing */ }

    @Override
    public void onProviderEnabled(String provider) { /* do nothing */ }

    @Override
    public void onProviderDisabled(String provider) { /* do nothing */ }

    private void setCurrentPositionMarker(Location location) {
        if (location != null) {
            if (mCurrentPositionMarker != null) {
                mMap.clear();
            }
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions options = new MarkerOptions();
            options.anchor(0.5f, 0.5f).position(position);
            mCurrentPositionMarker = mMap.addMarker(options);
            // Move camera to the current user position if position was changed
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 11.0f)));
        }
    }
}
