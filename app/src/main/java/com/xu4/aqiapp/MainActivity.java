package com.xu4.aqiapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class MainActivity extends AppCompatActivity {
  // global stuff
  private static final String TAG = "MainActivityLog";
  private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

  // views
  private TextView aqiTextView;
  private TextView additionalInfoTextView;
  private TextView infoTextView;
  private Button button;

  // other
  private AqiApi aqiApi;
  private List<AqiModel> aqiModelList;
  private LocationManager locationManager;
  private boolean locationPermissionsGranted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    aqiApi =
        new Retrofit.Builder()
            .baseUrl("http://opendata2.epa.gov.tw/AQI.json/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(AqiApi.class);

    additionalInfoTextView = findViewById(R.id.moreInfo_textView);
    aqiTextView = findViewById(R.id.aqi_textView);
    infoTextView = findViewById(R.id.info_textView);
    button = findViewById(R.id.button);

    button.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {

            checkLocationPermissions();
            if (locationPermissionsGranted) {
              Log.d(TAG, "locationPermissionsGranted");
              if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                  || !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(MainActivity.this, "請開啟定位設定，並選擇高精確度", Toast.LENGTH_LONG).show();
                Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(onGPS);
              } else {
                Toast.makeText(MainActivity.this, "取得空汙資訊", Toast.LENGTH_SHORT).show();
                getAqi();
              }
            } else Log.d(TAG, "not locationPermissionsGranted");
          }
        });

    locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
    checkLocationPermissions();
  }

  public void getAqi() {
    retrofit2.Call<List<AqiModel>> call = aqiApi.getAqi("http://opendata2.epa.gov.tw/AQI.json");
    call.enqueue(
        new Callback<List<AqiModel>>() {
          @Override
          public void onResponse(Call<List<AqiModel>> call, Response<List<AqiModel>> response) {
            aqiModelList = response.body();
            refreshText();
          }

          @Override
          public void onFailure(Call<List<AqiModel>> call, Throwable t) {
            Log.d(TAG, "failed");
            t.printStackTrace();
          }
        });
  }

  private void refreshText() {
    // get current location
    Location currentLocation = getLastBestLocation();

    if (currentLocation != null) {
      // get nearest station
      double currentLat = currentLocation.getLatitude();
      double currentLon = currentLocation.getLongitude();

      AqiModel nearest = aqiModelList.get(0);
      double distance =
          calcDistance(
              currentLat,
              Double.parseDouble(nearest.getLatitude()),
              currentLon,
              Double.parseDouble(nearest.getLongitude()));

      for (AqiModel aqiModel : aqiModelList) {
        double newDistance =
            calcDistance(
                currentLat,
                Double.parseDouble(aqiModel.getLatitude()),
                currentLon,
                Double.parseDouble(aqiModel.getLongitude()));

        if (newDistance < distance && !aqiModel.getAqi().isEmpty()) {
          nearest = aqiModel;
          distance = newDistance;
        }
      }

      String currentLatString = Double.toString(currentLat);
      String currentLonString = Double.toString(currentLon);

      String additionalInfo =
          "測站地點: "
              + nearest.getCounty()
              + ", "
              + nearest.getSiteName()
              + "\n測站經緯度: "
              + nearest.getLongitude()
              + ", "
              + nearest.getLatitude()
              + "\n與測站距離: "
              + (int) distance
              + "公尺"
              + "\n當前GPS位置: "
              + currentLonString
              + ", "
              + currentLatString;

      String aqiInfo = "AQI: " + nearest.getAqi() + " 狀態: " + nearest.getStatus();
      aqiTextView.setText(aqiInfo);
      additionalInfoTextView.setText(additionalInfo);

      double aqi = Double.parseDouble(nearest.getAqi());

      String colorString;
      String infoText;

      if (aqi >= 301) {
        colorString = "#FF990100";
        infoText = "健康威脅達到緊急，所有人都可能受到影響";
      } else if (aqi >= 201) {
        colorString = "#FF990098";
        infoText = "健康警報：所有人都可能產生較嚴重的健康影響";
      } else if (aqi >= 151) {
        colorString = "#FFFE0000";
        infoText = "對所有人的健康開始產生影響，對於敏感族群可能產生較嚴重的健康影響";
      } else if (aqi >= 101) {
        colorString = "#FFFF9934";
        infoText = "空氣汙染可能會對敏感族群的健康造成影響，但是對一般大眾的影響不明顯";
      } else if (aqi >= 51) {
        colorString = "#FFFFFF00";
        infoText = "空氣品質普通；但對非常少數之極敏感族群產生輕微影響";
      } else {
        colorString = "#FF00FF01";
        infoText = "空氣品質為良好，污染程度低或無污染";
      }

      aqiTextView.setTextColor(Color.parseColor(colorString));
      infoTextView.setText(infoText);
      infoTextView.setTextColor(Color.parseColor(colorString));
      infoTextView.setBackgroundColor(Color.parseColor("#000000"));
      aqiTextView.setBackgroundColor(Color.parseColor("#000000"));

    } else {
      aqiTextView.setText("目前無定位資訊 請稍候三分鐘再重試");
    }
  }

  private double calcDistance(double lat1, double lat2, double lon1, double lon2) {
    final int R = 6371; // Radius of the earth

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c * 1000;
  }

  private void checkLocationPermissions() {
    // if permission not granted, request permission
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          MainActivity.this,
          new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
          },
          MY_PERMISSIONS_REQUEST_LOCATION);
    } else locationPermissionsGranted = true;
  }

  /** @return the last known best location */
  @SuppressLint("MissingPermission")
  private Location getLastBestLocation() {
    Log.d(TAG, "getLastBestLocation");

    @SuppressLint("MissingPermission")
    Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    @SuppressLint("MissingPermission")
    Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

    long gpsLocationTime = 0;
    if (null != locationGPS) gpsLocationTime = locationGPS.getTime();

    long netLocationTime = 0;
    if (null != locationNet) netLocationTime = locationNet.getTime();

    //    locationGPS = null;
    //    locationNet = null;

    if (locationGPS == null && locationNet == null) {
      Log.d(TAG, "all locations are null");
      //      locationManager.requestLocationUpdates(
      //          LocationManager.GPS_PROVIDER, 1000, 1, mlocationListener);
      //      try {
      //        Thread.sleep(3000);
      //      } catch (InterruptedException e) {
      //        e.printStackTrace();
      //      }
      //
      //      locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      //      locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      //      if (null != locationGPS) gpsLocationTime = locationGPS.getTime();
      //      if (null != locationNet) netLocationTime = locationNet.getTime();
      SingleShotLocationProvider.requestSingleUpdate(
          this,
          new SingleShotLocationProvider.LocationCallback() {
            @Override
            public void onNewLocationAvailable(SingleShotLocationProvider.GPSCoordinates location) {
              Log.d("onNewLocationAvailable", "my location is " + location.toString());
            }
          });

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      if (null != locationGPS) gpsLocationTime = locationGPS.getTime();
      if (null != locationNet) netLocationTime = locationNet.getTime();
    }

    if (0 < gpsLocationTime - netLocationTime) return locationGPS;
    else return locationNet;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

    if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
      // If request is cancelled, the result arrays are empty.
      if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        // permission denied
        Toast.makeText(this, "請允許權限！", Toast.LENGTH_LONG).show();
        locationPermissionsGranted = false;
      } else {
        locationPermissionsGranted = true;
      }
    }
  }
}
