package com.xu4.aqiapp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class MlocationListener implements LocationListener {
  private static final String TAG = "MlocationListener";

  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, location.getLatitude() + " " + location.getLongitude());
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }
}
