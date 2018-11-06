package com.qwe7002.telegram_sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class boot_receiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
            Log.d(public_func.log_tag, "onReceive: boot_completed");
            Intent battery_service = new Intent(context, battery_listener_service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(battery_service);
            } else {
                context.startService(battery_service);
            }
            final SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
            if (sharedPreferences.getBoolean("webhook", false)) {
                Intent webhook_service = new Intent(context, webhook_service.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(webhook_service);
                } else {
                    context.startService(webhook_service);
                }
            }
            String bot_token = sharedPreferences.getString("bot_token", "");
            String chat_id = sharedPreferences.getString("chat_id", "");
            assert bot_token != null;
            assert chat_id != null;
            if (bot_token.isEmpty() || chat_id.isEmpty()) {
                Log.i(public_func.log_tag, "onReceive: token not found");
                return;
            }
            String request_uri = "https://api.telegram.org/bot" + bot_token + "/sendMessage";
            request_json request_body = new request_json();
            request_body.chat_id=chat_id;
            request_body.text = context.getString(R.string.system_message_head) + "\n" + context.getString(R.string.success_connect);
            Gson gson = new Gson();
            String request_body_raw = gson.toJson(request_body);
            RequestBody body = RequestBody.create(public_func.JSON, request_body_raw);
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url(request_uri).method("POST", body).build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    String error_message = "Send Startup Error:"+ e.getMessage();
                    Log.i(public_func.log_tag, error_message);
                    public_func.write_log(context,error_message);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.code() != 200) {
                        assert response.body() != null;
                        String error_message = "Send Startup Error:" + response.body().string();
                        public_func.write_log(context,error_message);
                    }
                }
            });
        }
    }
}
