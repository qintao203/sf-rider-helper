package com.sf.riderhelper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            TextView tv = new TextView(this);
            tv.setText("加载中...");
            tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundColor(0xFF000000);
            tv.setTextSize(18);
            tv.setGravity(17);
            setContentView(tv);
        } catch (Exception e) { e.printStackTrace(); }
        new Handler(getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1000);
    }
}