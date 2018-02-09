package com.swyftpartner.driver.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aurelhubert.simpleratingbar.SimpleRatingBar;
import com.bumptech.glide.Glide;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.activity.DatabaseHandler;
import com.swyftpartner.driver.activity.MainActivity;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.model.RequestDetails;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by user on 1/12/2017.
 */

public class FeedBackFragment extends Fragment implements View.OnClickListener, AsyncTaskCompleteListener {

    private TextView totalFare, tv_no_tolls;
    private ImageView vehicleIcon, userIcon, staticMapIcon;
    private TextView submitButton;
    private SimpleRatingBar simpleRatingBar;
    private Bundle bundle;
    private RequestDetails requestDetails;
    private int ratingStar = 0;
    private MainActivity activity;
    private PreferenceHelper pHelper;
    private ParseContent pContent;
    private TextView distance, duration, tv_payment_type, tv_cancellation_fee;
    private Handler reqHandler;
    private boolean ishown = false;
    private String payment_mode, status = "";
    private DatabaseHandler db;
    private LinearLayout layout_distance, toll_layout;

    Runnable runnable = new Runnable() {
        public void run() {
            if (requestDetails != null) {
                checkRequestStatus(requestDetails.getRequestId());
                reqHandler.postDelayed(this, 2000);
            }
        }
    };

