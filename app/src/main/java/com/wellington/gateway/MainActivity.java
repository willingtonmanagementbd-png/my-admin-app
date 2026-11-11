package com.wellington.gateway;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static MainActivity instance;

    private EditText etUrl, etToken;
    private SwitchCompat vpnSwitch;
    private TextView tvSwitchTitle, tvSwitchSub, tvSentCount, tvPayCount, tvConsole;
    private LinearLayout vpnCard;
    private ScrollView consoleScrollView;

    private int sentCount = 0;
    private int payCount = 0;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#050811"));
        }

        ScrollView rootScrollView = new ScrollView(this);
        rootScrollView.setBackgroundColor(Color.parseColor("#070C18"));
        rootScrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(36), dp(20), dp(30));
        rootScrollView.addView(root);

        // HEADER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("WELLINGTON NODE");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLetterSpacing(0.08f);
        header.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Institutional Payment & SMS Gateway Engine");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setPadding(0, dp(4), 0, dp(16));
        header.addView(tvSub);

        root.addView(header);

        // VPN MASTER TOGGLE CARD
        vpnCard = new LinearLayout(this);
        vpnCard.setOrientation(LinearLayout.HORIZONTAL);
        vpnCard.setGravity(Gravity.CENTER_VERTICAL);
        vpnCard.setPadding(dp(20), dp(22), dp(20), dp(22));
        vpnCard.setBackground(createCardDrawable("#0E1626", "#1E293B"));

        LinearLayout switchMeta = new LinearLayout(this);
        switchMeta.setOrientation(LinearLayout.VERTICAL);
        switchMeta.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvSwitchTitle = new TextView(this);
        tvSwitchTitle.setText("GATEWAY DISCONNECTED 🔴");
        tvSwitchTitle.setTextSize(14);
        tvSwitchTitle.setTextColor(Color.parseColor("#94A3B8"));
        tvSwitchTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvSwitchTitle.setLetterSpacing(0.06f);
        switchMeta.addView(tvSwitchTitle);

        tvSwitchSub = new TextView(this);
        tvSwitchSub.setText("Tap switch to activate 24/7 background mode");
        tvSwitchSub.setTextSize(11);
        tvSwitchSub.setTextColor(Color.parseColor("#64748B"));
        tvSwitchSub.setPadding(0, dp(4), 0, 0);
        switchMeta.addView(tvSwitchSub);

        vpnCard.addView(switchMeta);

        vpnSwitch = new SwitchCompat(this);
        vpnSwitch.setShowText(false);
        setSwitchColors(false);
        vpnCard.addView(vpnSwitch);

        root.addView(vpnCard);

        // METRICS STATS
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(0, dp(20), 0, dp(18));
        statsRow.setWeightSum(2f);

        LinearLayout cardSms = createStatCard("OUTGOING SMS", "0", "#00D2FF");
        tvSentCount = (TextView) cardSms.getChildAt(1);
        statsRow.addView(cardSms);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(12), ViewGroup.LayoutParams.MATCH_PARENT));
        statsRow.addView(spacer);

        LinearLayout cardPay = createStatCard("PAYMENTS CAPTURED", "0", "#00E676");
        tvPayCount = (TextView) cardPay.getChildAt(1);
        statsRow.addView(cardPay);

        root.addView(statsRow);

        // CONFIGURATION
        LinearLayout configBox = new LinearLayout(this);
        configBox.setOrientation(LinearLayout.VERTICAL);
        configBox.setPadding(dp(18), dp(18), dp(18), dp(18));
        configBox.setBackground(createCardDrawable("#0D1629", "#1E293B"));

        TextView tvCfgTitle = new TextView(this);
        tvCfgTitle.setText("SERVER CONFIGURATION");
        tvCfgTitle.setTextSize(11);
        tvCfgTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvCfgTitle.setTextColor(Color.parseColor("#00D2FF"));
        tvCfgTitle.setLetterSpacing(0.06f);
        tvCfgTitle.setPadding(0, 0, 0, dp(10));
        configBox.addView(tvCfgTitle);

        configBox.addView(createInputLabel("ENDPOINT URL"));
        etUrl = createStyledInput("https://wellington-info.com");
        configBox.addView(etUrl);

        configBox.addView(createInputLabel("SECRET ACCESS TOKEN"));
        etToken = createStyledInput("WELLINGTON_MY_SECRET_KEY_9988");
        configBox.addView(etToken);

        root.addView(configBox);

        // CONSOLE
        TextView tvConsoleTitle = new TextView(this);
        tvConsoleTitle.setText("REAL-TIME ACTIVITY CONSOLE");
        tvConsoleTitle.setTextSize(11);
        tvConsoleTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvConsoleTitle.setTextColor(Color.parseColor("#94A3B8"));
        tvConsoleTitle.setPadding(dp(4), dp(20), 0, dp(8));
        root.addView(tvConsoleTitle);

        consoleScrollView = new ScrollView(this);
        consoleScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));
        consoleScrollView.setBackground(createCardDrawable("#030712", "#0F172A"));
        consoleScrollView.setPadding(dp(12), dp(12), dp(12), dp(12));

        tvConsole = new TextView(this);
        tvConsole.setTextSize(10.5f);
        tvConsole.setTypeface(Typeface.MONOSPACE);
        tvConsole.setTextColor(Color.parseColor("#38BDF8"));
        tvConsole.setLineSpacing(dp(4), 1f);
        tvConsole.setText("[SYSTEM] Node ready.\n[PERSISTENCE] Background keep-alive enabled.");
        consoleScrollView.addView(tvConsole);

        root.addView(consoleScrollView);

        setContentView(rootScrollView);

        // PREFERENCES
        SharedPreferences pref = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
        etUrl.setText(pref.getString("server_url", "https://wellington-info.com"));
        etToken.setText(pref.getString("secret_token", "WELLINGTON_MY_SECRET_KEY_9988"));

        requestAllPermissions();

        // টগল সুইচ লজিক
        vpnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String url = etUrl.getText().toString().trim().replaceAll("/+$", "");
            String token = etToken.getText().toString().trim();

            pref.edit()
                .putString("server_url", url)
                .putString("secret_token", token)
                .putBoolean("service_running", isChecked)
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                updateVpnUi(true);
                addLog("[VPN-ENGINE] Master Switch ON. Engine running.");
            } else {
                stopService(serviceIntent);
                updateVpnUi(false);
                addLog("[VPN-ENGINE] Master Switch OFF. Engine stopped.");
            }
        });

        // যদি আগে থেকে ON থাকে, তাহলে সার্ভিসটি অটোমেটিক চালু করে দেওয়া
        boolean isAlreadyRunning = pref.getBoolean("service_running", false);
        if (isAlreadyRunning) {
            vpnSwitch.setChecked(true);
            Intent serviceIntent = new Intent(this, GatewayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void updateVpnUi(boolean isConnected) {
        setSwitchColors(isConnected);
        if (isConnected) {
            tvSwitchTitle.setText("GATEWAY CONNECTED 🟢");
            tvSwitchTitle.setTextColor(Color.parseColor("#00E676"));
            tvSwitchSub.setText("Active 24/7 • Will not stop on app close");
            vpnCard.setBackground(createCardDrawable("#08221B", "#00E676"));
        } else {
            tvSwitchTitle.setText("GATEWAY DISCONNECTED 🔴");
            tvSwitchTitle.setTextColor(Color.parseColor("#94A3B8"));
            tvSwitchSub.setText("Tap switch to activate 24/7 background mode");
            vpnCard.setBackground(createCardDrawable("#0E1626", "#1E293B"));
        }
    }

    private void setSwitchColors(boolean active) {
        int thumbColor = active ? Color.parseColor("#00E676") : Color.parseColor("#64748B");
        int trackColor = active ? Color.parseColor("#0B3D2E") : Color.parseColor("#1E293B");

        vpnSwitch.setThumbTintList(ColorStateList.valueOf(thumbColor));
        vpnSwitch.setTrackTintList(ColorStateList.valueOf(trackColor));
    }

    public static void log(String message) {
        if (instance != null) {
            new Handler(Looper.getMainLooper()).post(() -> instance.addLog(message));
        }
    }

    public static void countSent() {
        if (instance != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                instance.sentCount++;
                instance.tvSentCount.setText(String.valueOf(instance.sentCount));
            });
        }
    }

    public static void countPayment() {
        if (instance != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                instance.payCount++;
                instance.tvPayCount.setText(String.valueOf(instance.payCount));
            });
        }
    }

    private void addLog(String text) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvConsole.append("\n[" + time + "] " + text);
        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private LinearLayout createStatCard(String label, String value, String accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(createCardDrawable("#0D1629", "#1E293B"));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(9);
        tvLabel.setTextColor(Color.parseColor("#64748B"));
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setLetterSpacing(0.06f);
        card.addView(tvLabel);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(24);
        tvVal.setTextColor(Color.parseColor(accentColor));
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setPadding(0, dp(4), 0, 0);
        card.addView(tvVal);

        return card;
    }

    private TextView createInputLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setLetterSpacing(0.05f);
        tv.setPadding(0, dp(8), 0, dp(5));
        return tv;
    }

    private EditText createStyledInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#475569"));
        et.setTextColor(Color.WHITE);
        et.setTextSize(13);
        et.setTypeface(Typeface.MONOSPACE);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setBackground(createCardDrawable("#050A14", "#1E293B"));
        return et;
    }

    private GradientDrawable createCardDrawable(String bgColor, String strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1.5f), Color.parseColor(strokeColor));
        return gd;
    }

    private int dp(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.SEND_SMS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 101);
    }
}
