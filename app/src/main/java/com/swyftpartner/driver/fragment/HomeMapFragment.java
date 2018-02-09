package com.swyftpartner.driver.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.adapter.AdsAdapter;
import com.swyftpartner.driver.adapter.TaxiAdapter;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.location.LocationHelper;
import com.swyftpartner.driver.model.AdsList;
import com.swyftpartner.driver.model.TaxiTypes;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ItemClickSupport;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mahesh on 1/5/2017.
 */

public class HomeMapFragment extends BaseMapFragment implements LocationHelper.OnLocationReceived, AsyncTaskCompleteListener, GoogleMap.OnCameraMoveListener, OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private static final int DURATION = 2000;
    public static ImageButton btn_mylocation;
    public static RelativeLayout request_layout;
    private GoogleMap googleMap;
    private Bundle mBundle;
    private SupportMapFragment req_home_map;
    private View view;
    private LocationHelper locHelper;
    private Location myLocation;
    private LatLng latlong;
    private ArrayList<TaxiTypes> typesList;
    private TaxiAdapter taxiAdapter;
    private PreferenceHelper pHelper;
    private ImageView bottomSheetArrowImage;
    private RecyclerView adsRecyclerView;
    private AdsAdapter adsAdapter;
    private List<AdsList> adsLists;
    private String TAG = HomeMapFragment.class.getSimpleName();
    private BottomSheetBehavior behavior;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.home_map_fragment, container,
                false);

        btn_mylocation = (ImageButton) view.findViewById(R.id.btn_mylocation);
        btn_mylocation.setOnClickListener(this);
        req_home_map = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.req_home_map);

        if (null != req_home_map) {
            req_home_map.getMapAsync(this);
        }


        bottomSheetArrowImage = (ImageView) view.findViewById(R.id.imageViewArrow);
        final View bottomSheet = view.findViewById(R.id.design_bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.e("BottomSheetCallback", "BottomSheetBehavior.STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.e("BottomSheetCallback", "BottomSheetBehavior.STATE_SETTLING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                       /* if (!bottomsheet_actionbar.isShown()) {
                         //   bottomsheet_actionbar.setVisibility(View.VISIBLE);
                        }*/
                        Log.e("BottomSheetCallback", "BottomSheetBehavior.STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                       /* if (bottomsheet_actionbar.isShown()) {
                       //     bottomsheet_actionbar.setVisibility(View.GONE);
                        }*/
                        Log.e("BottomSheetCallback", "BottomSheetBehavior.STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.e("BottomSheetCallback", "BottomSheetBehavior.STATE_HIDDEN");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.e("asher", "slideOffset value " + slideOffset);
                rotateArrow(slideOffset);
            }
        });
        behavior.setHideable(false);
        behavior.setSkipCollapsed(false);
        adsRecyclerView = (RecyclerView) view.findViewById(R.id.recycAds);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        adsRecyclerView.setLayoutManager(mLayoutManager);
        adsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        ItemClickSupport.addTo(adsRecyclerView)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Log.e("asher", "item click " + adsLists.get(position).getAdUrl());

                        String url = adsLists.get(position).getAdUrl();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);


                    }
                });
        getAds();

        bottomSheetArrowImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });


        return view;
    }

    private void getAds() {
        if (!AndyUtils.isNetworkAvailable(activity)) {
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.ADVERTISEMENTS);
        map.put(Const.Params.ID, new PreferenceHelper(activity).getUserId());
        map.put(Const.Params.TOKEN, new PreferenceHelper(activity).getSessionToken());

        AndyUtils.appLog(TAG, "adsList " + map);

        new VollyRequester(activity, Const.POST, map, Const.ServiceCode.ADVERTISEMENTS, this);
    }

    private void rotateArrow(float v) {
        bottomSheetArrowImage.setRotation(-180 * v);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = savedInstanceState;
        typesList = new ArrayList<TaxiTypes>();


    }


    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        super.onTaskCompleted(response, serviceCode);
        switch (serviceCode) {
            case Const.ServiceCode.ADVERTISEMENTS:
                AndyUtils.appLog(TAG, "addsListResponse " + response);
                try {
                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {
                        JSONArray jsonArray = job1.optJSONArray("data");
                        if (null != adsLists) {
                            adsLists.clear();
                        }
                        if (null != jsonArray && jsonArray.length() > 0) {
                            adsLists = new ParseContent(activity).parseAdsList(jsonArray);
                            if (adsLists != null) {
                                adsAdapter = new AdsAdapter(adsLists, activity);
                                //Adding adapter to Listview
                                adsRecyclerView.setAdapter(adsAdapter);
                            }
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            MapsInitializer.initialize(getActivity());
        } catch (Exception e) {
        }

        locHelper = new LocationHelper(activity);
        locHelper.setLocationReceivedLister(this);
    }

    @Override
    public void onLocationReceived(LatLng latlong) {

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onLocationReceived(Location location) {
        if (location != null) {
            // drawTrip(latlong);
            myLocation = location;
            LatLng latLang = new LatLng(location.getLatitude(),
                    location.getLongitude());
            latlong = latLang;
//            AndyUtils.appLog("OnLocationReceived", location.toString());


        }

    }

    @Override
    public void onConntected(Bundle bundle) {

    }

    @Override
    public void onConntected(Location location) {

        if (location != null && googleMap != null) {
            LatLng currentlatLang = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentlatLang,
                    16));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mylocation:
                if (null != latlong) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong,
                            16));
                }
                break;


        }

    }


    @Override
    public void onResume() {
        super.onResume();
        activity.currentFragment = Const.HOME_MAP_FRAGMENT;

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SupportMapFragment f = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.req_home_map);
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

        googleMap = null;
    }

    @Override
    public void onMapReady(GoogleMap mgoogleMap) {
        googleMap = mgoogleMap;
        if (googleMap != null) {
            Commonutils.progressdialog_hide();
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setMapToolbarEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);
           /* MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(
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

            googleMap.setMyLocationEnabled(true);
            googleMap.setOnCameraMoveListener(this);
            googleMap.setOnCameraIdleListener(this);

        }
    }

    @Override
    public void onCameraMove() {

    }


    @Override
    public void onCameraIdle() {

    }
}
