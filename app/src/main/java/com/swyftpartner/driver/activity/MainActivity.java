package com.swyftpartner.driver.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.fragment.FeedBackFragment;
import com.swyftpartner.driver.fragment.HomeMapFragment;
import com.swyftpartner.driver.fragment.TravelMapFragment;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.location.LocationHelper;
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
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements AsyncTaskCompleteListener, LocationHelper.OnLocationReceived {

    private final long startTime = 60 * 1000;
    private final long interval = 1 * 1000;
    public String currentFragment = "";
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar mainToolbar;
    private Bundle mbundle;
    private PreferenceHelper pHelper;
    private Handler reqhandler;
    private AlertDialog gpsAlertDialog, internetDialog;
    private boolean isGpsDialogShowing = false, isRecieverRegistered = false, isNetDialogShowing = false;
    private boolean gpswindowshowing = false;
    AlertDialog.Builder gpsBuilder;
    private LocationManager manager;
    private MyCountDownTimer countDownTimer;
    MediaPlayer mediaPlayer;
    Runnable runnable = new Runnable() {
        public void run() {
            getIncomingRequestsInProgress();
            reqhandler.postDelayed(this, 4000);
        }
    };
    private ParseContent pContent;
    private boolean isShown = false;
    private TextView tv_timer;
    private Dialog requestDialog;
    ImageButton bnt_menu;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("FragmentRequest", "onReceive");
            String notification = intent.getStringExtra("ashutosh");
            Log.d("My Push notification", notification);
            String message = intent.getStringExtra(Const.PROVIDER_INTENT_MESSAGE);
            try {
                JSONObject messageObj = new JSONObject(message);
                JSONObject jsonObject = messageObj.getJSONObject("data");
                AndyUtils.appLog("JsonResponse", messageObj.toString());
                if (Integer.parseInt(jsonObject.getString("status")) == 6) {
                    if (requestDialog != null && requestDialog.isShowing()) {
                        requestDialog.cancel();
                        mediaPlayer.stop();
                        pHelper.clearRequestData();
                        isShown = false;
                        startCheckingUpcomingRequests();

                        countDownTimer.cancel();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        if (!TextUtils.isEmpty(new PreferenceHelper(this).getLanguage())) {
            Locale myLocale = null;
            switch (new PreferenceHelper(this).getLanguage()) {
                case "":
                    myLocale = new Locale("en");
                    break;
                case "en":
                    myLocale = new Locale("en");

                    break;
                case "fr":
                    myLocale = new Locale("fr");
                    break;

            }


            Locale.setDefault(myLocale);
            Configuration config = new Configuration();
            config.locale = myLocale;
            this.getResources().updateConfiguration(config,
                    this.getResources().getDisplayMetrics());
        }

        setContentView(R.layout.activity_main);
        bnt_menu = (ImageButton) findViewById(R.id.bnt_menu);
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle(null);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        initDrawer();
        pHelper = new PreferenceHelper(this);
        pContent = new ParseContent(this);
        upDateTimeZone();
        reqhandler = new Handler();
        mbundle = savedInstanceState;
        bnt_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        if (AndyUtils.isNetworkAvailable(this)) {
            checkStatus();
            //Log.d("Ashutosh", "onresume main activity");
        }

    }

    private void ShowGpsDialog() {

        isGpsDialogShowing = true;

        gpsBuilder = new AlertDialog.Builder(
                this);


        gpsBuilder.setCancelable(false);
        gpsBuilder
                .setTitle(getResources().getString(R.string.txt_gps_off))
                .setMessage(getResources().getString(R.string.txt_gps_msg))
                .setPositiveButton(getResources().getString(R.string.txt_enable),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // continue with delete
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                                removeGpsDialog();
                            }
                        })

                .setNegativeButton(getResources().getString(R.string.txt_exit),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // do nothing
                                removeGpsDialog();
                                finishAffinity();
                            }
                        });
        gpsAlertDialog = gpsBuilder.create();

        gpsAlertDialog.show();

    }


    private void removeGpsDialog() {
        if (gpsAlertDialog != null && gpsAlertDialog.isShowing()) {
            gpsAlertDialog.dismiss();
            isGpsDialogShowing = false;
            gpsAlertDialog = null;


        }
    }


    private void initDrawer() {
        // TODO Auto-generated method stub
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                mainToolbar, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                invalidateOptionsMenu();
            }
        };
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {

                drawerToggle.syncState();
            }
        });


    }

    public void startCheckRegTimer() {
        reqhandler.postDelayed(runnable, 4000);
    }

    public void getIncomingRequestsInProgress() {
        if (!AndyUtils.isNetworkAvailable(this)) {

            return;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.INCOMING_REQUEST_IN_PROGRESS_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        new VollyRequester(this, Const.POST, map,
                Const.ServiceCode.INCOMING_REQUEST, this);

    }

    private void startCheckingUpcomingRequests() {
        startCheckRegTimer();
    }

    private void stopCheckingUpcomingRequests() {
        if (reqhandler != null) {
            reqhandler.removeCallbacks(runnable);

            Log.d("mahi", "stop handler");
        }
    }

    public void closeDrawer() {
        drawerLayout.closeDrawers();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        //AndyUtils.appLog("CurrentFragment", currentFragment);
        openExitDialog();
    }

    public void addFragment(Fragment fragment, boolean addToBackStack,
                            String tag, boolean isAnimate) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        if (isAnimate) {
            ft.setCustomAnimations(R.anim.slide_in_right,
                    R.anim.slide_out_left, R.anim.slide_in_left,
                    R.anim.slide_out_right);

        }

        if (addToBackStack) {
            ft.addToBackStack(tag);
        }
        ft.replace(R.id.content_frame, fragment, tag);
        ft.commitAllowingStateLoss();
    }

    private void upDateTimeZone() {
        if (!AndyUtils.isNetworkAvailable(this)) {

            return;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.UPDATETIME);
        map.put(Const.Params.ID, new PreferenceHelper(this).getUserId());
        map.put(Const.Params.TOKEN, new PreferenceHelper(this).getSessionToken());
        map.put(Const.Params.TIMEZONE, TimeZone.getDefault().getID());
        new VollyRequester(this, Const.POST, map,
                Const.ServiceCode.UPDATETIME, this);
    }

    @Override
    public void onResume() {

        super.onResume();

        IntentFilter filter = new IntentFilter(Const.PROVIDER_REQUEST_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                filter);
        AndyUtils.appLog("RequestId", pHelper.getRequestId() + "");
        if (pHelper.getRequestId() == Const.NO_REQUEST) {
            startCheckingUpcomingRequests();
        }
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // do something else
            if (isGpsDialogShowing) {
                return;
            }
            ShowGpsDialog();
        } else {


            removeGpsDialog();
        }

        registerReceiver(internetConnectionReciever, new IntentFilter(
                "android.net.conn.CONNECTIVITY_CHANGE"));

        registerReceiver(GpsChangeReceiver, new IntentFilter(
                LocationManager.PROVIDERS_CHANGED_ACTION));
        isRecieverRegistered = true;


    }

    public BroadcastReceiver internetConnectionReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo activeWIFIInfo = connectivityManager
                    .getNetworkInfo(connectivityManager.TYPE_WIFI);

            if (activeWIFIInfo.isConnected() || activeNetInfo.isConnected()) {
                removeInternetDialog();
            } else {
                if (isNetDialogShowing) {
                    return;
                }
                showInternetDialog();
            }
        }
    };

    private void removeInternetDialog() {
        if (internetDialog != null && internetDialog.isShowing()) {
            internetDialog.dismiss();
            isNetDialogShowing = false;
            internetDialog = null;

        }
    }

    private void showInternetDialog() {

        isNetDialogShowing = true;
        AlertDialog.Builder internetBuilder = new AlertDialog.Builder(
                MainActivity.this);
        internetBuilder.setCancelable(false);
        internetBuilder
                .setTitle(getString(R.string.dialog_no_internet))
                .setMessage(getString(R.string.dialog_no_inter_message))
                .setPositiveButton(getString(R.string.dialog_enable_3g),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // continue with delete
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_SETTINGS);
                                startActivity(intent);
                                removeInternetDialog();
                            }
                        })
                .setNeutralButton(getString(R.string.dialog_enable_wifi),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // User pressed Cancel button. Write
                                // Logic Here
                                startActivity(new Intent(
                                        Settings.ACTION_WIFI_SETTINGS));
                                removeInternetDialog();
                            }
                        })
                .setNegativeButton(getString(R.string.dialog_exit),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // do nothing
                                removeInternetDialog();
                                finish();
                            }
                        });
        internetDialog = internetBuilder.create();
        internetDialog.show();
    }


    public BroadcastReceiver GpsChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            if (intent.getAction() != null) {

                final LocationManager manager = (LocationManager) context
                        .getSystemService(Context.LOCATION_SERVICE);
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // do something

                    removeGpsDialog();
                } else {
                    // do something else
                    if (isGpsDialogShowing) {
                        return;
                    }

                    ShowGpsDialog();
                }

            }
        }
    };


    public void checkStatus() {
        checkRequestStatus(new PreferenceHelper(this).getRequestId());
    }

    public void checkRequestStatus(int req_id) {
        if (!AndyUtils.isNetworkAvailable(this)) {

            return;
        }
        Commonutils.progressdialog_show(MainActivity.this, "");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.CHECK_REQUEST_STATUS_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        //  map.put(Const.Params.REQUEST_ID, String.valueOf(req_id));
        Log.d("mahi", "check req status no calling" + map.toString());
        new VollyRequester(this, Const.POST, map,
                Const.ServiceCode.CHECK_REQUEST_STATUS, this);

    }


    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {
            case Const.ServiceCode.UPDATE_LOCATION:
                AndyUtils.appLog("Ashutosh", "UpdateLocationResponse" + response);
                break;
            case Const.ServiceCode.UPDATETIME:
                Log.d("mahi", "update time" + response);
                break;
            case Const.ServiceCode.INCOMING_REQUEST:
                AndyUtils.appLog("Ashutosh", "InComingRequestResponse" + response);

                if (!pContent.isSuccess(response)) {
                    if (pContent.getErrorCode(response) == Const.REQUEST_ID_NOT_FOUND) {

                        pHelper.clearRequestData();

                    } else if (pContent.getErrorCode(response) == Const.INVALID_TOKEN) {

//                        if (mDialog != null && mDialog.isShowing()) {
//                            if (!isFinishing()) {
//                                mDialog.dismiss();
//                                isApprovedCheck = true;
//                            }
//                        }

                        pHelper.clearRequestData();
                        stopCheckingUpcomingRequests();
                        new PreferenceHelper(this).Logout();
                        Intent i = new Intent(this, LoginActivity.class);
                        this.startActivity(i);
                        this.finish();
                        AndyUtils.showShortToast(getResources().getString(R.string.login_errror), this);

                    }
                    return;
                }
                int requestId = pContent.parseRequestInProgress(response);

                if (requestId == Const.NO_REQUEST) {

                } else {
                    String number_hours = "";
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jarray = jsonObject.getJSONArray("data");
                        JSONObject Jobj = jarray.getJSONObject(0);

                        String picture = Jobj.optString("user_picture");
                        String name = Jobj.optString("user_name");
                        String request_id = Jobj.optString("request_id");
                        String address = Jobj.optString("s_address");
                        String s_lat = Jobj.optString("s_latitude");
                        String s_lan = Jobj.optString("s_longitude");
                        String request_status_type = Jobj.optString("request_status_type");
                        if (request_status_type.equals("2")) {
                            JSONObject hourObj = jsonObject.getJSONObject("hourly_package_details");
                            number_hours = hourObj.getString("number_hours");
                        }

                        String staticMapUrl = "http://maps.google.com/maps/api/staticmap?center=" + s_lat + "," + s_lan + "&markers=" + s_lat + "," + s_lan + "&zoom=14&size=270x270&sensor=false";

                        long countDown = Long.parseLong(Jobj.optString("time_left_to_respond"));
                        String request_type = Jobj.getString("request_type");
                        Log.d("mahi", "stau" + isShown);
                        if (countDown > 0) {
                            if (!isShown) {
                                if (!isFinishing()) {

                                    showRequestDialog(picture, name, String.valueOf(request_id), address, countDown, staticMapUrl, request_status_type, number_hours);
                                    stopCheckingUpcomingRequests();
                                }


                                isShown = true;
                            }
                        }
//                            if (!status.equals("0") && checkreq_status == false) {
//                                checkreq_status = true;
//                                Log.d("mahi", "check status in log");
//                                checkRequestStatus(Integer.valueOf(request_id));
//                                stopCheckingUpcomingRequests();
//                            }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }


                break;
            case Const.ServiceCode.CHECK_REQUEST_STATUS:
                AndyUtils.appLog("mahi", "CheckRequestStatusResponse :" + response);
                if (!pContent.isSuccess(response)) {
                    if (pContent.getErrorCode(response) == Const.INVALID_TOKEN) {
                        Log.e("mahi", "coming here Response ");
                        pHelper.clearRequestData();
                        new PreferenceHelper(this).Logout();
                        Intent i = new Intent(this, LoginActivity.class);
                        this.startActivity(i);
                        this.finish();
                        AndyUtils.showShortToast(getResources().getString(R.string.login_errror), this);

                    } else if (pContent.getErrorCode(response) == Const.REQUEST_ID_NOT_FOUND) {
                        pHelper.clearRequestData();
                    } else if (pContent.getErrorCode(response) == Const.INVALID_TOKEN) {

                    } else if (pContent.getErrorCode(response) == Const.INVALID_REQUEST_ID) {

                        pHelper.clearRequestData();
                        startCheckingUpcomingRequests();

                    }

                    return;
                }


                Bundle bundle = new Bundle();
                TravelMapFragment travelMapFragment = new TravelMapFragment();
                RequestDetails requestDetail = null;

                requestDetail = pContent.parseRequestArrayStatus(response);
                if (requestDetail != null) {
                    switch (Integer.parseInt(requestDetail.getProviderStatus())) {

                        case Const.NO_REQUEST:
                            pHelper.clearRequestData();

                            break;

                        case Const.IS_PROVIDER_ACCEPTED:

                            if (requestDetail != null) {
                                bundle.putSerializable(Const.REQUEST_DETAIL,
                                        requestDetail);
                                bundle.putInt(Const.PROVIDER_STATUS,
                                        Const.IS_PROVIDER_ACCEPTED);
                                travelMapFragment.setArguments(bundle);
                                addFragment(travelMapFragment, false, Const.TRAVEL_MAP_FRAGMENT,
                                        true);
                            } else {
                                return;
                            }

                            break;
                        case Const.IS_PROVIDER_STARTED:
                            if (requestDetail != null) {
                                bundle.putSerializable(Const.REQUEST_DETAIL,
                                        requestDetail);
                                bundle.putInt(Const.PROVIDER_STATUS,
                                        Const.IS_PROVIDER_STARTED);
                                travelMapFragment.setArguments(bundle);

                                addFragment(travelMapFragment, false, Const.TRAVEL_MAP_FRAGMENT,
                                        true);
                            } else {
                                return;
                            }
                            break;
                        case Const.IS_PROVIDER_ARRIVED:
                            if (requestDetail != null) {
                                bundle.putSerializable(Const.REQUEST_DETAIL,
                                        requestDetail);
                                bundle.putInt(Const.PROVIDER_STATUS,
                                        Const.IS_PROVIDER_ARRIVED);
                                travelMapFragment.setArguments(bundle);
                                addFragment(travelMapFragment, false, Const.TRAVEL_MAP_FRAGMENT,
                                        true);
                            } else {
                                return;
                            }
                            break;
                        case Const.IS_PROVIDER_SERVICE_STARTED:
                            if (requestDetail != null) {
                                bundle.putSerializable(Const.REQUEST_DETAIL,
                                        requestDetail);
                                bundle.putInt(Const.PROVIDER_STATUS,
                                        Const.IS_PROVIDER_SERVICE_STARTED);
                                travelMapFragment.setArguments(bundle);
                                addFragment(travelMapFragment, false, Const.TRAVEL_MAP_FRAGMENT,
                                        true);
                            } else {
                                return;
                            }

                            break;
                        case Const.IS_PROVIDER_SERVICE_COMPLETED:
                            if (requestDetail != null) {
                                bundle.putSerializable(Const.REQUEST_DETAIL,
                                        requestDetail);
                                bundle.putInt(Const.PROVIDER_STATUS,
                                        Const.IS_PROVIDER_SERVICE_COMPLETED);

                                bundle.putString("SCHEDULE", "0");

                                FeedBackFragment feedbackFragment = new FeedBackFragment();
                                feedbackFragment.setArguments(bundle);
                                addFragment(feedbackFragment, false, Const.FEEDBACK_FRAGMENT,
                                        true);
                            } else {
                                return;
                            }
                            break;

                        case Const.IS_USER_RATED:

                            break;
                        default:
                            break;
                    }
                } else {

                    addFragment(new HomeMapFragment(), false, Const.HOME_MAP_FRAGMENT, true);
                }
                break;

            case Const.ServiceCode.DRIVER_ACCEPTED:
                AndyUtils.appLog("Ashutosh", "AcceptedResponse" + response);
                stopCheckingUpcomingRequests();

                try {

                    JSONObject job1 = new JSONObject(response);
                    if (job1.getString("success").equals("true")) {

                        Commonutils.progressdialog_hide();
                        isShown = false;
                        JSONObject datj = job1.getJSONObject("data");
                        String request_id = datj.getString("request_id");
                        pHelper.putRequestId(Integer.valueOf(request_id));
                        pHelper.putClient_id(datj.getString("user_id"));

                        RequestDetails reqDetail = pContent
                                .parseRequestStatus(response);
                        if (reqDetail != null) {
                            Bundle bundle2 = new Bundle();
                            TravelMapFragment tripFragment = new TravelMapFragment();
                            bundle2.putInt(Const.PROVIDER_STATUS,
                                    Const.IS_PROVIDER_ACCEPTED);
                            bundle2.putSerializable(Const.REQUEST_DETAIL,
                                    reqDetail);
                            tripFragment.setArguments(bundle2);
                            addFragment(tripFragment, false, Const.TRAVEL_MAP_FRAGMENT, true);
                        }


                    } else {
                        Commonutils.progressdialog_hide();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case Const.ServiceCode.DRIVER_REJECTED:
                Commonutils.progressdialog_hide();
                pHelper.clearRequestData();
                startCheckingUpcomingRequests();
                isShown = false;
                break;
            default:
                break;
        }


    }

    private void showRequestDialog(String picture, String name, final String request_id, String address, long countDown, String map_img, String req_type, String number_hours) {
        isShown = true;
        mediaPlayer = MediaPlayer.create(this, R.raw.beep);
        requestDialog = new Dialog(this, R.style.DialogSlideAnim_leftright_Fullscreen);
        requestDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestDialog.setCancelable(false);
        requestDialog.setContentView(R.layout.dialog_request_layout);
        CircleImageView iv_location = (CircleImageView) requestDialog.findViewById(R.id.iv_location);
        if (map_img != null) {
            Glide.with(this).load(map_img).into(iv_location);
        }
        TextView tv_name = (TextView) requestDialog.findViewById(R.id.tv_name);
        TextView tv_hours = (TextView) requestDialog.findViewById(R.id.tv_hours);
        CircularProgressBar circularProgressBar = (CircularProgressBar) requestDialog.findViewById(R.id.req_progress_bar);
        tv_timer = (TextView) requestDialog.findViewById(R.id.tv_timer);
        tv_name.setText(getResources().getString(R.string.txt_name) + " " + name);
        TextView tv_address = (TextView) requestDialog.findViewById(R.id.tv_address);
        ImageButton btn_accept = (ImageButton) requestDialog.findViewById(R.id.btn_accept);
        ImageButton btn_reject = (ImageButton) requestDialog.findViewById(R.id.btn_reject);
        tv_address.setText(getResources().getString(R.string.txt_pic_address) + " " + address);
        if (req_type.equals("2")) {
            tv_hours.setVisibility(View.VISIBLE);
            tv_hours.setText(getResources().getString(R.string.txt_no_hours) + " " + number_hours);
        } else {
            tv_hours.setVisibility(View.GONE);
        }
        int animationDuration = (int) (countDown * 1000); // 2500ms = 2,5s
        circularProgressBar.setProgressWithAnimation(100, animationDuration);
        countDownTimer = new MyCountDownTimer(countDown * 1000, interval);
        countDownTimer.start();
        btn_reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDialog.dismiss();
                mediaPlayer.stop();
                reject(request_id);
                countDownTimer.cancel();
                startCheckingUpcomingRequests();

            }
        });
        btn_accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDialog.dismiss();
                mediaPlayer.stop();
                accept(request_id);
                countDownTimer.cancel();

            }
        });
        requestDialog.show();
    }

    private void accept(String request_id) {
        if (!AndyUtils.isNetworkAvailable(this)) {

            return;
        }
        Commonutils.progressdialog_show(this, getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_ACCEPTED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, request_id);
        Log.d("mahi", "accept map" + map);
        new VollyRequester(this, Const.POST, map,
                Const.ServiceCode.DRIVER_ACCEPTED, this);

    }

    private void reject(String request_id) {

        if (!AndyUtils.isNetworkAvailable(this)) {

            return;
        }
        Commonutils.progressdialog_show(this, getResources().getString(R.string.txt_respod));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.PROVIDER_REJECTED_URL);
        map.put(Const.Params.ID, pHelper.getUserId());
        map.put(Const.Params.TOKEN, pHelper.getSessionToken());
        map.put(Const.Params.REQUEST_ID, request_id);

        new VollyRequester(this, Const.POST, map,
                Const.ServiceCode.DRIVER_REJECTED, this);
    }

    @Override
    public void onLocationReceived(LatLng latlong) {


    }


    @Override
    public void onLocationReceived(Location location) {


    }

    @Override
    public void onConntected(Bundle bundle) {

    }

    @Override
    public void onConntected(Location location) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (isRecieverRegistered) {

            unregisterReceiver(GpsChangeReceiver);
            unregisterReceiver(internetConnectionReciever);
        }

    }

    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            tv_timer.setText("" + (l / 1000));
            mediaPlayer.start();
        }

        @Override
        public void onFinish() {
            pHelper.clearRequestData();
            if (requestDialog != null && requestDialog.isShowing()) {
                requestDialog.cancel();
                mediaPlayer.stop();
                startCheckingUpcomingRequests();
                isShown = false;
            }

        }
    }

    private void openExitDialog() {

        final Dialog exit_dialog = new Dialog(this);
        exit_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        exit_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        exit_dialog.setCancelable(true);
        exit_dialog.setContentView(R.layout.exit_layout);
        TextView tvExitOk = (TextView) exit_dialog.findViewById(R.id.tvExitOk);
        TextView tvExitCancel = (TextView) exit_dialog.findViewById(R.id.tvExitCancel);
        tvExitOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exit_dialog.dismiss();
                finishAffinity();
            }
        });
        tvExitCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exit_dialog.dismiss();
            }
        });
        exit_dialog.show();
    }
}
