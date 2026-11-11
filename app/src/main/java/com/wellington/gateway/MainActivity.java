package com.wellington.gateway;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static MainActivity instance;

    private EditText etUrl, etToken;
    private Button btnAction;
    private TextView tvStatusBadge, tvSentCount, tvPayCount, tvConsole;
    private ScrollView consoleScrollView;

    private int sentCount = 0;
    private int payCount = 0;
    private boolean isRunning = false;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        // স্ট্যাটাস বার ডার্ক করা
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#050811"));
        }

        // ================= ROOT SCROLL CONTAINER =================
        ScrollView rootScrollView = new ScrollView(this);
        rootScrollView.setBackgroundColor(Color.parseColor("#070C18"));
        rootScrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(30));
        rootScrollView.addView(root);

        // ================= HEADER SECTION =================
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("WELLINGTON NODE");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLetterSpacing(0.08f);
        header.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Institutional Payment & SMS Gateway Engine");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setPadding(0, dp(4), 0, dp(14));
        header.addView(tvSub);

        // Status Badge (🟢 / 🔴)
        tvStatusBadge = new TextView(this);
        tvStatusBadge.setText("● SYSTEM STANDBY");
        tvStatusBadge.setTextSize(11);
        tvStatusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
        tvStatusBadge.setPadding(dp(16), dp(6), dp(16), dp(6));
        tvStatusBadge.setBackground(createBadgeDrawable("#F59E0B", "#1C1917"));
        header.addView(tvStatusBadge);

        root.addView(header);

        // ================= METRICS STATS CARDS =================
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(0, dp(24), 0, dp(20));
        statsRow.setWeightSum(2f);

        // Card 1: Outgoing SMS Counter
        LinearLayout cardSms = createStatCard("OUTGOING SMS", "0", "#00D2FF");
        tvSentCount = (TextView) cardSms.getChildAt(1);
        statsRow.addView(cardSms);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(12), ViewGroup.LayoutParams.MATCH_PARENT));
        statsRow.addView(spacer);

        // Card 2: Received Payment Counter
        LinearLayout cardPay = createStatCard("PAYMENTS CAPTURED", "0", "#00E676");
        tvPayCount = (TextView) cardPay.getChildAt(1);
        statsRow.addView(cardPay);

        root.addView(statsRow);

        // ================= CONFIGURATION CARD =================
        LinearLayout configBox = new LinearLayout(this);
        configBox.setOrientation(LinearLayout.VERTICAL);
        configBox.setPadding(dp(18), dp(18), dp(18), dp(18));
        configBox.setBackground(createCardDrawable("#0D1629", "#1E293B"));

        TextView tvCfgTitle = new TextView(this);
        tvCfgTitle.setText("SERVER CONFIGURATION");
        tvCfgTitle.setTextSize(12);
        tvCfgTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvCfgTitle.setTextColor(Color.parseColor("#00D2FF"));
        tvCfgTitle.setLetterSpacing(0.06f);
        tvCfgTitle.setPadding(0, 0, 0, dp(12));
        configBox.addView(tvCfgTitle);

        // Endpoint Input
        TextView lblUrl = createInputLabel("ENDPOINT URL");
        configBox.addView(lblUrl);
        etUrl = createStyledInput("https://wellington-info.com");
        configBox.addView(etUrl);

        // Token Input
        TextView lblToken = createInputLabel("SECRET ACCESS TOKEN");
        configBox.addView(lblToken);
        etToken = createStyledInput("WELLINGTON_MY_SECRET_KEY_9988");
        configBox.addView(etToken);

        // Action Button
        btnAction = new Button(this);
        btnAction.setText("▶  START GATEWAY ENGINE");
        btnAction.setTextColor(Color.WHITE);
        btnAction.setTextSize(14);
        btnAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnAction.setLetterSpacing(0.04f);
        btnAction.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnAction.setBackground(createButtonDrawable("#0066FF", "#00D2FF"));

        LinearLayout.LayoutParams btnParams = (LinearLayout.LayoutParams) btnAction.getLayoutParams();
        btnParams.setMargins(0, dp(14), 0, 0);
        configBox.addView(btnAction);

        root.addView(configBox);

        // ================= LIVE TERMINAL CONSOLE =================
        TextView tvConsoleTitle = new TextView(this);
        tvConsoleTitle.setText("REAL-TIME ACTIVITY CONSOLE");
        tvConsoleTitle.setTextSize(12);
        tvConsoleTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvConsoleTitle.setTextColor(Color.parseColor("#94A3B8"));
        tvConsoleTitle.setPadding(dp(4), dp(22), 0, dp(8));
        root.addView(tvConsoleTitle);

        consoleScrollView = new ScrollView(this);
        consoleScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160)));
        consoleScrollView.setBackground(createCardDrawable("#030712", "#0F172A"));
        consoleScrollView.setPadding(dp(12), dp(12), dp(12), dp(12));

        tvConsole = new TextView(this);
        tvConsole.setTextSize(11);
        tvConsole.setTypeface(Typeface.MONOSPACE);
        tvConsole.setTextColor(Color.parseColor("#38BDF8"));
        tvConsole.setLineSpacing(dp(4), 1f);
        tvConsole.setText("[SYSTEM] Wellington Gateway Node Initialized.\n[STATUS] Ready to boot service.");
        consoleScrollView.addView(tvConsole);

        root.addView(consoleScrollView);

        setContentView(rootScrollView);

        // ================= PREFERENCES & LOGIC =================
        SharedPreferences pref = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
        etUrl.setText(pref.getString("server_url", "https://wellington-info.com"));
        etToken.setText(pref.getString("secret_token", "WELLINGTON_MY_SECRET_KEY_9988"));

        requestAllPermissions();

        btnAction.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim().replaceAll("/+$", "");
            String token = etToken.getText().toString().trim();

            pref.edit()
                .putString("server_url", url)
                .putString("secret_token", token)
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);

            if (!isRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                setRunningState(true);
                addLog("[ENGINE] Service Started. Listening to bKash/Nagad & Outgoing SMS.");
                Toast.makeText(this, "Gateway Engine Started!", Toast.LENGTH_SHORT).show();
            } else {
                stopService(serviceIntent);
                setRunningState(false);
                addLog("[ENGINE] Service Stopped.");
                Toast.makeText(this, "Gateway Engine Stopped!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setRunningState(boolean running) {
        isRunning = running;
        if (running) {
            tvStatusBadge.setText("● ENGINE ONLINE & POLLING");
            tvStatusBadge.setTextColor(Color.parseColor("#00E676"));
            tvStatusBadge.setBackground(createBadgeDrawable("#00E676", "#062817"));
            btnAction.setText("⏹  STOP GATEWAY ENGINE");
            btnAction.setBackground(createButtonDrawable("#DC2626", "#EF4444"));
        } else {
            tvStatusBadge.setText("● SYSTEM STANDBY");
            tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            tvStatusBadge.setBackground(createBadgeDrawable("#F59E0B", "#1C1917"));
            btnAction.setText("▶  START GATEWAY ENGINE");
            btnAction.setBackground(createButtonDrawable("#0066FF", "#00D2FF"));
        }
    }

    // ================= LIVE LOGGING & METRICS =================
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

    // ================= DRAWABLE BUILDERS =================
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

    private GradientDrawable createBadgeDrawable(String textColor, String bgColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(dp(99));
        gd.setStroke(dp(1), Color.parseColor(textColor));
        return gd;
    }

    private RippleDrawable createButtonDrawable(String color1, String color2) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor(color1), Color.parseColor(color2)}
        );
        gd.setCornerRadius(dp(14));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), gd, null);
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
