package com.sf.riderhelper;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatsActivity extends Activity {
    private ConfigManager config;
    private TextView tvGrab, tvFail, tvIncome;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F0F1A);
        root.setPadding(12, 12, 12, 12);

        // 标题 + 返回
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        Button back = new Button(this);
        back.setText("< 返回");
        back.setTextColor(0xFFCCCCCC);
        back.setTextSize(14);
        back.setBackgroundColor(0xFF333355);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("数据日志");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(title);

        root.addView(header);

        // 统计卡片
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(0xFF16213E);
        card.setPadding(12, 16, 12, 16);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 16, 0, 0);
        card.setLayoutParams(clp);

        tvGrab = makeCardItem(card, "成功", 0xFF4CAF50);
        tvFail = makeCardItem(card, "失败", 0xFFE94560);
        tvIncome = makeCardItem(card, "收入", 0xFFFFD700);
        root.addView(card);

        // 记录列表提示
        TextView empty = new TextView(this);
        empty.setText("暂无抢单记录\n启动服务抢单后自动记录");
        empty.setTextColor(0xFF444466);
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        empty.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(empty);

        setContentView(root);

        tvGrab.setText(String.valueOf(config.getStatGrabbed()));
        tvFail.setText(String.valueOf(config.getStatFailed()));
        tvIncome.setText("¥" + (config.getStatGrabbed() * 12));
    }

    private TextView makeCardItem(LinearLayout parent, String label, int color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView num = new TextView(this);
        num.setText("0");
        num.setTextColor(color);
        num.setTextSize(24);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        item.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF888888);
        lbl.setTextSize(12);
        lbl.setGravity(Gravity.CENTER);
        item.addView(lbl);

        parent.addView(item);
        return num;
    }
}
