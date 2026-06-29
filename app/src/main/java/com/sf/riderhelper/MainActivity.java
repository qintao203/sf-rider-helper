package com.sf.riderhelper;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setText("顺丰抢单助手 v9.9.9");
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundColor(0xFF0F0F1A);
        tv.setTextSize(20);
        tv.setGravity(17);
        setContentView(tv);
    }
}