package com.swyftpartner.driver.gcmHandlers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.activity.ChatActivity;
import com.swyftpartner.driver.activity.MainActivity;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by user on 6/29/2015.
 */
public class GCMIntentService extends GcmListenerService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    private PreferenceHelper preferenceHelper;
    private String date="";



    @Override
    public void onMessageReceived(final String from, final Bundle bundle) {

        Bundle extras = bundle;
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        preferenceHelper = new PreferenceHelper(this);

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        // String messageType = gcm.getMessageType(intent);


        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */


            String recieved_message = bundle.getString("message");
            String msg_title = "", request_id, request_type = "";


            try {
                JSONObject jsonObject=new JSONObject(recieved_message);
                msg_title=jsonObject.getString("title");
                request_type = jsonObject.getString("type");
                JSONObject jsonObject1=jsonObject.optJSONObject("data");
                if(jsonObject1!=null) {
                    int status_position = Integer.parseInt(jsonObject1.optString("status"));
                    AndyUtils.appLog("PushStatusPosition", status_position + "");
                    switch (status_position) {
                        case 6: //cancel Request
                            sendFirstJsonRequest(recieved_message,request_type);
                            sendNotification(msg_title,request_type);
                            break;
                        default:
                            sendNotification(msg_title,request_type);

                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e("mahi", "rec push" + recieved_message);

        }
    }

    private void sendFirstJsonRequest(String recieved_message,String type) {
        Intent newRequestIntent = new Intent(Const.PROVIDER_REQUEST_STATUS);
        newRequestIntent.putExtra(Const.PROVIDER_INTENT_MESSAGE, recieved_message);
        newRequestIntent.putExtra("ashutosh", "pushNotificationByAshutosh");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(newRequestIntent);

    }
        private void sendNotification(String msg,String type){
            mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent chat_intent = null;
            if(type.equals("2")){
                 chat_intent = new Intent(this, ChatActivity.class);
                chat_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                chat_intent.putExtra("newques", "new_quest");

            } else {
                chat_intent = new Intent(this, MainActivity.class);
                chat_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                chat_intent.putExtra("newques", "new_quest");
            }

            PendingIntent contentIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                    chat_intent, 0);
            Notification.Builder mBuilder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setStyle(new Notification.BigTextStyle()
                                    .bigText(msg))
                            .setVibrate(new long[]{100, 500})
                            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                            .setAutoCancel(true)
                            .setContentText(msg);

            mBuilder.setContentIntent(contentIntent);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());


        }




}
