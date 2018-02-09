package com.swyftpartner.driver.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.swyftpartner.driver.R;
import com.swyftpartner.driver.adapter.UploadAdapter;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.model.Uploads;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.PreferenceHelper;
import com.swyftpartner.driver.utils.RecyclerLongPressClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by user on 1/26/2017.
 */

public class UploadDocActivity extends AppCompatActivity implements AsyncTaskCompleteListener {
    private Toolbar docToolbar;
    private ImageButton doc_back;
    private RecyclerView upload_lv;
    private ProgressBar upload_progress_bar;
    private ArrayList<Uploads> uploadlst;
    private UploadAdapter uploadAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.activity_documents);
        setResult(RESULT_OK);
        uploadlst = new ArrayList<Uploads>();
        getDoc();

        docToolbar = (Toolbar) findViewById(R.id.toolbar_upload);
        upload_lv = (RecyclerView) findViewById(R.id.upload_lv);
        upload_progress_bar = (ProgressBar) findViewById(R.id.upload_progress_bar);
        upload_progress_bar.setVisibility(View.VISIBLE);


        setSupportActionBar(docToolbar);
        getSupportActionBar().setTitle(null);
        doc_back = (ImageButton) findViewById(R.id.upload_back);
        doc_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        upload_lv.addOnItemTouchListener(new RecyclerLongPressClickListener(this, upload_lv, new RecyclerLongPressClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {


            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (null != uploadAdapter) {
            uploadAdapter.onActivityResult(requestCode, resultCode, data);
        }

    }

    private void getDoc() {
        if (!AndyUtils.isNetworkAvailable(this)) {
            return;
        }

        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.GET_DOC + Const.Params.ID + "="
                + new PreferenceHelper(this).getUserId() + "&" + Const.Params.TOKEN + "="
                + new PreferenceHelper(this).getSessionToken());

        new VollyRequester(this, Const.GET, map, Const.ServiceCode.GET_DOC, this);
    }


    @Override
    public void onTaskCompleted(String response, int serviceCode) {


        switch (serviceCode) {
            case Const.ServiceCode.GET_DOC:
                Log.d("mahi", "get doc" + response);
                if (response != null) {
                    try {
                        JSONObject job = new JSONObject(response);
                        if (job.getString("success").equals("true")) {
                            uploadlst.clear();
                            upload_progress_bar.setVisibility(View.GONE);
                            JSONArray jarray = job.getJSONArray("documents");
                            if (jarray.length() > 0) {
                                for (int i = 0; i < jarray.length(); i++) {
                                    JSONObject docjob = jarray.getJSONObject(i);
                                    Uploads upload = new Uploads();
                                    upload.setUpload_id(docjob.getString("id"));
                                    upload.setUpload_name(docjob.getString("name"));
                                    upload.setUpload_img(docjob.getString("document_url"));
                                    uploadlst.add(upload);

                                }

                                if (uploadlst != null) {
                                    uploadAdapter = new UploadAdapter(this, uploadlst);
                                    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
                                    upload_lv.setLayoutManager(mLayoutManager);
                                    upload_lv.setItemAnimator(new DefaultItemAnimator());
                                    upload_lv.setAdapter(uploadAdapter);
                                }

                            }

                        } else {
                            upload_progress_bar.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                break;
        }

    }

}
