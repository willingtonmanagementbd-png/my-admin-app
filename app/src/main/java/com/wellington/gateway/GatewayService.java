package com.wellington.gateway;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

public class GatewayService extends Service {
    private final OkHttpClient client = new OkHttpClient();
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "gateway_channel")
                .setContentTitle("Wellington Gateway Active")
                .setContentText("Listening for bKash/Nagad & Dispatching SMS...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }

        startPolling();
        return START_STICKY;
    }

    private void startPolling() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchAndSendPendingSms();
            }
        }, 1000, 5000);
    }

    private void fetchAndSendPendingSms() {
        SharedPreferences pref = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
        String serverUrl = pref.getString("server_url", "");
        String token = pref.getString("secret_token", "");

        if (serverUrl == null || serverUrl.isEmpty()) return;
        serverUrl = serverUrl.replaceAll("/+$", "");

        Request request = new Request.Builder()
                .url(serverUrl + "/api/device/sms/pending")
                .addHeader("X-SECRET-TOKEN", token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                JSONArray array = new JSONArray(jsonData);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int id = obj.getInt("id");
                    String phone = obj.getString("phone");
                    String message = obj.getString("message");

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phone, null, message, null, null);
                        markSent(serverUrl, token, id, "sent");
                    } catch (Exception e) {
                        markSent(serverUrl, token, id, "failed");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void markSent(String serverUrl, String token, int id, String status) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("status", status);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request req = new Request.Builder()
                    .url(serverUrl + "/api/device/sms/mark-sent")
                    .addHeader("X-SECRET-TOKEN", token)
                    .post(body)
                    .build();

            client.newCall(req).execute().close();
        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "gateway_channel", 
                    "Wellington Gateway Service", 
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Running Gateway background service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
