package com.swyftpartner.driver.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.fragment.ForgotpassFragment;
import com.swyftpartner.driver.fragment.RegisterFragment;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.model.SocialMediaProfile;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.ParseContent;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by user on 1/4/2017.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, AsyncTaskCompleteListener{
    private ImageButton btn_cancel;
    public static String currentfragment = "";
    private RelativeLayout log_layout;
    private String loginType = Const.MANUAL;
    private String sFirstName, sLastName, sEmailId, sPassword, sUserName, sSocial_unique_id, pictureUrl;
    private CallbackManager callbackManager;
    private String sPictureUrl;
    private String sLoginUserId, sLoginPassword;

    private String filePath = "";
    private SocialMediaProfile mediaProfile;
    private TextView login_btn,btn_forgot_pass;
    private EditText et_login_password, et_login_userid;
    private int mFragmentId = 0;
    private String mFragmentTag = null;
    private ImageButton loc_pass;
    private boolean isclicked = false;
    private ParseContent pcontent;
    private TextInputLayout input_layout_userid,input_layout_pass;
    private TextView btn_new_driver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        callbackManager = CallbackManager.Factory.create();
        facebookRegisterCallBack();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.login);
        pcontent = new ParseContent(this);
        btn_cancel = (ImageButton) findViewById(R.id.btn_cancel);
        input_layout_userid = (TextInputLayout) findViewById(R.id.input_layout_userid);
        input_layout_pass = (TextInputLayout) findViewById(R.id.input_layout_pass);
        et_login_password = (EditText) findViewById(R.id.et_login_password);
        et_login_userid = (EditText) findViewById(R.id.et_login_userid);
        btn_forgot_pass = (TextView) findViewById(R.id.btn_forgot_pass);
        btn_new_driver = (TextView) findViewById(R.id.btn_new_driver);
        log_layout = (RelativeLayout) findViewById(R.id.log_layout);
        login_btn = (TextView) findViewById(R.id.login_btn);

        btn_cancel.setOnClickListener(this);
        login_btn.setOnClickListener(this);
        btn_forgot_pass.setOnClickListener(this);
        btn_new_driver.setOnClickListener(this);




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
        ft.replace(R.id.frame_login, fragment, tag);
        ft.commitAllowingStateLoss();
    }


    @Override
    public void onResume() {
        super.onResume();
        currentfragment = "";
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                Intent i = new Intent(getApplicationContext(), WelcomeActivity.class);
                startActivity(i);

                break;
            case R.id.login_btn:
                if (validate()) {
                    UserLogin(Const.MANUAL);
                }
                break;
            case R.id.btn_forgot_pass:
                addFragment(new ForgotpassFragment(), false, Const.FORGOT_PASSWORD_FRAGMENT, true);
                log_layout.setVisibility(View.GONE);
                break;
            case R.id.btn_new_driver:
                addFragment(new RegisterFragment(), false, Const.REGISTER_FRAGMENT, true);
                log_layout.setVisibility(View.GONE);
                break;
        }
    }

    private void getLoginDetails() {
        sLoginUserId = et_login_userid.getText().toString().trim();
        sLoginPassword = et_login_password.getText().toString().trim();
    }

    private boolean validate() {
        getLoginDetails();
        if (sLoginUserId.length() == 0) {
            input_layout_userid.setError(getResources().getString(R.string.txt_email_error));
            et_login_userid.requestFocus();
            return false;
        } else if (sLoginPassword.length() == 0) {
            input_layout_pass.setError(getResources().getString(R.string.txt_pass_error));
            et_login_password.requestFocus();
            return false;
        } else {
            input_layout_userid.setError(null);
            input_layout_pass.setError(null);
            return true;
        }
    }


    public void startActivityForResult(Intent intent, int requestCode,
                                       String fragmentTag) {

        mFragmentTag = fragmentTag;
        mFragmentId = 0;
        super.startActivityForResult(intent, requestCode);
    }

    private void facebookRegisterCallBack() {
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            //    private FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                                if (jsonObject != null && graphResponse != null) {
                                    AndyUtils.appLog("Json Object", jsonObject.toString());
                                    AndyUtils.appLog("Graph response", graphResponse.toString());
                                    try {
                                        sUserName = jsonObject.getString("name");
                                        sEmailId = jsonObject.getString("email");
                                        sSocial_unique_id = jsonObject.getString("id");
                                        sPictureUrl = "https://graph.facebook.com/" + sSocial_unique_id + "/picture?type=large";
                                        mediaProfile = new SocialMediaProfile();

                                        if (sUserName != null) {
                                            String[] name = sUserName.split(" ");
                                            if (name[0] != null) {
                                                mediaProfile.setFirstName(name[0]);
                                            }
                                            if (name[1] != null) {
                                                mediaProfile.setLastName(name[1]);
                                            }
                                        }
                                        mediaProfile.setEmailId(sEmailId);
                                        mediaProfile.setSocialUniqueId(sSocial_unique_id);
                                        mediaProfile.setPictureUrl(sPictureUrl);
                                        mediaProfile.setLoginType(Const.SOCIAL_FACEBOOK);

                                        AndyUtils.appLog("all details", sUserName + "" + sEmailId + " " + " " + sPictureUrl);
                                        if (sSocial_unique_id != null) {
                                            loginType = Const.SOCIAL_FACEBOOK;
                                            UserLogin(Const.SOCIAL_FACEBOOK);
                                        } else {
                                            AndyUtils.showShortToast("Invalidate Data", LoginActivity.this);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }

                );
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,link,locale,hometown,email,gender,birthday,location");
                request.setParameters(parameters);
                request.executeAsync();
            }


            @Override
            public void onCancel() {
                AndyUtils.showLongToast(getString(R.string.login_cancelled), LoginActivity.this);
            }

            @Override
            public void onError(FacebookException error) {
                AndyUtils.showLongToast(getString(R.string.login_failed), LoginActivity.this);
                AndyUtils.appLog("login failed Error", error.toString());
            }
        });
    }

    private void UserLogin(String logintype) {

        Commonutils.progressdialog_show(this, getResources().getString(R.string.txt_signin));

        if (logintype.equalsIgnoreCase(Const.MANUAL)) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(Const.Params.URL, Const.ServiceType.LOGIN);
            map.put(Const.Params.EMAIL, sLoginUserId);
            map.put(Const.Params.PASSWORD, sLoginPassword);
            map.put(Const.Params.DEVICE_TOKEN, new PreferenceHelper(this).getDeviceToken());
            map.put(Const.Params.DEVICE_TYPE, Const.DEVICE_TYPE_ANDROID);
            map.put(Const.Params.LOGIN_BY, Const.MANUAL);
            Log.d("mahi", map.toString());
            new VollyRequester(this, Const.POST, map, Const.ServiceCode.LOGIN,
                    this);
        } else {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(Const.Params.URL, Const.ServiceType.LOGIN);
            map.put(Const.Params.SOCIAL_ID, sSocial_unique_id);

            map.put(Const.Params.DEVICE_TOKEN, new PreferenceHelper(this).getDeviceToken());
            map.put(Const.Params.DEVICE_TYPE, Const.DEVICE_TYPE_ANDROID);
            map.put(Const.Params.LOGIN_BY, logintype);

            Log.d("mahi", "social" + map.toString());
            new VollyRequester(this, Const.POST, map, Const.ServiceCode.LOGIN,
                    this);
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Activity Res", "" + requestCode);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = null;

        if (mFragmentId > 0) {
            fragment = getSupportFragmentManager().findFragmentById(
                    mFragmentId);
        } else if (mFragmentTag != null
                && !mFragmentTag.equalsIgnoreCase("")) {
            fragment = getSupportFragmentManager().findFragmentByTag(
                    mFragmentTag);
        }
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {

        if (currentfragment.equals(Const.REGISTER_FRAGMENT) || currentfragment.equals(Const.FORGOT_PASSWORD_FRAGMENT)) {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            log_layout.setVisibility(View.VISIBLE);
        } else {
            Intent i = new Intent(getApplicationContext(), WelcomeActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {
            case Const.ServiceCode.LOGIN:
                Log.d("mahi", "" + response);

                if (response != null) {

                    try {
                        JSONObject job1 = new JSONObject(response);
                        if (job1.getString("success").equals("true")) {
                            Commonutils.progressdialog_hide();
                            if (pcontent.isSuccessWithStoreId(response)) {

                                pcontent.parseUserAndStoreToDb(response);
                                new PreferenceHelper(this).putPassword(et_login_password.getText()
                                        .toString());
                                startActivity(new Intent(this, MainActivity.class));
                                this.finish();
                            } else {

                            }

                        } else {
                            Commonutils.progressdialog_hide();
                            if (job1.getString("error_code").equals("125")) {

                                if (mediaProfile != null) {
                                    RegisterFragment regFragment = new RegisterFragment();
                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable("social_profile", mediaProfile);
                                    regFragment.setArguments(bundle);
                                    addFragment(regFragment, false, Const.REGISTER_FRAGMENT,
                                            true);
                                    log_layout.setVisibility(View.GONE);

                                }

                            } else {
                                String error = job1.getString("error");
                                Commonutils.showtoast(error, this);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LoginManager.getInstance().logOut();

    }

}
