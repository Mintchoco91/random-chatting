package com.kj.random_chatting.userLocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kj.random_chatting.databinding.FragmentUserLocationBinding;
import com.kj.random_chatting.messenger.FcmClient;
import com.kj.random_chatting.messenger.FcmInterface;
import com.kj.random_chatting.messenger.NotificationData;
import com.kj.random_chatting.messenger.NotificationRequest;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class UserLocationFragment extends Fragment implements MapView.CurrentLocationEventListener, MapView.POIItemEventListener {
    private static final String TAG = "UserLocationFragment";
    private FragmentUserLocationBinding binding;
    private Context context;
    private FragmentActivity fragmentActivity;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private MapView mapView;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private DatabaseReference mDatabase;
    private List<UserLocationDTO.OutputDTO> userLocations = new ArrayList<UserLocationDTO.OutputDTO>();
    private String myUserId, myUserName, fcmToken;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d(TAG, "??????: " + location.getLatitude() + ", ??????: " + location.getLongitude());
        }
    };

    private final MapView.POIItemEventListener poiItemEventListener = new MapView.POIItemEventListener() {
        @Override
        public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {

        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
            AlertDialog.Builder builder = new AlertDialog.Builder(fragmentActivity);
            builder.setTitle("?????? ??????");
            builder.setMessage(mapPOIItem.getItemName() + "????????? ????????? ?????????????????????????");
            builder.setPositiveButton("???", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // ??????????????? ????????? ???????????? ?????? ????????? ??????
                    NotificationData data = new NotificationData();
                    data.setTitle("?????? ??????");
                    data.setMessage(myUserName + "?????? ????????? ?????????????????????.");
                    data.setUserName(myUserName);

                    NotificationRequest request = new NotificationRequest();
                    String token = (String) mapPOIItem.getUserObject();
                    request.setToken(token);
                    request.setData(data);

                    FcmInterface apiService = FcmClient.getClient().create(FcmInterface.class);
                    Call<ResponseBody> response = apiService.sendNotification(request);

                    response.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Toast.makeText(context, "??????????????? ?????? ???????????? ??????????????? ?????????????????????.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {

                        }
                    });

                }
            });
            builder.setNegativeButton("?????????", null);
            builder.create().show();
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

        }

        @Override
        public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

        }
    };

    @Override
    public void onDestroy() {
        // ??? ?????? ??? DB??? ????????? ??? ?????? ????????? ?????????.
        SharedPreferences prefs = getActivity().getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            mDatabase.child("userLocation").child(userId).removeValue();
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserLocationBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "Log : " + TAG + " -> onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initializeView();
        setListener();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }

    private void setListener() {
        // ?????????
    }

    public void initializeView() {
        Log.d(TAG, "Log : " + TAG + " -> initializeView");
        context = getContext();
        fragmentActivity = getActivity();

        SharedPreferences prefs = getActivity().getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
        myUserId = prefs.getString("userId", null);
        myUserName = prefs.getString("userName", null);
        fcmToken = prefs.getString("fcmToken", null);

        mapView = new MapView(context);
        mapView.setPOIItemEventListener(poiItemEventListener);

        ViewGroup mapViewContainer = binding.mapView;
        mapViewContainer.addView(mapView);

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result) {
                            // PERMISSION GRANTED
                            Toast.makeText(context, "????????? ?????????", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "????????? ?????????");

                            // ????????? ??? ????????? ?????? ???????????? ????????? ????????????.
                            displayUserLocationsOnMap();

                        } else {
                            // PERMISSION NOT GRANTED
                            Toast.makeText(context, "????????? ???????????? ??????", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "????????? ???????????? ??????");
                        }
                    }
                }
        );

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @SuppressLint("MissingPermission")
    private void displayUserLocationsOnMap() {
        // ??? ?????? ????????? ????????? ????????????.
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location myLocation = null;
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) {
                continue;
            }
            if (myLocation == null || location.getAccuracy() < myLocation.getAccuracy()) {
                myLocation = location;
            }
        }

        if (myLocation == null) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 500, locationListener);
                myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (myLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 500, locationListener);
                myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (myLocation == null) {
                Toast.makeText(context, "?????? ??? ????????? ????????? ??? ????????????.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(myLocation.getLatitude(), myLocation.getLongitude()), true);
        mapView.setCurrentLocationEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);


        // ?????? ??? ????????? DB??? ????????????.
        UserLocationDTO.InputDTO dto = UserLocationDTO.InputDTO.builder()
                .userId(Integer.parseInt(myUserId))
                .userName(myUserName)
                .latitude(myLocation.getLatitude())
                .longitude(myLocation.getLongitude())
                .fcmToken(fcmToken)
                .build();
        mDatabase.child("userLocation").child(myUserId).setValue(dto);

        // ???????????? ?????? ???????????? ????????? ????????? ????????????.
        mDatabase.child("userLocation").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userLocations.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    if (childSnapshot.getKey().equals(myUserId)) {
                        continue;
                    }
                    userLocations.add(childSnapshot.getValue(UserLocationDTO.OutputDTO.class));
                }

                if (!userLocations.isEmpty()) {
                    for (UserLocationDTO.OutputDTO userLocation : userLocations) {
                        MapPoint point = MapPoint.mapPointWithGeoCoord(userLocation.getLatitude(), userLocation.getLongitude());
                        MapPOIItem marker = new MapPOIItem();
                        marker.setUserObject(userLocation.getFcmToken());
                        marker.setItemName(userLocation.getUserName());
                        marker.setTag(0);
                        marker.setMapPoint(point);
                        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);
                        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);
                        mapView.addPOIItem(marker);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        Toast.makeText(context, "????????? ?????????", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }
}