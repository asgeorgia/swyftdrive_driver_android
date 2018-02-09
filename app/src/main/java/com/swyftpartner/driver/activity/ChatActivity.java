package com.swyftpartner.driver.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.swyftpartner.driver.R;
import com.swyftpartner.driver.adapter.ChatAdapter;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.model.ChatObject;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by user on 2/18/2017.
 */

public class ChatActivity extends AppCompatActivity implements AsyncTaskCompleteListener {
    private Toolbar chatToolbar;
    private ListView chat_lv;
    private ImageView btn_send;
    ChatAdapter messageAdapter;
    DatabaseHandler db;
    private List<ChatObject> messages;
    private Socket mSocket;
    private Boolean isConnected = true;
    private EditText et_message;
    private ImageButton chat_back;
    private String reciver_id="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.chat_activity);
        db = new DatabaseHandler(this);
        chatToolbar = (Toolbar) findViewById(R.id.toolbar_chat);


        setSupportActionBar(chatToolbar);
        getSupportActionBar().setTitle(null);

        chat_back = (ImageButton) findViewById(R.id.chat_back);
        chat_lv = (ListView) findViewById(R.id.chat_lv);
        et_message = (EditText) findViewById(R.id.et_message);
        btn_send = (ImageView) findViewById(R.id.btn_send);
        messages = db.get_Chat("" + new PreferenceHelper(this).getRequestId());
        if (messages != null && messages.size() > 0) {
            messageAdapter = new ChatAdapter(this, R.layout.list_item_chat_message, messages);
            // messageAdapter.notifyDataSetChanged();
            chat_lv.setAdapter(messageAdapter);
        }

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (et_message.getText().toString().length() == 0) {
                    Commonutils.showtoast(getResources().getString(R.string.txt_error_enter_msg), getApplicationContext());
                    et_message.requestFocus();
                } else {
                    if (mSocket.connected()) {
                        attemptSend(et_message.getText().toString());
                        et_message.setText("");
                       // AndyUtils.hideKeyBoard(getApplicationContext());
                    }
                }

            }
        });
        chat_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        Intent intent = getIntent();
        if(intent!=null){
            reciver_id = intent.getStringExtra("reciver_id");
            initiateSokect();
        }
    }

    private void attemptSend(String message) {
        if (!mSocket.connected()) return;

        JSONObject messageObj = new JSONObject();
        try {
            messageObj.put("message", message);
            messageObj.put("sender", new PreferenceHelper(this).getUserId());

            messageObj.put("receiver", reciver_id);
            messageObj.put("type", "sent");
            messageObj.put("data_type", "TEXT");
            messageObj.put("status", "1");
            messageObj.put("request_id", new PreferenceHelper(this).getRequestId());

            Log.e("mahi", "calling socket" + messageObj.toString());
            showChat("sent", "TEXT", message);
            mSocket.emit("send location", messageObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initiateSokect() {

        try {
            mSocket = IO.socket(Const.ServiceType.SOCKET_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("message", onNewMessage);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.connect();
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {

    }

    private void showChat(String type, String data_type, String message) {

        if (messages == null || messages.size() == 0) {

            messages = new ArrayList<ChatObject>();
        }

        messages.add(new ChatObject(message, type, data_type, ""
                + new PreferenceHelper(this).getRequestId(), new PreferenceHelper(this).getUserId(), reciver_id));

        ChatAdapter chatAdabter = new ChatAdapter(ChatActivity.this, R.layout.list_item_chat_message, messages);

        chat_lv.setAdapter(chatAdabter);
        // chatAdabter.notifyDataSetChanged();

        ChatObject chat = new ChatObject(message, type, data_type, ""
                + new PreferenceHelper(this).getRequestId(), new PreferenceHelper(this).getUserId(), reciver_id);

        db.insertChat(chat);

        // chatAdabter.notifyDataSetChanged();

    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {

                        try {

                            JSONObject object = new JSONObject();
                            object.put("sender", new PreferenceHelper(getApplicationContext()).getUserId());
                            object.put("receiver", reciver_id);
                            Log.e("update_object", "" + object);
                            mSocket.emit("update sender", object);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        isConnected = true;
                    }
                    if (isConnected) {


                        try {

                            JSONObject object = new JSONObject();
                            object.put("sender", new PreferenceHelper(getApplicationContext()).getUserId());
                            object.put("receiver", reciver_id);
                            Log.e("update_object", "" + object);
                            mSocket.emit("update sender", object);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;

                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    String type;
                    String data_type;
                    String message;
                    try {

                        type = data.getString("type");
                        data_type = data.getString("data_type");
                        message = data.getString("message");
                        Log.d("mahi", "response mssage" + message);
                        showChat("receive", data_type, message);

                    } catch (JSONException e) {
                        return;
                    }


                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }
/*
                    addLog(getResources().getString(R.string.message_user_joined, username));
                    addParticipantsLog(numUsers);*/
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

  /*  private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("stop typing");
        }
    };*/

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
    }
}
