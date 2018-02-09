package com.swyftpartner.driver.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.activity.ChatActivity;
import com.swyftpartner.driver.activity.MainActivity;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.location.LocationHelper;
import com.swyftpartner.driver.model.RequestDetails;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.CarAnimation.AnimateMarker;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;
import com.swyftpartner.driver.utils.chathead.ChatHeadService;
import com.swyftpartner.driver.utils.chathead.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by user on 1/12/2017.
 */

public class TravelMapFragment extends Fragment implements LocationHelper.OnLocationReceived, AsyncTaskCompleteListener, View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap googleMap;
    private Bundle mBundle;
    private SupportMapFragment driver_travel_map;
    private View view;
    private LocationHelper locHelper;
    private Location myLocation;
    private TextView tripStatusButton, userName, userMobileNumber, userRating, addressTitle, srcDesAddress;
    private MainActivity activity;
    private int jobStatus = 0;
    private PreferenceHelper pHelper;
    private Bundle requestBundle;
    private RequestDetails requestDetails;
    private ImageView userIcon;
    private ParseContent pContent;
    private LatLng srcLang;
    private LatLng desLang;
    private Marker pickup_marker, drop_marker, currentMarker;
    private Location lastLocation, currentLocation;
    private float bearing = 0.0f;
    private String mobileNo = "";
    private boolean isMarkerRotating = false;
    private Handler reqHandler;
    private boolean isShown = false;
    private ArrayList<LatLng> driverlatlan;
    int mIndexCurrentPoint = 0;
    private List<LatLng> mPathPolygonPoints;
    Bitmap mMarkerIcon;
    private LatLng delayLatlan;
    private HashMap<Marker, Integer> mHashMap = new HashMap<Marker, Integer>();
    long starttime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedtime = 0L;
    int t = 1;
    int secs = 0;
    int mins = 0;
    int milliseconds = 0;
    int hours = 0;
    int count = 0;
    Handler timerHandler;
    private String duration = "";
    private int trip_duration = 0;
    Polyline poly_line;
    public static int OVERLAY_PERMISSION_REQ_CODE_CHATHEAD = 1234;
    private LinearLayout btn_direction, driver_contact, cancel_trip;


    Runnable runnable = new Runnable() {
        public void run() {
            if (requestDetails != null) {
                checkRequestStatus(requestDetails.getRequestId());
                reqHandler.postDelayed(this, 4000);
                tripStatusButton.setEnabled(true);
            }
        }
    };


    private Socket mSocket;
    private Boolean isConnected = true;
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {

                        if (requestDetails != null) {
                            JSONObject object = new JSONObject();

                            try {
                                object.put("receiver", requestDetails.getClientId());
                                object.put("sender", pHelper.getUserId());
                                Log.e("update_object", "" + object);
                                mSocket.emit("update sender", object);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            isConnected = true;
                        }
                    }
                    if (isConnected) {

                        JSONObject object = new JSONObject();

                        try {
                            object.put("receiver", requestDetails.getClientId());
                            object.put("sender", pHelper.getUserId());
                            Log.e("update_object", "" + object);
                            mSocket.emit("update sender", object);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }
            });
        }
    };
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;

                }
            });
        }
    };
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };
    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String sender;
                    String receiver;
                    String latitude;
                    String longitude;
                    try {
                        sender = data.getString("sender");
                        receiver = data.getString("receiver");
                        latitude = data.getString("latitude");
                        longitude = data.getString("longitude");

                    } catch (JSONException e) {
                        return;
                    }
                    Log.d("mahi", "new message recived" + receiver + " " + sender + " " + latitude + " " + longitude + " " + data.toString());
                }
            });
        }
    };
    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }
