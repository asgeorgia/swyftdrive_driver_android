package com.swyftpartner.driver.activity;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.adapter.PlacesAutoCompleteAdapter;
import com.swyftpartner.driver.fragment.BaseMapFragment;
import com.swyftpartner.driver.utils.AndyUtils;

import java.util.List;

/**
 * Created by user on 1/7/2017.
 */

public class SearchPlaceActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar search_mainToolbar;
    private ImageButton search_back;
    private AutoCompleteTextView et_source_address, et_destination_address;
    private PlacesAutoCompleteAdapter placesadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.source_destination_layout);
        search_mainToolbar = (Toolbar) findViewById(R.id.toolbar_search_place);
        setSupportActionBar(search_mainToolbar);
        getSupportActionBar().setTitle(null);

        search_back = (ImageButton) findViewById(R.id.search_back);
        et_source_address = (AutoCompleteTextView) findViewById(R.id.et_source_address);
        et_destination_address = (AutoCompleteTextView) findViewById(R.id.et_destination_address);

        placesadapter = new PlacesAutoCompleteAdapter(this,
                R.layout.autocomplete_list_text);

        if (placesadapter != null) {
            et_source_address.setAdapter(placesadapter);
            et_destination_address.setAdapter(placesadapter);
        }
        et_source_address.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                et_source_address.setSelection(0);
                LatLng latLng = getLocationFromAddress(getApplicationContext(), et_source_address.getText().toString());
                if (latLng != null) {
                    BaseMapFragment.pic_latlan = latLng;

                }
            }
        });

        et_destination_address.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                et_destination_address.setSelection(0);

                LatLng des_latLng = getLocationFromAddress(getApplicationContext(), et_destination_address.getText().toString());

                if (des_latLng != null) {
                    BaseMapFragment.drop_latlan = des_latLng;
                    BaseMapFragment.searching =true;
                    AndyUtils.hideKeyBoard(getApplicationContext());
                }
                onBackPressed();
            }
        });
        search_back.setOnClickListener(this);
        et_destination_address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //Commonutils.showtoast("search clicked", getApplicationContext());
                    onBackPressed();
                    BaseMapFragment.searching =true;
                    AndyUtils.hideKeyBoard(getApplicationContext());
                    return true;
                }
                return false;
            }
        });

        String source_address = "";
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                source_address = "";
            } else {
                source_address = extras.getString("pickup_address");
                et_source_address.setText(source_address);
                et_source_address.setSelection(0);

            }
        } else {
            source_address = (String) savedInstanceState.getSerializable("pickup_address");
            et_source_address.setText(source_address);
            et_source_address.setSelection(0);
        }

        et_destination_address.requestFocus();

        LatLng latLng = getLocationFromAddress(this, et_source_address.getText().toString());

        if (latLng != null) {
            BaseMapFragment.pic_latlan = latLng;
        }


    }

    @Override
    public void onBackPressed() {
        // code here to show dialog
        super.onBackPressed();  // optional depending on your needs
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_back:
                onBackPressed();
                break;

        }

    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return p1;
    }
}
