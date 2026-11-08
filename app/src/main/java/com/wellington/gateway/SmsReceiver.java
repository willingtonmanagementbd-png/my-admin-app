package com.wellington.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class SmsReceiver extends BroadcastReceiver {
    private static final OkHttpClient client = new OkHttpClient();

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = sms.getDisplayOriginatingAddress();
                        String messageBody = sms.getMessageBody();

                        // শুধুমাত্র বিকাশ বা নগদ এর এসএমএস ফিল্টার করা
                        if (sender.contains("16247") || sender.contains("16167") || 
                            sender.toLowerCase().contains("bkash") || sender.toLowerCase().contains("nagad")) {
                            
                            sendToLaravel(context, sender, messageBody);
                        }
                    }
                }
            }
        }
    }

    private void sendToLaravel(Context context, String sender, String message) {
        SharedPreferences pref = context.getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
        String serverUrl = pref.getString("server_url", "");
        String token = pref.getString("secret_token", "");

        if (serverUrl.isEmpty()) return;

        try {
            JSONObject json = new JSONObject();
            json.put("sender", sender);
            json.put("message", message);

            RequestBody body = RequestBody.create(
                json.toString(), MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                .url(serverUrl + "/api/device/webhook/payment-sms")
                .addHeader("X-SECRET-TOKEN", token)
                .post(body)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception ignored) {}
    }
}
