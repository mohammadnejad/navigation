package com.majidsoft.navigationlibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NavigationFragment
 *
 * @author Majid Mohammadnejad
 * @version 1.0
 * @email : majidrasht@gmail.com
 * @since 10/9/16
 */
public class NavigationFragment extends Fragment implements
        LocationListener,
        SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = NavigationFragment.class.getName();
    private static final String ARG_PARAM_LAT = "lat";
    private static final String ARG_PARAM2_LNG = "lng";

    private LatLng destLocation;

    private RequestQueue mRequestQueue;

    private static final String MAPS_DIRECTIONS = "http://maps.googleapis.com/maps/api/directions/json";
    private Context instance = null;

    private static final float LEVEL_ZOOM = 17f;
    private static final float LEVEL_TILT = 30f;

    private static final int REQUEST_LOCATION_ = 1;
    private static String[] PERMISSIONS_Location = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private MapView mapView;
    private GoogleMap mGoogleMap;
    private LatLng source;
    private LatLng destination;
    private boolean firstCurrentLocationFound = false;

    private float mDeclination;
    private float lastBearing = 0f;
    private long lastUpdateTime;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private float[] mRotationMatrix = new float[16];
    private LatLng currentLocation;
    private Marker currentMarker;
    private Location mLastLocation;
    private MarkerOptions currentMarkerOptions;

    private boolean routingFinished;
    private List<LatLng> latLngs;

    private MapUtil mapUtil;

    public NavigationFragment() {
        // Required empty public constructor
    }

    public static NavigationFragment newInstance(double lat, double lng) {
        NavigationFragment fragment = new NavigationFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_PARAM_LAT, lat);
        args.putDouble(ARG_PARAM2_LNG, lng);
        fragment.setArguments(args);
        return fragment;
    }

    //*************************** Android Fragment life cycle***************************************
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        instance = getContext();

        if (getArguments() != null) {
            double destLat = getArguments().getDouble(ARG_PARAM_LAT);
            double destLng = getArguments().getDouble(ARG_PARAM2_LNG);
            destLocation = new LatLng(destLat, destLng);
        }

        mapUtil = new MapUtil();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated");

        mapView = (MapView) view.findViewById(R.id.mapView);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        if (mGoogleApiClient.isConnected())
            startLocationUpdates();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        createLocationRequest();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        if (mGoogleApiClient == null)
            stopLocationUpdates();

        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

    //******************************* initial methods **********************************************
    private void init() {
        Log.e(TAG, "onCreate -> init");

        setGoogleApi();

        initialMapFragment();

        initialSensorManager();
    }

    private void initialSensorManager() {
        mSensorManager = (SensorManager) instance.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        lastUpdateTime = System.currentTimeMillis();
    }

    //*************************** SensorEventListener Override methods *****************************
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
//            Log.i(TAG, event.sensor.getName() + "");

            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(mRotationMatrix, orientation);

            long actulTime = event.timestamp;

            float bearing = (float) Math.toDegrees(orientation[0]) + mDeclination;

            if (Math.abs(bearing - lastBearing) < 2 || Math.abs(actulTime - lastUpdateTime) < 50) {
                return;
            }

            lastUpdateTime = actulTime;
            updateCamera(bearing);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.e(TAG, "accuracy change : " + i + "");
    }


    //*************************** GoogleApiClient override methods**********************************
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "google client api connected");

        if (ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        PERMISSIONS_Location,
                        REQUEST_LOCATION_);
            }

        } else {
            Log.i(TAG, "requestLocationUpdates");
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        currentMarkerOptions = new MarkerOptions();
        currentMarkerOptions.title("current location'");
        currentMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.gps_indicator));

        if (mLastLocation != null) {
            if (currentMarker == null) {
                Log.i(TAG, "add gps marker in connect");

                currentMarker = mGoogleMap.addMarker(currentMarkerOptions.position(
                        new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                );
            } else {
                Log.i(TAG, "change position connect marker");
                currentMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "google client api suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "google client api failed");
    }

    //************************ Map initialize methods***********************************************
    private void setGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(instance)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void initialMapFragment() {
        mapView.onCreate(null);
        mapView.onResume();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
            }
        });
    }

    private void initSourceLocation(Location location) {
        if (location != null) {
            source = new LatLng(location.getLatitude(), location.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions().position(source);
            markerOptions.title("source");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

            mGoogleMap.addMarker(markerOptions);

            goToLocation(source.latitude, source.longitude);
        }
    }

    private void initDestinationLocation(LatLng location) {
        if (location != null) {
            destination = new LatLng(location.latitude, location.longitude);

            MarkerOptions markerOptions = new MarkerOptions().position(destination);
            markerOptions.title("destination");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            mGoogleMap.addMarker(markerOptions);
        }
    }

    private void goToLocation(double lat, double lon) {
        CameraPosition cameraPosition = CameraPosition.builder()
                .target(new LatLng(lat, lon))
                .zoom(LEVEL_ZOOM)
                .tilt(LEVEL_TILT)
                .build();

        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), null);
    }

    private void updateCamera(float bearing) {
//        Log.e(TAG, "move camera: " + bearing);
        lastBearing = bearing;

        if (mGoogleMap != null && currentLocation != null && currentLocation.longitude != 0 && currentLocation.latitude != 0) {
            CameraPosition oldPos = mGoogleMap.getCameraPosition();

            CameraPosition pos = new CameraPosition.Builder()
                    .target(currentLocation)
                    .zoom(18f)
                    .bearing(bearing)
                    .tilt(50)
                    .build();

            mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 400, null);
        }
    }

    //************************* LocationListener Override method ***********************************
    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "location update : " + "lat = " + location.getLatitude() +
                " , lon = " + location.getLongitude() +
                " , accuracy = " + location.getAccuracy() +
                " , provider = " + location.getProvider() +
                " , time ; " + DateFormat.getTimeInstance().format(new Date()));

        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        // in route gps
        if (routingFinished && currentLocation.longitude != 0 && currentLocation.longitude != 0) {
            currentLocation = mapUtil.getProjectLocation(latLngs, currentLocation);
        }

        if (!firstCurrentLocationFound) {
            initSourceLocation(location);
            initDestinationLocation(destLocation);
            if (destination != null)
                routing();
            firstCurrentLocationFound = true;
        }

        GeomagneticField field = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );

        // getDeclination returns degrees
        mDeclination = field.getDeclination();

        if (firstCurrentLocationFound) {
            if (currentMarker != null) {
                Log.i(TAG, "change position marker");

                currentMarker.setPosition(currentLocation);
            } else {
                Log.i(TAG, "add gps marker");

                currentMarker = mGoogleMap.addMarker(currentMarkerOptions.position(currentLocation));
            }
        }
    }

    //************************ Location request ****************************************************
    private void createLocationRequest() {
        Log.i(TAG, "create location request");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(7000); // 10000 is normal
        mLocationRequest.setFastestInterval(3000); // 5000 is normal
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        Log.i(TAG, "start update location");

        if (ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        PERMISSIONS_Location,
                        REQUEST_LOCATION_);
            }

        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.i(TAG, "requestLocationUpdates");
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case REQUEST_LOCATION_: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean locationEnabled = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(instance, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "onRequestPermissionsResult : if 1");
                        return;
                    }
                    Log.i(TAG, "onRequestPermissionsResult : ok");
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }

    }

    private void stopLocationUpdates() {
        Log.i(TAG, "stop location update");

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    //************************** Routing ***********************************************************
    private void routing() {
        drawDirectionRequest(source, destination);
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(instance);
        }
        return mRequestQueue;
    }

    public <T> void add(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    private void drawDirectionRequest(LatLng origin, LatLng destination) {
        CustomJsonRequest request = new CustomJsonRequest(Request.Method.GET, makeURLForDirection(origin, destination), null,

                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        new DrawDirectionTask().execute(response.toString());
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    }
                }
        );

        add(request);
    }

    public static String makeURLForDirection(LatLng origin, LatLng destination) {

        StringBuilder urlString = new StringBuilder();
        urlString.append(MAPS_DIRECTIONS);
        urlString.append("?origin=");
        urlString.append(Double.toString(origin.latitude));
        urlString.append(",");
        urlString.append(Double.toString(origin.longitude));
        urlString.append("&destination=");
        urlString.append(Double.toString(destination.latitude));
        urlString.append(",");
        urlString.append(Double.toString(destination.longitude));
        urlString.append("&sensor=false&mode=driving&alternatives=true");

        return urlString.toString();
    }

    public static List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    /**
     * draw path from the result(json cast to string) of direction api callback used in draw direction method
     *
     * @param result
     */
    private List<LatLng> parseJsonFromGoogle(String result) {
        List<LatLng> latLongs = null;
        try {
            latLongs = new ArrayList<>();

            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);

            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);

            JSONArray legsArray = routes.getJSONArray("legs");
            JSONObject leg = legsArray.getJSONObject(0);
            JSONArray stepsArray = leg.getJSONArray("steps");

            for (int j = 0; j < stepsArray.length(); j++) {
                JSONObject step = stepsArray.getJSONObject(j);

                JSONObject polyLine = step.getJSONObject("polyline");
                latLongs.addAll(decodePoly(polyLine.getString("points")));
            }
        } catch (JSONException e) {
            Log.e("test", e.getMessage() + "");
        }

        return latLongs;
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class DrawDirectionTask extends AsyncTask<String, Integer, List<LatLng>> {
        // Parsing the data in non-ui thread
        @Override
        protected List<LatLng> doInBackground(String... jsonData) {
            List<LatLng> routes = null;

            try {
                routes = parseJsonFromGoogle(jsonData[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<LatLng> result) {
            latLngs = result;

            for (int z = 0; z < result.size() - 1; z++) {
                LatLng src = result.get(z);
                LatLng dest = result.get(z + 1);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.dot));
                mGoogleMap.addMarker(markerOptions.position(new LatLng(src.latitude, src.longitude)));

                @SuppressWarnings("unused")
                Polyline line = mGoogleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(15)
                        .color(Color.BLUE).geodesic(true));
            }

            routingFinished = true;
        }
    }

}
