package com.swyftpartner.driver.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;

import com.swyftpartner.driver.R;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;

import java.util.HashMap;

public class StatusAvailabilityActivity extends AppCompatActivity implements AsyncTaskCompleteListener {

    private SwitchCompat availabilityStatus;
    private String switchStatus="";
    private ParseContent pContent;
    private Toolbar statusToolbar;
    private ImageButton setting_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.activity_status_availability);
        availabilityStatus = (SwitchCompat) findViewById(R.id.switch_availability);
        statusToolbar= (Toolbar) findViewById(R.id.tb_status);
        setSupportActionBar(statusToolbar);
        getSupportActionBar().setTitle(null);
        setting_back = (ImageButton) findViewById(R.id.setting_back);
        setting_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        pContent=new ParseContent(this);
        getSupportActionBar().setTitle(getString(R.string.setting));
        statusToolbar.setTitleTextColor(Color.parseColor("#FE9700"));
        getSupportActionBar().setHomeButtonEnabled(true);
        availabilityStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if (b)
                {
                  switchStatus="1";
                  updateAvailabilityStatus(switchStatus);
                }
                else
                {
                    switchStatus="0";
                    updateAvailabilityStatus(switchStatus);
                }

            }
        });


        checkAvailabilityStatus();
    }

    private void updateAvailabilityStatus(String status) {
        if (!AndyUtils.isNetworkAvailable(this)) {
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.POST_AVAILABILITY_STATUS_URL);
        map.put(Const.Params.ID, new PreferenceHelper(this).getUserId());
        map.put(Const.Params.TOKEN, new PreferenceHelper(this).getSessionToken());
        map.put(Const.Params.STATUS, String.valueOf(status));

        AndyUtils.appLog("Ashutosh", "updateAvailabilityMap" + map);

        new VollyRequester(this, Const.POST, map, Const.ServiceCode.POST_AVAILABILITY_STATUS, this);
    }

    private void checkAvailabilityStatus() {
        if (!AndyUtils.isNetworkAvailable(this)) {
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.GET_CHECK_AVAILABLE_STATUS_URL + Const.Params.ID + "="
                + new PreferenceHelper(this).getUserId() + "&" + Const.Params.TOKEN + "="
                + new PreferenceHelper(this).getSessionToken());

        AndyUtils.appLog("Ashutosh", "CheckAvailabilityMap" + map);

        new VollyRequester(this, Const.GET, map, Const.ServiceCode.GET_CHECK_AVAILABLE_STATUS, this);
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {


        switch (serviceCode) {
            case Const.ServiceCode.GET_CHECK_AVAILABLE_STATUS:
                AndyUtils.appLog("Ashutosh","ChechStatusAvailabilityResponse" +response);
                if (pContent.isSuccess(response))
                {
                     if(pContent.getActiveStatus(response)==0)
                     {
                         availabilityStatus.setChecked(false);
                     }
                     else if(pContent.getActiveStatus(response)==1)
                     {
                         availabilityStatus.setChecked(true);
                     }
                }
                break;
            case Const.ServiceCode.POST_AVAILABILITY_STATUS:
                AndyUtils.appLog("Ashutosh","PostAvailabilityResponse"+response);
                if (pContent.isSuccess(response))
                {
                    if(pContent.getActiveStatus(response)==0)
                    {
                        availabilityStatus.setChecked(false);
                    }
                    else if(pContent.getActiveStatus(response)==1)
                    {
                        availabilityStatus.setChecked(true);
                    }
                }
                break;
        }
    }
}
