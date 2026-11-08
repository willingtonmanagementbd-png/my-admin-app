package com.wellington.gateway;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl, etToken;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 80, 50, 50);

        TextView tv = new TextView(this);
        tv.setText("Wellington Gateway (SMS & Pay)");
        tv.setTextSize(22);
        layout.addView(tv);

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

        requestPermissions();

        btnSave.setOnClickListener(v -> {
            pref.edit()
                .putString("server_url", etUrl.getText().toString().trim())
                .putString("secret_token", etToken.getText().toString().trim())
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Gateway Service Started!", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        }, 101);
    }
}
