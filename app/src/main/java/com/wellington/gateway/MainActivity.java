package com.wellington.gateway;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl, etToken;
    private Button btnSave;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 80, 50, 50);

        TextView tv = new TextView(this);
        tv.setText("Wellington Gateway (SMS & Pay)");
        tv.setTextSize(22);
        tv.setPadding(0, 0, 0, 16);
        layout.addView(tv);

        tvStatus = new TextView(this);
        tvStatus.setText("Status: Not Started");
        tvStatus.setTextSize(15);
        tvStatus.setPadding(0, 0, 0, 24);
        layout.addView(tvStatus);

        etUrl = new EditText(this);
        etUrl.setHint("Website URL (e.g. https://wellington-info.com)");
        layout.addView(etUrl);

        etToken = new EditText(this);
        etToken.setHint("Secret Token");
        layout.addView(etToken);

        btnSave = new Button(this);
        btnSave.setText("Save & Start Service");
        layout.addView(btnSave);

        setContentView(layout);

        SharedPreferences pref = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
        etUrl.setText(pref.getString("server_url", "https://wellington-info.com"));
        etToken.setText(pref.getString("secret_token", "WELLINGTON_MY_SECRET_KEY_9988"));

        requestAllPermissions();

        btnSave.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim().replaceAll("/+$", "");
            String token = etToken.getText().toString().trim();

            pref.edit()
                .putString("server_url", url)
                .putString("secret_token", token)
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            tvStatus.setText("Status: 🟢 Gateway Service Running!");
            Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.SEND_SMS);

        // Android 13 ও 14 এর জন্য নোটিফিকেশন পারমিশন চাওয়া
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 101);
    }
}
