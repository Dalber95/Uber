package com.parse.starter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    ListView requestListView;
    ArrayAdapter<String> arrayAdapter;

    ArrayList<String> requests = new ArrayList<>();
    ArrayList<Double> requestLatitudes = new ArrayList<>();
    ArrayList<Double> requestLongitudes = new ArrayList<>();
    ArrayList<String> usernames = new ArrayList<>();

    LocationManager locationManager;
    LocationListener locationListener;

    public void updateListView(Location location) {
        if (location != null) {
            ParseQuery<ParseObject> query = new ParseQuery<>("Request");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            query.whereNear("location", geoPointLocation);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        requests.clear();
                        requestLongitudes.clear();
                        requestLatitudes.clear();

                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");

                                if (requestLocation != null) {
                                    double distanceInKilometers = geoPointLocation.distanceInKilometersTo(requestLocation);
                                    double distanceOneDP = (double) Math.round(distanceInKilometers * 10) / 10;
                                    requests.add(distanceOneDP + " kilometers");

                                    requestLatitudes.add(requestLocation.getLatitude());
                                    requestLongitudes.add(requestLocation.getLongitude());
                                    usernames.add(object.getString("username"));
                                }
                            }
                        } else {
                            requests.add("No active requests nearby");
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("Nearby Requests");

        requestListView = findViewById(R.id.requestListView);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requests);

        requests.clear();
        requests.add("Getting nearby requests...");

        requestListView.setAdapter(arrayAdapter);

        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (requestLatitudes.size() > position && requestLongitudes.size() > position && usernames.size() > position && lastKnownLocation != null) {
                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);

                        intent.putExtra("requestLatitude", requestLatitudes.get(position));
                        intent.putExtra("requestLongitude", requestLongitudes.get(position));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(position));

                        startActivity(intent);
                    }
                }
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);

                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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
        };

        if (Build.VERSION.SDK_INT < 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    updateListView(lastKnownLocation);
                }
            }
        }
    }
}