/*
                    addLog(getResources().getString(R.string.message_user_joined, username));
                    addParticipantsLog(numUsers);*/
                }
            });
        }
    };
    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };
    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };
    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = savedInstanceState;

        activity = (MainActivity) getActivity();
        pHelper = new PreferenceHelper(activity);
        timerHandler = new Handler();
        driverlatlan = new ArrayList<>();
        mPathPolygonPoints = new ArrayList<LatLng>();
        mMarkerIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_booking_lux_map_topview);
        reqHandler = new Handler();
        requestBundle = getArguments();
        if (requestBundle != null) {
            jobStatus = requestBundle.getInt(Const.PROVIDER_STATUS,
                    Const.IS_PROVIDER_ACCEPTED);
            requestDetails = (RequestDetails) requestBundle.getSerializable(
                    Const.REQUEST_DETAIL);
            mobileNo = requestDetails.getClientPhoneNumber();
        }


        try {
            mSocket = IO.socket(Const.ServiceType.SOCKET_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("message", onNewMessage);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.connect();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.travel_fragment, container,
                false);

        pHelper = new PreferenceHelper(activity);
        pContent = new ParseContent(activity);
        userIcon = (ImageView) view.findViewById(R.id.iv_user);
        userName = (TextView) view.findViewById(R.id.tv_userName);
        userMobileNumber = (TextView) view.findViewById(R.id.tv_userMobileNumber);
        srcDesAddress = (TextView) view.findViewById(R.id.tv_current_location);
        addressTitle = (TextView) view.findViewById(R.id.address_title);

        tripStatusButton = (TextView) view.findViewById(R.id.tv_trip_status);
        btn_direction = (LinearLayout) view.findViewById(R.id.btn_direction);


        btn_direction.setOnClickListener(this);

        tripStatusButton.setEnabled(false);
        tripStatusButton.setOnClickListener(this);
        srcDesAddress.setSelected(true);


        driver_travel_map = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.driver_travel_map);

        if (null != driver_travel_map) {
            driver_travel_map.getMapAsync(this);
        }
        srcDesAddress.setOnClickListener(this);


        cancel_trip = (LinearLayout) view.findViewById(R.id.cancel_trip);
        cancel_trip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final iOSDialog cancelDialog = new iOSDialog(activity);
                cancelDialog.setTitle(getResources().getString(R.string.txt_cancel_ride));
                cancelDialog.setSubtitle(getResources().getString(R.string.cancel_txt));

                cancelDialog.setNegativeLabel(getResources().getString(R.string.txt_no));
                cancelDialog.setPositiveLabel(getResources().getString(R.string.txt_yes));
                cancelDialog.setBoldPositiveLabel(false);
                cancelDialog.setNegativeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cancelDialog.dismiss();
                    }
                });
                cancelDialog.setPositiveListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        stopCheckingUpcomingRequests();
                        cancelRide();
                        cancelDialog.dismiss();
                    }
                });
                cancelDialog.show();
            }
        });

        ((LinearLayout) view.findViewById(R.id.driver_contact)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final iOSDialog ContactDialog = new iOSDialog(activity);
                ContactDialog.setTitle(getResources().getString(R.string.txt_contact_user));
                ContactDialog.setSubtitle(mobileNo);

                ContactDialog.setNegativeLabel(getResources().getString(R.string.txt_call));
                ContactDialog.setPositiveLabel(getResources().getString(R.string.txt_msg));
                ContactDialog.setBoldPositiveLabel(false);
                ContactDialog.setNegativeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mobileNo.equals("")) {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + mobileNo));
                            startActivity(callIntent);

                            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                //request permission from user if the app hasn't got the required permission
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.CALL_PHONE},   //request specific permission from user
                                        10);
                                return;

                            } else {     //have got permission
                                try {
                                    startActivity(callIntent);  //call activity and make phone call
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toast.makeText(activity, "yourActivity is not founded", Toast.LENGTH_SHORT).show();
                                }
                            }


                        }
                        ContactDialog.dismiss();
                    }
                });
                ContactDialog.setPositiveListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendnotification();
                        if (requestDetails != null) {

                            sendnotification();
                            Intent i = new Intent(activity, ChatActivity.class);
                            i.putExtra("reciver_id", requestDetails.getClientId());
                            startActivity(i);
                        }
                        ContactDialog.dismiss();
                    }
                });
                ContactDialog.show();
            }
        });


        return view;
    }

    private void setJobStatus(int jobStatus) {

        switch (jobStatus) {
            case Const.IS_PROVIDER_ACCEPTED:
                tripStatusButton.setText(activity.getResources().getString(R.string.btn_status_txt_1));
                if (null != delayLatlan)
                    getDirections(Double.valueOf(requestDetails.getsLatitude()), Double.valueOf(requestDetails.getsLongitude()), delayLatlan.latitude, delayLatlan.longitude);
                break;
            case Const.IS_PROVIDER_STARTED:
                tripStatusButton.setText(activity.getResources().getString(R.string.btn_status_txt_2));
                if (null != delayLatlan)
                    getDirections(Double.valueOf(requestDetails.getsLatitude()), Double.valueOf(requestDetails.getsLongitude()), delayLatlan.latitude, delayLatlan.longitude);
                break;
            case Const.IS_PROVIDER_ARRIVED:
                cancel_trip.setVisibility(View.GONE);
                addressTitle.setText(getString(R.string.drop_address));
                addressTitle.setTextColor(Color.parseColor("#ff0000"));
                if (!requestDetails.getDestinationAddress().equals("")) {
                    srcDesAddress.setText(requestDetails.getDestinationAddress());
                } else {
                    srcDesAddress.setText("--Not Available--");
                }
                if (null != delayLatlan && requestDetails.getdLatitude() != null && requestDetails.getdLongitude() != null) {
                    getDirections(delayLatlan.latitude, delayLatlan.longitude, Double.valueOf(requestDetails.getdLatitude()), Double.valueOf(requestDetails.getdLongitude()));
                }
                tripStatusButton.setText(activity.getResources().getString(R.string.btn_status_txt_3));
                break;
            case Const.IS_PROVIDER_SERVICE_STARTED:
                addressTitle.setText(getString(R.string.drop_address));
                addressTitle.setTextColor(Color.parseColor("#ff0000"));
                if (!requestDetails.getDestinationAddress().equals("")) {
                    srcDesAddress.setText(requestDetails.getDestinationAddress());
                } else {
                    srcDesAddress.setText(getResources().getString(R.string.txt_not_avialbel));
                }
                tripStatusButton.setText(activity.getResources().getString(R.string.btn_status_txt_4));
                if (null != delayLatlan && requestDetails.getdLatitude() != null && requestDetails.getdLongitude() != null) {
                    getDirections(delayLatlan.latitude, delayLatlan.longitude, Double.valueOf(requestDetails.getdLatitude()), Double.valueOf(requestDetails.getdLongitude()));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }


    public Runnable updateTimer = new Runnable() {

        public void run() {


            timeInMilliseconds = System.currentTimeMillis() - starttime;


            duration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeInMilliseconds),
                    TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds) % TimeUnit.MINUTES.toSeconds(1));
            String[] units = duration.split(":");
            int hours = Integer.valueOf(units[0]);
            int minutes = Integer.valueOf(units[1]);
            int seconds = Integer.valueOf(units[2]);

            trip_duration = 3600 * hours + 60 * minutes + seconds;


            timerHandler.postDelayed(this, 0);
        }

    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            MapsInitializer.initialize(getActivity());
        } catch (Exception e) {
        }

        locHelper = new LocationHelper(activity);
        locHelper.setLocationReceivedLister(this);


        if (requestDetails != null) {
            userName.setText(requestDetails.getClientName());
//            userRating.setText(String.valueOf(requestDetails.getUserRating()));
            userMobileNumber.setText(getResources().getString(R.string.txt_mobile) + " " + requestDetails.getClientPhoneNumber());
            Glide.with(activity).load(requestDetails.getClientProfile()).error(R.drawable.passenger).into(userIcon);
            srcDesAddress.setText(requestDetails.getSourceAddress());
            startCheckingUpcomingRequests();

        }
        setJobStatus(jobStatus);

        if (jobStatus == 4) {
            startTimer();
        }


    }

    private void setSourceDestinationMarkerOnMap() {
        if (requestDetails != null) {
            srcLang = new LatLng(Double.valueOf(requestDetails.getsLatitude()), Double.valueOf(requestDetails.getsLongitude()));
            desLang = new LatLng(Double.valueOf(requestDetails.getdLatitude()), Double.valueOf(requestDetails.getdLongitude()));

            /*googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(srcLang,
                    16));*/


            if (srcLang != null && desLang != null) {


//                if (markerpatient != null) {
//                    markerpatient.remove();
//                }
//                if (markerDcotor != null) {
//                    markerDcotor.remove();
//                }

                MarkerOptions opt = new MarkerOptions();
                opt.position(srcLang);
                opt.title(getResources().getString(R.string.txt_pickup_address));
                opt.anchor(0.5f, 0.5f);
                opt.icon(BitmapDescriptorFactory
                        .fromResource(R.mipmap.pickup_location));
                pickup_marker = googleMap.addMarker(opt);
                mHashMap.put(pickup_marker, 0);
                googleMap.addMarker(opt);
                googleMap.setOnMarkerClickListener(this);
                if (desLang != null && !requestDetails.getDestinationAddress().equals("")) {
                    MarkerOptions doctorOpt = new MarkerOptions();
                    doctorOpt.position(desLang);
                    doctorOpt.title(getResources().getString(R.string.txt_drop_address));
                    doctorOpt.anchor(0.5f, 0.5f);
                    doctorOpt.icon(BitmapDescriptorFactory.fromResource(R.mipmap.drop_location));
                    drop_marker = googleMap.addMarker(doctorOpt);
                    googleMap.addMarker(doctorOpt);
                    mHashMap.put(drop_marker, 1);
                }


                if (pickup_marker != null && drop_marker != null && !requestDetails.getDestinationAddress().equals("")) {
                    fitmarkers_toMap();
                }

                /*if (srcLang != null && desLang != null) {

                    getDirections(Double.valueOf(requestDetails.getsLatitude()), Double.valueOf(requestDetails.getsLongitude()), Double.valueOf(requestDetails.getdLatitude()), Double.valueOf(requestDetails.getdLongitude()));
                }*/


            }

          /*  googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(srcLang,
                    17));*/
        }


    }

    private void getDirections(double latitude, double longitude, double latitude1, double longitude1) {

        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }

        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.DIRECTION_API_BASE + Const.ORIGIN + "="
                + String.valueOf(latitude) + "," + String.valueOf(longitude) + "&" + Const.DESTINATION + "="
                + String.valueOf(latitude1) + "," + String.valueOf(longitude1) + "&" + Const.EXTANCTION);

        new VollyRequester(activity, Const.GET, map, Const.ServiceCode.GOOGLE_DIRECTION_API, this);
    }

    public void drawPath(String result) {


        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);


            PolylineOptions options = new PolylineOptions().width(8).color(Color.BLACK).geodesic(true);
            for (int z = 0; z < list.size(); z++) {
                LatLng point = list.get(z);
                options.add(point);
            }
            if (googleMap != null) {
                if (null != poly_line) {
                    poly_line.remove();
                    poly_line = googleMap.addPolyline(options);
                } else {
                    poly_line = googleMap.addPolyline(options);
                }

            }


           /*
           for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                .width(2)
                .color(Color.BLUE).geodesic(true));
            }
           */
        } catch (JSONException e) {

        }
    }

    private List<LatLng> decodePoly(String encoded) {

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

    private void fitmarkers_toMap() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

//the include method will calculate the min and max bound.
        builder.include(pickup_marker.getPosition());
        builder.include(drop_marker.getPosition());

        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 12% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

        googleMap.moveCamera(cu);
//        if (pic_latlan != null) {
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(pic_latlan)
//                    .zoom(15).build();
//            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


    }


    @Override
    public void onResume() {
        super.onResume();
        activity.currentFragment = Const.TRAVEL_MAP_FRAGMENT;


    }

    @Override
    public void onLocationReceived(LatLng latlong) {

    }

    @Override
    public void onLocationReceived(Location location) {

        if (location != null) {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions mOptions = new MarkerOptions();
            delayLatlan = latLng;
            if (currentMarker == null) {
                if (null != googleMap) {
                    currentMarker = googleMap.addMarker(mOptions
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_booking_lux_map_topview))
                            .title(getResources().getString(R.string.txt_driver)));
                }

                setJobStatus(jobStatus);
                AnimateMarker.animateMarker(activity, location, currentMarker, googleMap);
            } else {
                AnimateMarker.animateMarker(activity, location, currentMarker, googleMap);
            }
            if (mSocket.connected()) {
                attemptSend(latLng, location.getBearing());

            }

            if (null != googleMap) {
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng,
                        16);
                googleMap.moveCamera(update);
            }

        }
    }


    @Override
    public void onConntected(Bundle bundle) {

    }

    @Override
    public void onConntected(Location location) {
        if (location != null) {
            currentLocation = location;
            LatLng currentlatLang = new LatLng(location.getLatitude(), location.getLongitude());

        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.tv_current_location:
            case R.id.btn_direction:

                if (Utils.canDrawOverlays(activity))
                    activity.startService(new Intent(activity, ChatHeadService.class));
                else {
                    requestPermission(OVERLAY_PERMISSION_REQ_CODE_CHATHEAD);
                }

                if (jobStatus == 1 || jobStatus == 2) {
                    Intent sourcenNvi = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=" + requestDetails.getsLatitude() + "," + requestDetails.getsLongitude()));
                    startActivity(sourcenNvi);
                } else {
                    if (!requestDetails.getDestinationAddress().equals("")) {
                        Intent destNavi = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("http://maps.google.com/maps?daddr=" + requestDetails.getdLatitude() + "," + requestDetails.getdLongitude()));
                        startActivity(destNavi);
                    }
                }
                break;
            case R.id.tv_trip_status:
                switch (jobStatus) {
                    case Const.IS_PROVIDER_ACCEPTED:
                        providerStarted();
                        break;
                    case Const.IS_PROVIDER_STARTED:

                        providerArrived();
                        break;
                    case Const.IS_PROVIDER_ARRIVED:
                        addressTitle.setText(getString(R.string.drop_address));
                        addressTitle.setTextColor(Color.parseColor("#ff0000"));
                        if (!requestDetails.getDestinationAddress().equals("")) {
                            srcDesAddress.setText(requestDetails.getDestinationAddress());
                        } else {
                            srcDesAddress.setText(getResources().getString(R.string.txt_not_avialbel));
                        }
                        providerServiceStarted();
                        startTimer();
                        if (null != delayLatlan) {
                            pHelper.putTrip_Start_Lat(String.valueOf(delayLatlan.latitude));
                            pHelper.putTrip_Start_Lan(String.valueOf(delayLatlan.longitude));
                        }
                        break;
                    case Const.IS_PROVIDER_SERVICE_STARTED:
                        addressTitle.setText(getString(R.string.drop_address));
                        addressTitle.setTextColor(Color.parseColor("#ff0000"));
                        if (!requestDetails.getDestinationAddress().equals("")) {
                            srcDesAddress.setText(requestDetails.getDestinationAddress());
                        } else {
                            srcDesAddress.setText(getResources().getString(R.string.txt_not_avialbel));
                        }

                        if (!TextUtils.isEmpty(pHelper.getTrip_Start_Lat()) || !TextUtils.isEmpty(pHelper.getTrip_Start_Lan())) {
                            LatLng s_latlan = new LatLng(Double.valueOf(pHelper.getTrip_Start_Lat()), Double.valueOf(pHelper.getTrip_Start_Lan()));
                            if (null != delayLatlan) {
                                findDistanceAndTime(s_latlan, delayLatlan);
                            }
                        } else {
                            if (null != delayLatlan && null != pickup_marker) {
                                findDistanceAndTime(pickup_marker.getPosition(), delayLatlan);
                            }
                        }


                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void startTimer() {

        if (pHelper.getTrip_time() == 0L) {
            pHelper.putTrip_time(System.currentTimeMillis());
        }

        starttime = pHelper.getTrip_time();
        timerHandler.postDelayed(updateTimer, 0);

    }


    private void sendnotification() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }

        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.USER_MESSAGE_NOTIFY + Const.Params.ID + "="
                + new PreferenceHelper(activity).getUserId() + "&" + Const.Params.TOKEN + "="
                + new PreferenceHelper(activity).getSessionToken() + "&" + Const.Params.REQUEST_ID + "=" + String.valueOf(requestDetails.getRequestId()));
        Log.d("mahi", "send_noty" + map.toString());
        new VollyRequester(activity, Const.GET, map, Const.ServiceCode.USER_MESSAGE_NOTIFY,
                this);
    }

    private void cancelRide() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.POST_CANCEL_TRIP_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        Log.d("mahi", "map 1" + map.toString());
        new VollyRequester(activity, Const.POST, map,
                Const.ServiceCode.POST_CANCEL_TRIP, this);

    }

    private void providerStarted() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_STARTED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        Log.d("mahi", "map 1" + map.toString());
        new VollyRequester(activity, Const.POST, map,
                Const.ServiceCode.PROVIDER_STARTED, this);
    }

    private void providerArrived() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_ARRIVED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        Log.d("mahi", "map 2" + map.toString());
        new VollyRequester(activity, Const.POST, map,
                Const.ServiceCode.PROVIDER_ARRIVED, this);
    }

    private void providerServiceStarted() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_SERVICE_STARTED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        Log.d("mahi", "map 3" + map.toString());
        new VollyRequester(activity, Const.POST, map,
                Const.ServiceCode.PROVIDER_SERVICE_STARTED, this);
    }


    private void startCheckingUpcomingRequests() {
        startCheckRegTimer();
    }

    private void stopCheckingUpcomingRequests() {
        if (reqHandler != null) {
            reqHandler.removeCallbacks(runnable);

            Log.d("mahi", "stop handler");
        }
    }

    public void startCheckRegTimer() {
        reqHandler.postDelayed(runnable, 4000);
    }


    private void providerServiceCompleted(String distance, String time) {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_SERVICE_COMPLETED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        map.put(Const.Params.TIME, String.valueOf(time));
        map.put(Const.Params.DISTANCE, String.valueOf(distance));
        Log.d("mahi", "map 4" + map.toString());
        new VollyRequester(activity, Const.POST, map,
                Const.ServiceCode.PROVIDER_SERVICE_COMPLETED, this);

    }

    public void checkRequestStatus(int req_id) {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.CHECK_REQUEST_STATUS_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(req_id));
        Log.d("mahi", "check req status no calling" + map.toString());
        new VollyRequester(activity, Const.POST, map, Const.ServiceCode.CHECK_REQUEST_STATUS, this);

    }

    private void findDistanceAndTime(LatLng s_latlan, LatLng d_latlan) {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }

        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.GOOGLE_MATRIX_URL + Const.Params.ORIGINS + "="
                + String.valueOf(s_latlan.latitude) + "," + String.valueOf(s_latlan.longitude) + "&" + Const.Params.DESTINATION + "="
                + String.valueOf(d_latlan.latitude) + "," + String.valueOf(d_latlan.longitude) + "&" + Const.Params.MODE + "="
                + "driving" + "&" + Const.Params.LANGUAGE + "="
                + "en-EN" + "&" + "key=" + Const.GOOGLE_API_KEY + "&" + Const.Params.SENSOR + "="
                + String.valueOf(false));
        AndyUtils.appLog("Ashutosh", "distance api" + map);
        new VollyRequester(activity, Const.GET, map, Const.ServiceCode.GOOGLE_MATRIX, this);
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {

            case Const.ServiceCode.GOOGLE_DIRECTION_API:

                if (response != null) {
                    drawPath(response);

                }
                break;
            case Const.ServiceCode.USER_MESSAGE_NOTIFY:

                if (response != null) {

                }
                break;

            case Const.ServiceCode.PROVIDER_STARTED:

                try {

                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {
                        Commonutils.progressdialog_hide();
                        jobStatus = Const.IS_PROVIDER_STARTED;
                        setJobStatus(jobStatus);
                    } else {
                        Commonutils.progressdialog_hide();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Const.ServiceCode.PROVIDER_ARRIVED:

                try {

                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {
                        Commonutils.progressdialog_hide();

                        jobStatus = Const.IS_PROVIDER_ARRIVED;
                        setJobStatus(jobStatus);
                    } else {
                        Commonutils.progressdialog_hide();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case Const.ServiceCode.PROVIDER_SERVICE_STARTED:

                try {

                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {
                        Commonutils.progressdialog_hide();
                        jobStatus = Const.IS_PROVIDER_SERVICE_STARTED;
                        setJobStatus(jobStatus);

                    } else {
                        Commonutils.progressdialog_hide();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case Const.ServiceCode.PROVIDER_SERVICE_COMPLETED:
                Log.d("mahi", "walk completed" + response);
                try {

                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {
                        Commonutils.progressdialog_hide();
                        jobStatus = Const.IS_USER_RATED;
                        stopCheckingUpcomingRequests();
                        FeedBackFragment feedbackFrament = new FeedBackFragment();
                        Bundle bundle = new Bundle();
                        RequestDetails requestDetail2 = pContent
                                .parseRequestArrayInvoice(response);
                        bundle.putSerializable(Const.REQUEST_DETAIL,
                                requestDetail2);
                        bundle.putString("SCHEDULE", "0");
                        feedbackFrament.setArguments(bundle);
                        activity.addFragment(feedbackFrament, false,
                                Const.FEEDBACK_FRAGMENT, true);

                    } else {
                        Commonutils.progressdialog_hide();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case Const.ServiceCode.GOOGLE_MATRIX:
                Log.d("mahi", "google distance api" + response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("status").equals("OK")) {
                        JSONArray sourceArray = jsonObject.getJSONArray("origin_addresses");
                        String sourceObject = (String) sourceArray.get(0);

                        JSONArray destinationArray = jsonObject.getJSONArray("destination_addresses");
                        String destinationObject = (String) destinationArray.get(0);

                        JSONArray jsonArray = jsonObject.getJSONArray("rows");
                        JSONObject elementsObject = jsonArray.getJSONObject(0);
                        JSONArray elementsArray = elementsObject.getJSONArray("elements");
                        JSONObject distanceObject = elementsArray.getJSONObject(0);
                        JSONObject dObject = distanceObject.getJSONObject("distance");
                        String distance = dObject.getString("value");
                        JSONObject durationObject = distanceObject.getJSONObject("duration");
                        String duration = durationObject.getString("value");
                        double trip_dis = Integer.valueOf(distance) * 0.001;

                        providerServiceCompleted(String.valueOf(trip_dis), String.valueOf(trip_duration));


                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Const.ServiceCode.POST_CANCEL_TRIP:
                AndyUtils.appLog("Ashutosh", "CancelResponse" + response);
                pHelper.clearRequestData();
                stopCheckingUpcomingRequests();
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
                activity.finish();
                break;
            case Const.ServiceCode.CHECK_REQUEST_STATUS:
                AndyUtils.appLog("Ashutosh", "TravelCheckStatusResponse" + response);
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    JSONArray dataObjectArray = jsonObject.optJSONArray("data");

                    if (dataObjectArray != null && dataObjectArray.length() == 0) {
                        if (!isShown) {
                            if (isAdded() && activity.currentFragment.equals(Const.TRAVEL_MAP_FRAGMENT)) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setMessage(getResources().getString(R.string.txt_cancel_msg_user))
                                        .setCancelable(false)
                                        .setPositiveButton(getResources().getString(R.string.txt_ok), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                pHelper.clearRequestData();
                                                dialog.dismiss();
                                                stopCheckingUpcomingRequests();
                                                Intent intent = new Intent(activity, MainActivity.class);
                                                activity.startActivity(intent);
                                                activity.finish();
                                            }
                                        });

                                AlertDialog alert = builder.create();
                                alert.show();

                            }
                            isShown = true;
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();


        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
    }

    private void attemptSend(LatLng latlong, float bear) {

        if (!mSocket.connected()) return;

        JSONObject messageObj = new JSONObject();
        try {
            messageObj.put("latitude", String.valueOf(latlong.latitude));
            messageObj.put("longitude", String.valueOf(latlong.longitude));
            messageObj.put("sender", pHelper.getUserId());
            messageObj.put("receiver", requestDetails.getClientId());
            messageObj.put("status", "1");
            messageObj.put("request_id", pHelper.getRequestId());
            messageObj.put("bearing", String.valueOf(bear));

            Log.e("mahi", "calling socket" + messageObj.toString());

            mSocket.emit("send location", messageObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SupportMapFragment f = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.driver_travel_map);
        if (f != null) {
            try {
                getFragmentManager().beginTransaction().remove(f).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
     /*TO clear all views */
        ViewGroup mContainer = (ViewGroup) getActivity().findViewById(R.id.content_frame);
        mContainer.removeAllViews();

    }


    @Override
    public void onMapReady(GoogleMap mgoogleMap) {
        googleMap = mgoogleMap;
        // map.
        Commonutils.progressdialog_hide();
        if (googleMap != null) {

            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setMapToolbarEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);
            /*MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(
                    activity, R.raw.maps_style);
            googleMap.setMapStyle(style);*/
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }


            setSourceDestinationMarkerOnMap();
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (null != mHashMap.get(marker)) {
            switch (mHashMap.get(marker)) {
                case 0:
                    Intent sourcenNvi = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=" + requestDetails.getsLatitude() + "," + requestDetails.getsLongitude()));
                    startActivity(sourcenNvi);
                    break;
                case 1:
                    Intent destNavi = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=" + requestDetails.getdLatitude() + "," + requestDetails.getdLongitude()));
                    startActivity(destNavi);
                    break;
            }
        }
        return false;
    }


    private void requestPermission(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        startActivityForResult(intent, requestCode);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE_CHATHEAD) {
            if (!Utils.canDrawOverlays(activity)) {
                needPermissionDialog(requestCode);
            } else {
                activity.startService(new Intent(activity, ChatHeadService.class));
            }

        }

    }


    private void needPermissionDialog(final int requestCode) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setMessage(getResources().getString(R.string.txt_allow_permission));
        builder.setPositiveButton(getResources().getString(R.string.txt_ok),
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        requestPermission(requestCode);
                    }
                });
        builder.setNegativeButton(getResources().getString(R.string.txt_cancel), new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        builder.setCancelable(false);
        builder.show();
    }

}

