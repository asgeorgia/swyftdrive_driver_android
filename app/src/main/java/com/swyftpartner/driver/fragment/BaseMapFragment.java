package com.swyftpartner.driver.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.activity.MainActivity;

/**
 * Created by user on 1/5/2017.
 */

public class BaseMapFragment extends Fragment implements
        View.OnClickListener, AsyncTaskCompleteListener {
    MainActivity activity;
    public static LatLng pic_latlan;
    public static LatLng drop_latlan;
    public static boolean searching =false;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();


    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {

    }

    @Override
    public void onClick(View v) {


    }
}
