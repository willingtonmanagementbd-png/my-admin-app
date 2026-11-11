package com.wellington.gateway;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GatewayService extends Service {
    private final OkHttpClient client = new OkHttpClient();
    private Timer timer;
    private static final String ACTION_SMS_SENT = "com.wellington.gateway.SMS_SENT_ACTION";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerSmsDeliveryReceiver();
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
        }, 1000, 4000);
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
                    String phone = obj.getString("phone").trim();
                    String message = obj.getString("message");

                    // নম্বর থেকে অপ্রয়োজনীয় চিহ্ন পরিষ্কার করা
                    phone = phone.replaceAll("[^0-9+]", "");

                    sendPhysicalSms(id, phone, message, serverUrl, token);
                }
            }
        } catch (Exception ignored) {}
    }

    private void sendPhysicalSms(int id, String phone, String message, String serverUrl, String token) {
        try {
            SmsManager smsManager = null;

            // ডুয়াল সিমের ক্ষেত্রে সঠিক ডিফল্ট সিম সনাক্ত করা
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subManager != null) {
                    int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
                    if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                    }
                }
            }

            if (smsManager == null) {
                smsManager = SmsManager.getDefault();
            }

            // ডেলিভারি ইন্টেন্ট ট্র্যাকিং
            Intent sentIntent = new Intent(ACTION_SMS_SENT);
            sentIntent.putExtra("sms_id", id);
            sentIntent.putExtra("server_url", serverUrl);
            sentIntent.putExtra("secret_token", token);
            sentIntent.putExtra("phone", phone);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(this, id, sentIntent, flags);

            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() > 1) {
                ArrayList<PendingIntent> sentPIs = new ArrayList<>();
                sentPIs.add(pi);
                for (int p = 1; p < parts.size(); p++) {
                    sentPIs.add(null);
                }
                smsManager.sendMultipartTextMessage(phone, null, parts, sentPIs, null);
            } else {
                smsManager.sendTextMessage(phone, null, message, pi, null);
            }

        } catch (Exception e) {
            markSent(serverUrl, token, id, "failed");
            showToast("SMS Failed: " + e.getMessage());
        }
    }

    private void registerSmsDeliveryReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int id = intent.getIntExtra("sms_id", 0);
                String serverUrl = intent.getStringExtra("server_url");
                String token = intent.getStringExtra("secret_token");
                String phone = intent.getStringExtra("phone");

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        markSent(serverUrl, token, id, "sent");
                        showToast("✅ SMS Sent to " + phone);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        markSent(serverUrl, token, id, "failed");
                        showToast("❌ SMS Failed: Generic Failure (Check Balance/SMS Pack)");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        markSent(serverUrl, token, id, "failed");
                        showToast("❌ SMS Failed: No SIM Network");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        markSent(serverUrl, token, id, "failed");
                        showToast("❌ SMS Failed: Airplane mode / Radio off");
                        break;
                    default:
                        markSent(serverUrl, token, id, "failed");
                        showToast("❌ SMS Failed. Code: " + getResultCode());
                        break;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, new IntentFilter(ACTION_SMS_SENT), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, new IntentFilter(ACTION_SMS_SENT));
        }
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show()
        );
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
