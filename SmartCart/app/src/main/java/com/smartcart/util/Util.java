package com.smartcart.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.smartcart.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small helpers for formatting, colours and toolbars. */
public final class Util {
    private Util() {}
    public static final long DAY = 24L * 60 * 60 * 1000;

    public static String money(double v) { return "\u20B9" + String.format(Locale.US, "%,.0f", v); }
    public static String date(long t) { return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(t)); }
    public static String dateTime(long t) { return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(t)); }
    public static int daysSince(long t) { return (int) ((System.currentTimeMillis() - t) / DAY); }
    public static long daysAgo(int d) { return System.currentTimeMillis() - d * DAY; }
    public static void toast(Context c, String m) { Toast.makeText(c, m, Toast.LENGTH_LONG).show(); }
    public static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    /** Sets up the shared toolbar (id: toolbar) with a title and a back arrow. */
    public static void toolbar(AppCompatActivity a, String title, boolean back) {
        Toolbar t = a.findViewById(R.id.toolbar);
        a.setSupportActionBar(t);
        a.getSupportActionBar().setTitle(title);
        if (back) {
            t.setNavigationIcon(R.drawable.ic_back);
            t.setNavigationOnClickListener(v -> a.finish());
        }
    }

    public static int color(String key) {
        switch (key) {
            case "Placed": case "Pickup Scheduled": return Color.parseColor("#2563EB");
            case "Shipped": case "Under Review": case "Medium": return Color.parseColor("#D97706");
            case "Delivered": case "Approved": case "Low": return Color.parseColor("#16A34A");
            case "Refund Processed": return Color.parseColor("#0D9488");
            case "Rejected": case "High": return Color.parseColor("#DC2626");
            case "Pending": return Color.parseColor("#6B7280");
            case "Gold": return Color.parseColor("#CA8A04");
            case "Silver": return Color.parseColor("#6B7280");
            case "Bronze": return Color.parseColor("#B45309");
            default: return Color.parseColor("#4F46E5");
        }
    }

    /** Rounded coloured pill used for statuses, risk levels and tiers. */
    public static void badge(TextView tv, String text, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(tv.getContext(), 14));
        tv.setBackground(g);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setPadding(dp(tv.getContext(), 10), dp(tv.getContext(), 3), dp(tv.getContext(), 10), dp(tv.getContext(), 3));
    }

    /** Product "image": an emoji on a soft coloured rounded tile (no network or image assets needed). */
    public static void emoji(TextView tv, String emoji, String category) {
        int[] cols = {0xFFE0E7FF, 0xFFFCE7F3, 0xFFDCFCE7, 0xFFFEF3C7, 0xFFE0F2FE, 0xFFFFEDD5};
        GradientDrawable g = new GradientDrawable();
        g.setColor(cols[Math.abs(category.hashCode()) % cols.length]);
        g.setCornerRadius(dp(tv.getContext(), 16));
        tv.setBackground(g);
        tv.setText(emoji);
        tv.setGravity(Gravity.CENTER);
    }

    public static String categoryEmoji(String cat) {
        switch (cat) {
            case "Electronics": return "\uD83D\uDD0C";
            case "Fashion": return "\uD83D\uDC57";
            case "Home & Kitchen": return "\uD83C\uDFE0";
            case "Books": return "\uD83D\uDCDA";
            case "Sports": return "\u26BD";
            default: return "\uD83D\uDED2";
        }
    }
}