    private void checkRequestStatus(int requestId) {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.CHECK_REQUEST_STATUS_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestId));
        Log.d("mahi", "check req status is calling" + map.toString());
        new VollyRequester(activity, Const.POST, map, Const.ServiceCode.CHECK_REQUEST_STATUS, this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();
        db = new DatabaseHandler(activity);
        reqHandler = new Handler();
        pHelper = new PreferenceHelper(activity);
        pContent = new ParseContent(activity);

        Commonutils.progressdialog_hide();

        bundle = getArguments();
        if (bundle != null) {
            requestDetails = (RequestDetails) bundle.getSerializable(Const.REQUEST_DETAIL);
            startCheckingstatusRequests();
        }


        if (db != null) {
            db.DeleteChat(String.valueOf(pHelper.getRequestId()));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback_layout, container, false);
        totalFare = (TextView) view.findViewById(R.id.tv_feedback_total_fare);
        vehicleIcon = (ImageView) view.findViewById(R.id.iv_feedback_vehicle);
        userIcon = (ImageView) view.findViewById(R.id.iv_feedback_user);
        staticMapIcon = (ImageView) view.findViewById(R.id.iv_feedback_location);
        submitButton = (TextView) view.findViewById(R.id.bn_feedback_submit);
        simpleRatingBar = (SimpleRatingBar) view.findViewById(R.id.feedback_rating_bar);
        distance = (TextView) view.findViewById(R.id.tv_feedback_distance);
        duration = (TextView) view.findViewById(R.id.tv_feedback_time);
        tv_payment_type = (TextView) view.findViewById(R.id.tv_payment_type);
        tv_no_tolls = (TextView) view.findViewById(R.id.tv_no_tolls);
        //tv_cancellation_fee = (TextView) view.findViewById(R.id.tv_cancellation_fee);
        layout_distance = (LinearLayout) view.findViewById(R.id.layout_distance);
        toll_layout = (LinearLayout) view.findViewById(R.id.toll_layout);
        submitButton.setOnClickListener(this);
        if (requestDetails != null) {
            setDataOnFeedBackView();
        }

        simpleRatingBar.setListener(new SimpleRatingBar.SimpleRatingBarListener() {
            @Override
            public void onValueChanged(int value) {
                ratingStar = value;
                AndyUtils.appLog("RatingValue", value + "");
            }
        });

        if (requestDetails.getRequest_type().equals("1") || requestDetails.getRequest_type().equals("2")) {
            toll_layout.setVisibility(View.GONE);
        } else {
            toll_layout.setVisibility(View.VISIBLE);
            tv_no_tolls.setText(getResources().getString(R.string.txt_total_no_tolls) + " " + requestDetails.getNo_tolls());
        }
        if (requestDetails.getRequest_type().equals("2") || requestDetails.getRequest_type().equals("3")) {
            layout_distance.setVisibility(View.GONE);
            staticMapIcon.setVisibility(View.GONE);
        } else {
            layout_distance.setVisibility(View.VISIBLE);
            staticMapIcon.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public static String getGoogleMapThumbnail(double lati, double longi) {
        String staticMapUrl = "http://maps.google.com/maps/api/staticmap?center=" + lati + "," + longi + "&markers=" + lati + "," + longi + "&zoom=14&size=150x120&sensor=false";
        return staticMapUrl;
    }


    private void setDataOnFeedBackView() {
        Glide.with(activity).load(requestDetails.getClientProfile()).error(R.drawable.passenger).into(userIcon);
        Glide.with(activity).load(requestDetails.getTypePicture()).into(vehicleIcon);
        totalFare.setText(requestDetails.getCurrency_unit() + " " + requestDetails.getTotal());
        distance.setText(requestDetails.getDistance() + " " + requestDetails.getDistance_unit());
        duration.setText(requestDetails.getTime() + " " + "mins");
        tv_payment_type.setText(getResources().getString(R.string.txt_payment_type) + requestDetails.getPayment_type());
        if (!requestDetails.getdLatitude().equals("") && !requestDetails.getdLongitude().equals("")) {
            Glide.with(this).load(getGoogleMapThumbnail(Double.valueOf(requestDetails.getdLatitude()), Double.valueOf(requestDetails.getdLongitude()))).centerCrop().into(staticMapIcon);
        }

       /* if (null != requestDetails.getCancellationFee() && !TextUtils.isEmpty(requestDetails.getCancellationFee())) {
            tv_cancellation_fee.setVisibility(View.VISIBLE);
            tv_cancellation_fee.setText("Cancellation fee:"+" "+requestDetails.getCurrency_unit() + " " + requestDetails.getCancellationFee());
        } else {
            tv_cancellation_fee.setVisibility(View.GONE);
        }*/
    }

    private void startCheckingstatusRequests() {
        startCheckRegTimer();
    }

    private void stopCheckingstatusRequests() {
        if (reqHandler != null) {
            reqHandler.removeCallbacks(runnable);
            Log.d("mahi", "stop handler");
        }
    }

    public void startCheckRegTimer() {
        reqHandler.postDelayed(runnable, 1000);
    }


    @Override
    public void onResume() {
        super.onResume();

        activity.currentFragment = Const.FEEDBACK_FRAGMENT;


    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bn_feedback_submit:
                if (status.equals("3") && payment_mode.equals(Const.CASH)) {
                    Commonutils.showtoast(getResources().getString(R.string.txt_error_payment_confirm), activity);

                    return;
                }

                if (ratingStar > 0) {

                    postUserRated();
                } else {
                    AndyUtils.showShortToast(getString(R.string.give_rating), getActivity());
                }

                break;
        }

    }


    private void showCashPaymentDialog() {

        String Mesaage =  getResources().getString(R.string.txt_ride_total) + requestDetails.getCurrency_unit() + " " + requestDetails.getTotal();
        AlertDialog.Builder paybuilder = new AlertDialog.Builder(activity);
        paybuilder.setMessage(Mesaage)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.btn_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        postCodConfirmation();
                    }
                });
        AlertDialog alert = paybuilder.create();
        alert.show();
    }


    private void postCodConfirmation() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.COD_CONFIRM_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));

        AndyUtils.appLog("Ashutosh", "CodConfirmMap" + map);
        new VollyRequester(activity, Const.POST, map, Const.ServiceCode.COD_CONFIRM, this);
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {
            case Const.ServiceCode.COD_CONFIRM:

                if (pContent.isSuccess(response)) {
                    AndyUtils.showShortToast(getResources().getString(R.string.txt_confirm_cash), activity);

                }
                break;
            case Const.ServiceCode.RATE_USER:
                if (pContent.isSuccess(response)) {
                    AndyUtils.showShortToast(getString(R.string.rated_successfully), activity);
                    stopCheckingstatusRequests();
                    pHelper.clearRequestData();
                    Intent homeIntent = new Intent(activity, MainActivity.class);
                    startActivity(homeIntent);
                    activity.finish();
                }
                break;

            case Const.ServiceCode.CHECK_REQUEST_STATUS:
                AndyUtils.appLog("AShutosh", "check req" + response);
                if (pContent.isSuccess(response)) {
                    if (response != null) {


                        try {
                            JSONObject job = new JSONObject(response);
                            JSONArray jarray = job.getJSONArray("data");
                            JSONArray invoicearray = job.getJSONArray("invoice");
                            JSONObject stausobj = jarray.getJSONObject(0);
                            status = stausobj.getString("status");
                            JSONObject invobj = invoicearray.getJSONObject(0);
                            payment_mode = invobj.getString("payment_mode");
                            //Log.d("mahi","payment_mode"+payment_mode);
                            if (ishown == false && status.equals("8") && payment_mode.equals(Const.CASH) && activity != null && !activity.isFinishing() && activity.currentFragment.equals(Const.FEEDBACK_FRAGMENT)) {
                                ishown = true;
                                showCashPaymentDialog();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                break;

        }

    }


    private void postUserRated() {
        if (!AndyUtils.isNetworkAvailable(activity)) {

            return;
        }
        Commonutils.progressdialog_show(activity, activity.getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.RATE_USER_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, String.valueOf(requestDetails.getRequestId()));
        map.put(Const.Params.RATING, String.valueOf(ratingStar));

        AndyUtils.appLog("Ashutosh", "CodConfirmMap" + map);
        new VollyRequester(activity, Const.POST, map, Const.ServiceCode.RATE_USER, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCheckingstatusRequests();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
          /*TO clear all views */
        ViewGroup mContainer = (ViewGroup) getActivity().findViewById(R.id.content_frame);
        mContainer.removeAllViews();
    }
}
