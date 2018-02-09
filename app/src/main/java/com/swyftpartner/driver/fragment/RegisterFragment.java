package com.swyftpartner.driver.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.bumptech.glide.Glide;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.activity.LoginActivity;
import com.swyftpartner.driver.activity.MainActivity;
import com.swyftpartner.driver.adapter.SpinnerAdapter;
import com.swyftpartner.driver.adapter.TaxiAdapter;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.MultiPartRequester;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.model.SocialMediaProfile;
import com.swyftpartner.driver.model.TaxiTypes;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ItemClickSupport;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;
import com.swyftpartner.driver.utils.ReadFiles;
import com.soundcloud.android.crop.Crop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

/**
 * Created by user on 1/5/2017.
 */

public class RegisterFragment extends BaseRegFragment implements AsyncTaskCompleteListener {
    private ImageButton btn_back_reg;
    private String type = Const.MANUAL;
    private EditText et_register_first_name, et_register_last_name, et_register_your_email,
            et_register_your_password, et_register_phone, et_vehicle_model, et_vehicle_colour, et_vehicle_number;
    private CircleImageView iv_register_user_icon;
    private ImageView iv_vehicle_image;
    private AQuery aQuery;
    private String socialUrl;
    private ImageButton vis_pass;
    private boolean isclicked = false;
    private LinearLayout password_lay;
    private String filePath = "", filePath_vehicle = "";
    private String socialId;
    private Spinner sp_code, sp_country_reg, sp_curency_reg;
    private ArrayList<String> countryCodes, countryCodesIso;
    private SpinnerAdapter adapter_currencey, adapter;
    private Uri uri = null;
    private File cameraFile;
    private TextView rigister_btn;
    private String sFirstName, sLastName, sEmailId, sPassword, medical_no, phone, model, colour, plate_number;
    private ParseContent pcontent;
    private RadioGroup radioGroup;
    private RadioButton rd_btn;
    private TaxiAdapter typesAdapter;
    private ArrayList<TaxiTypes> taxiLst;
    private RecyclerView lst_vehicle;
    private int service_id = -1;
    private boolean isClicked = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_fragment, container, false);
        btn_back_reg = (ImageButton) view.findViewById(R.id.btn_back_reg);
        btn_back_reg.setOnClickListener(this);
        aQuery = new AQuery(activity);
        countryCodes = new ArrayList<>();
        countryCodesIso = new ArrayList<>();
        pcontent = new ParseContent(activity);
        iv_register_user_icon = (CircleImageView) view.findViewById(R.id.iv_register_user_icon);
        iv_vehicle_image = (ImageView) view.findViewById(R.id.iv_register_vehicle_icon);
        et_register_first_name = (EditText) view.findViewById(R.id.et_register_first_name);
        et_register_last_name = (EditText) view.findViewById(R.id.et_register_last_name);
        et_register_your_email = (EditText) view.findViewById(R.id.et_register_your_email);
        et_register_your_password = (EditText) view.findViewById(R.id.et_register_your_password);
        et_register_phone = (EditText) view.findViewById(R.id.et_register_phone);
        et_vehicle_model = (EditText) view.findViewById(R.id.et_register_vehicle_model);
        et_vehicle_colour = (EditText) view.findViewById(R.id.et_register_vehicle_colour);
        et_vehicle_number = (EditText) view.findViewById(R.id.et_register_vehicle_number);
        rigister_btn = (TextView) view.findViewById(R.id.rigister_btn);
        password_lay = (LinearLayout) view.findViewById(R.id.password_lay);
        vis_pass = (ImageButton) view.findViewById(R.id.vis_pass);
        sp_code = (Spinner) view.findViewById(R.id.sp_code);
        sp_country_reg = (Spinner) view.findViewById(R.id.sp_country_reg);
        sp_curency_reg = (Spinner) view.findViewById(R.id.sp_curency_reg);
        radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        lst_vehicle = (RecyclerView) view.findViewById(R.id.lst_vehicle);

        iv_register_user_icon.setOnClickListener(this);
        iv_vehicle_image.setOnClickListener(this);
        rigister_btn.setOnClickListener(this);

        lst_vehicle.setLayoutManager(new GridLayoutManager(activity, 3));

        ItemClickSupport.addTo(lst_vehicle)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        // do it
                        service_id = Integer.valueOf(taxiLst.get(position).getId());
                        typesAdapter.ItemClicked(position);

                    }
                });


        et_register_your_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vis_pass.setVisibility(View.VISIBLE);
                vis_pass.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isclicked == false) {
                            et_register_your_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            et_register_your_password.setSelection(et_register_your_password.getText().length());
                            isclicked = true;
                            vis_pass.setVisibility(View.VISIBLE);

                        } else {
                            isclicked = false;
                            et_register_your_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            vis_pass.setVisibility(View.VISIBLE);

                            et_register_your_password.setSelection(et_register_your_password.getText().length());
                        }
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        setSpinners();

        return view;

    }

    private void getTaxiTypes() {
        if (!AndyUtils.isNetworkAvailable(activity)) {
            return;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.TAXI_TYPE);
        Log.d("mahi", map.toString());
        new VollyRequester(activity, Const.GET, map, Const.ServiceCode.TAXI_TYPE,
                this);
    }


    private void setSpinners() {
        ArrayAdapter<String> countryCodeAdapter = new ArrayAdapter<String>(activity, R.layout.spinner_item, parseCountryCodes());
        //  countryCodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_code.setAdapter(countryCodeAdapter);
        TelephonyManager tm = (TelephonyManager) activity.getSystemService(activity.TELEPHONY_SERVICE);
        String countryCodeValue = tm.getNetworkCountryIso();
        for (int i = 0; i < countryCodesIso.size(); i++) {
            if (countryCodesIso.get(i).equalsIgnoreCase(countryCodeValue)) {
                sp_code.setSelection(i);
            }
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, R.layout.spinner_item, parseCountry());
        sp_country_reg.setAdapter(adapter);

        String[] lst_currency = getResources().getStringArray(R.array.currency);
        Integer[] currency_imageArray = {R.drawable.us, R.drawable.ic_india};

        adapter_currencey = new SpinnerAdapter(activity, R.layout.spinner_value_layout, lst_currency, currency_imageArray);
        sp_curency_reg.setAdapter(adapter_currencey);
    }

    public ArrayList<String> parseCountry() {
        String response = "";
        ArrayList<String> list = new ArrayList<String>();
        try {
            response = ReadFiles.readRawFileAsString(activity,
                    R.raw.countrycodes);

            JSONArray array = new JSONArray(response);
            Log.d("mahi", "countries" + response);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                list.add(object.getString("name"));
            }

            Collections.sort(list);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<String> parseCountryCodes() {
        String response = "";
        ArrayList<String> list = new ArrayList<String>();
        try {
            response = ReadFiles.readRawFileAsString(activity,
                    R.raw.countrycodes);

            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                list.add(object.getString("alpha-2") + " (" + object.getString("phone-code") + ")");
                countryCodes.add(object.getString("phone-code"));
                countryCodesIso.add(object.getString("alpha-2"));
            }

            Collections.sort(list);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back_reg:
                Intent i = new Intent(activity, LoginActivity.class);
                startActivity(i);
                break;
            case R.id.iv_register_user_icon:
                showPictureDialog();
                break;
            case R.id.iv_register_vehicle_icon:
                isClicked = true;
                showPictureDialog();
                break;
            case R.id.rigister_btn:
                if (validate()) {

                    registeration(type, socialId);
                }
                break;

        }
    }

    private void registeration(String type, String socialId) {

        Commonutils.progressdialog_show(activity, getResources().getString(R.string.reg_load));
        // Speciality speclty = speciality_list.get(pos);

        if (type.equals(Const.MANUAL)) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(Const.Params.URL, Const.ServiceType.REGISTER);
            map.put(Const.Params.FIRSTNAME, sFirstName);
            map.put(Const.Params.LAST_NAME, sLastName);
            map.put(Const.Params.EMAIL, sEmailId);
            map.put(Const.Params.PASSWORD, et_register_your_password.getText().toString());
            map.put(Const.Params.PICTURE, filePath);
            map.put(Const.Params.MODEL, model);
            map.put(Const.Params.CAR_IMAGE, filePath_vehicle);
            map.put(Const.Params.COLOR, colour);
            map.put(Const.Params.PLATE_NUMBER, plate_number);
            // map.put(Const.Params.SPECIALITY, String.valueOf(speclty.getId()));
            map.put(Const.Params.DEVICE_TOKEN, new PreferenceHelper(activity).getDeviceToken());
            map.put(Const.Params.DEVICE_TYPE, Const.DEVICE_TYPE_ANDROID);

            map.put(Const.Params.LOGIN_BY, Const.MANUAL);
            String[] items1 = sp_code.getSelectedItem().toString().split(" ");
            String country = items1[0];
            String code = items1[1];
            map.put(Const.Params.PHONE, code.replace("(", "").replace(")", "") + "" + phone);

            // map.put(Const.Params.CURRENCEY, sp_curency_reg.getSelectedItem().toString());
            map.put(Const.Params.TIMEZONE, TimeZone.getDefault().getID());
            map.put("service_type", String.valueOf(service_id));
            // map.put(Const.Params.COUNTRY, sp_country_reg.getSelectedItem().toString());

            int selectedId = radioGroup.getCheckedRadioButtonId();
            rd_btn = (RadioButton) activity.findViewById(selectedId);
            map.put(Const.Params.GENDER, rd_btn.getText().toString());

            Log.d("mahi", map.toString());
            new MultiPartRequester(activity, map, Const.ServiceCode.REGISTER,
                    this);
        } else {
            registerSocial(socialId, type);
        }

    }

    private void registerSocial(String socialId, String type) {

        //Speciality speclty = speciality_list.get(pos);


        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.REGISTER);
        map.put(Const.Params.FIRSTNAME, sFirstName);
        map.put(Const.Params.LAST_NAME, sLastName);
        map.put(Const.Params.EMAIL, sEmailId);
        map.put(Const.Params.PICTURE, filePath);

        // map.put(Const.Params.SPECIALITY, String.valueOf(speclty.getId()));
        map.put(Const.Params.DEVICE_TOKEN,
                new PreferenceHelper(activity).getDeviceToken());
        map.put(Const.Params.DEVICE_TYPE, Const.DEVICE_TYPE_ANDROID);

        map.put(Const.Params.LOGIN_BY, type);
        map.put(Const.Params.SOCIAL_ID, socialId);


        String[] items1 = sp_code.getSelectedItem().toString().split(" ");
        String country = items1[0];
        String code = items1[1];
        map.put(Const.Params.PHONE, code.replace("(", "").replace(")", "") + "" + phone);

        //map.put(Const.Params.CURRENCEY, sp_curency_reg.getSelectedItem().toString());
        map.put(Const.Params.TIMEZONE, TimeZone.getDefault().getID());
        map.put("service_type", String.valueOf(service_id));
        // map.put(Const.Params.COUNTRY, sp_country_reg.getSelectedItem().toString());

        int selectedId = radioGroup.getCheckedRadioButtonId();
        rd_btn = (RadioButton) activity.findViewById(selectedId);
        map.put(Const.Params.GENDER, rd_btn.getText().toString());


        Log.d("mahi", "social reg" + map.toString());
        new MultiPartRequester(activity, map, Const.ServiceCode.REGISTER,
                this);

    }


    private boolean validate() {
        getAllRegisterDetails();

        if (sFirstName.length() == 0) {
            et_register_first_name.setError(getResources().getString(R.string.txt_fname_error));
            et_register_first_name.requestFocus();
            return false;

        } else if (sLastName.length() == 0) {
            et_register_last_name.setError(getResources().getString(R.string.txt_lname_error));
            et_register_last_name.requestFocus();
            return false;

        } else if (sEmailId.length() == 0) {
            et_register_your_email.setError(getResources().getString(R.string.txt_email_error));
            et_register_your_email.requestFocus();
            return false;

        } else if (!AndyUtils.eMailValidation(sEmailId)) {
            et_register_your_email.setError(getResources().getString(R.string.txt_incorrect_error));
            et_register_your_email.requestFocus();
            return false;

        } else if (filePath == null || filePath.equals("")) {
            AndyUtils.showLongToast(getResources().getString(R.string.txt_picture_error), activity);
            return false;

        } else if (filePath_vehicle == null || filePath_vehicle.equals("")) {
            AndyUtils.showLongToast(getResources().getString(R.string.txt__vehicle_picture_error), activity);
            return false;

        } else if (phone.length() == 0) {
            et_register_phone.setError(getResources().getString(R.string.txt_phone_error));
            et_register_phone.requestFocus();
            return false;

        } else if (plate_number.length() == 0) {
            et_vehicle_number.setError(getResources().getString(R.string.txt_plate_error));
            et_vehicle_number.requestFocus();
            return false;

        } else if (colour.length() == 0) {
            et_vehicle_colour.setError(getResources().getString(R.string.txt_colour_error));
            et_vehicle_colour.requestFocus();
            return false;

        } else if (model.length() == 0) {
            et_vehicle_model.setError(getResources().getString(R.string.txt_model_error));
            et_vehicle_model.requestFocus();
            return false;

        } else if (service_id == -1) {
            AndyUtils.showLongToast(getResources().getString(R.string.error_select_taxi), activity);
            return false;
        }

        if (password_lay.getVisibility() == View.VISIBLE) {
            if (TextUtils.isEmpty(et_register_your_password.getText().toString())) {
                et_register_your_password.setError(getResources().getString(R.string.txt_pass_error));
                et_register_your_password.requestFocus();
                return false;

            } else if (et_register_your_password.getText().toString().length() < 8) {
                et_register_your_password.setError(getResources().getString(R.string.txt_pass8_error));
                et_register_your_password.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void getAllRegisterDetails() {
        sFirstName = et_register_first_name.getText().toString().trim();
        sLastName = et_register_last_name.getText().toString().trim();
        sEmailId = et_register_your_email.getText().toString().trim();
        phone = et_register_phone.getText().toString();
        model = et_vehicle_model.getText().toString();
        colour = et_vehicle_colour.getText().toString();
        plate_number = et_vehicle_number.getText().toString();
        if (!type.equals(Const.MANUAL)) {
            filePath = new AQuery(activity).getCachedFile(socialUrl)
                    .getAbsolutePath();
        }
    }

    private void showPictureDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(getResources().getString(R.string.txt_slct_option));
        String[] items = {getResources().getString(R.string.txt_gellery), getResources().getString(R.string.txt_cameray)};

        dialog.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                switch (which) {
                    case 0:
                        choosePhotoFromGallary();
                        break;
                    case 1:
                        takePhotoFromCamera();
                        break;

                }
            }
        });
        dialog.show();
    }

    private void choosePhotoFromGallary() {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        activity.startActivityForResult(i, Const.CHOOSE_PHOTO, Const.REGISTER_FRAGMENT);

    }

    private void takePhotoFromCamera() {
        Calendar cal = Calendar.getInstance();
        cameraFile = new File(Environment.getExternalStorageDirectory(),
                (cal.getTimeInMillis() + ".jpg"));


        if (!cameraFile.exists()) {
            try {
                cameraFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {

            cameraFile.delete();
            try {
                cameraFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        uri = Uri.fromFile(cameraFile);
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(i, Const.TAKE_PHOTO, Const.REGISTER_FRAGMENT);
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = activity.getContentResolver().query(contentURI, null,
                null, null, null);

        if (cursor == null) { // Source is Dropbox or other similar local file
            // path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle mBundle = getArguments();
        if (mBundle != null) {
            SocialMediaProfile mediaProfile = (SocialMediaProfile) mBundle.getSerializable("social_profile");
            if (mediaProfile != null) {
                type = mediaProfile.getLoginType();
                setSocailDetailsOnView(mediaProfile);

            }
        }

        taxiLst = new ArrayList<TaxiTypes>();

        getTaxiTypes();

    }

    private void setSocailDetailsOnView(SocialMediaProfile mediaProfile) {
        aQuery.id(iv_register_user_icon).image(mediaProfile.getPictureUrl(), true, true,
                300, 300, new BitmapAjaxCallback() {
                    @Override
                    public void callback(String url, ImageView iv, Bitmap bm,
                                         AjaxStatus status) {

                        filePath = aQuery.getCachedFile(url).getPath();

                        iv.setImageBitmap(bm);
                    }
                });

        socialId = mediaProfile.getSocialUniqueId();
        et_register_first_name.setText(mediaProfile.getFirstName());
        et_register_last_name.setText(mediaProfile.getLastName());
        et_register_your_email.setText(mediaProfile.getEmailId());
        socialUrl = mediaProfile.getPictureUrl();
        et_register_your_password.setVisibility(View.GONE);
        password_lay.setVisibility(View.GONE);

        Log.d("mahi", "fb profile" + mediaProfile.getSocialUniqueId());
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.currentfragment = Const.REGISTER_FRAGMENT;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("mahi", "req code" + requestCode);

        switch (requestCode) {

            case Const.CHOOSE_PHOTO:
                if (data != null) {

                    uri = data.getData();
                    if (uri != null) {

                        beginCrop(uri);

                    } else {
                        Toast.makeText(activity, getResources().getString(R.string.txt_img_error),
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case Const.TAKE_PHOTO:


                if (uri != null) {
                    beginCrop(uri);
                } else {
                    Toast.makeText(activity, getResources().getString(R.string.txt_img_error),
                            Toast.LENGTH_LONG).show();
                }

                break;
            case Crop.REQUEST_CROP:


                if (data != null)
                    handleCrop(resultCode, data);

                break;
        }
    }

    private void beginCrop(Uri source) {

        Uri outputUri = Uri.fromFile(new File(Environment
                .getExternalStorageDirectory(), (Calendar.getInstance()
                .getTimeInMillis() + ".jpg")));
        Crop.of(source, outputUri).asSquare().start(activity);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            if (isClicked == true) {
                filePath_vehicle = getRealPathFromURI(Crop.getOutput(result));
                Log.e("asher 1 ", filePath_vehicle + " " + Crop.getOutput(result));
                //.setImageURI(Crop.getOutput(result));
                Glide.with(activity).load(filePath_vehicle).override(400, 400).centerCrop().into(iv_vehicle_image);
                isClicked = false;
            } else {

                filePath = getRealPathFromURI(Crop.getOutput(result));
                Log.e("asher 2 ", filePath + " " + Crop.getOutput(result));
                //.setImageURI(Crop.getOutput(result));
                Glide.with(activity).load(filePath).override(400, 400).centerCrop().into(iv_register_user_icon);
            }

        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(activity, Crop.getError(result).getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    @Override
    public void onDestroyView() {

        Fragment fragment = (getFragmentManager()
                .findFragmentById(R.id.frame_login));
        if (fragment.isResumed()) {
            getFragmentManager().beginTransaction().remove(fragment)
                    .commitAllowingStateLoss();
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

    }


    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {

            case Const.ServiceCode.REGISTER:
                Commonutils.progressdialog_hide();
                Log.d("mahi", "reg response" + response);
                if (response != null)
                    try {

                        JSONObject job1 = new JSONObject(response);
                        if (job1.getString("success").equals("true")) {

                            try {
                                if (!filePath.equals("")) {

                                    File file = new File(filePath);
                                    file.getAbsoluteFile().delete();

                                }
                                if (cameraFile != null) {
                                    cameraFile.getAbsoluteFile().delete();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (pcontent.isSuccessWithStoreId(response)) {
                                pcontent.parseUserAndStoreToDb(response);
                                new PreferenceHelper(activity).putPassword(et_register_your_password.getText()
                                        .toString());

                                startActivity(new Intent(activity, MainActivity.class));

                            } else {

                            }

                        } else {

                            String error = job1.getString("error_messages");
                            Commonutils.showtoast(error, activity);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                break;
            case Const.ServiceCode.TAXI_TYPE:
                try {
                    JSONObject job = new JSONObject(response);
                    if (job.optString("success").equals("true")) {
                        taxiLst.clear();
                        JSONArray jArray = job.optJSONArray("services");
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject typesObj = jArray.optJSONObject(i);
                            TaxiTypes types = new TaxiTypes();
                            types.setId(typesObj.optString("id"));
                            types.setTaxitype(typesObj.optString("name"));
                            types.setTaxiimage(typesObj.optString("picture"));
                            taxiLst.add(types);
                        }

                        if (null != taxiLst) {
                            typesAdapter = new TaxiAdapter(activity, taxiLst);
                            lst_vehicle.setAdapter(typesAdapter);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;


        }
    }

}
