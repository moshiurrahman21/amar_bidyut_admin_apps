package com.iqra.amarbidyutadmin;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OverviewFragment extends Fragment {

    private static final String ARG_ROLE     = "arg_role";
    private static final String ARG_SUB_ID   = "arg_sub_id";
    private static final String ARG_SUB_NAME = "arg_sub_name";

    // একই get_feeders.php থেকে সব ডাটা আনা হবে
    static final String BASE          = "https://dainikbhorerbarta.com/bidyut_super_admin/";
    static final String FEEDERS_URL   = BASE + "get_feeders.php";
    static final String OVERVIEW_URL  = BASE + "get_overview.php";

    TextView tvStatOn, tvStatOff, tvStatComplaints, tvStatSolved, tvCurrentTime;
    LinearLayout llFeederGrid;

    String role, subId, subName;

    RequestQueue requestQueue;

    // ── Live clock ──
    Handler clockHandler = new Handler(Looper.getMainLooper());
    Runnable clockRunnable;

    // ── Auto refresh ──
    Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    Runnable autoRefreshRunnable;

    boolean isLoading = false;

    public static OverviewFragment newInstance(String role, String subId, String subName) {
        OverviewFragment f = new OverviewFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ROLE, role);
        b.putString(ARG_SUB_ID, subId);
        b.putString(ARG_SUB_NAME, subName);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_overview, container, false);

        tvStatOn         = v.findViewById(R.id.tvStatOn);
        tvStatOff        = v.findViewById(R.id.tvStatOff);
        tvStatComplaints = v.findViewById(R.id.tvStatComplaints);
        tvStatSolved     = v.findViewById(R.id.tvStatSolved);
        tvCurrentTime    = v.findViewById(R.id.tvCurrentTime);
        llFeederGrid     = v.findViewById(R.id.llFeederGrid);

        if (getArguments() != null) {
            role   = getArguments().getString(ARG_ROLE, "sba");
            subId  = getArguments().getString(ARG_SUB_ID, "");
            subName = getArguments().getString(ARG_SUB_NAME, "");
        }

        requestQueue = Volley.newRequestQueue(requireContext());

        startClock();
        loadFeederGrid();
        loadOverviewStats();
        startAutoRefresh();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clockHandler.removeCallbacks(clockRunnable);
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    // ══════════════════════════════════════════
    // ── LIVE CLOCK ──
    // ══════════════════════════════════════════
    private void startClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvCurrentTime != null && isAdded()) {
                    tvCurrentTime.setText("📋 বর্তমান সময়: " + nowBengaliTime());
                }
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private String nowBengaliTime() {
        Calendar c = Calendar.getInstance();
        String[] months = {"জানুয়ারি","ফেব্রুয়ারি","মার্চ","এপ্রিল","মে","জুন","জুলাই","আগস্ট","সেপ্টেম্বর","অক্টোবর","নভেম্বর","ডিসেম্বর"};
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return toBn(c.get(Calendar.DAY_OF_MONTH)) + " " + months[c.get(Calendar.MONTH)] + " " +
                toBn(c.get(Calendar.YEAR)) + "  " + tf.format(c.getTime());
    }

    // ══════════════════════════════════════════
    // ── AUTO REFRESH (৩০ সেকেন্ড) ──
    // ══════════════════════════════════════════
    private void startAutoRefresh() {
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && !isLoading) {
                    loadFeederGrid();
                    loadOverviewStats();
                }
                autoRefreshHandler.postDelayed(this, 30000);
            }
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);
    }

    // ══════════════════════════════════════════
    // ── FEEDER COLOR GRID ──
    // get_feeders.php থেকেই ডাটা আনা হচ্ছে
    // ══════════════════════════════════════════
    private void loadFeederGrid() {
        if (isLoading) return;
        isLoading = true;

        String url = FEEDERS_URL;
        if ("sba".equals(role) && subId != null && !subId.isEmpty()) {
            url += "?sub_id=" + subId;
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    isLoading = false;
                    if (!isAdded() || llFeederGrid == null) return;
                    buildGrid(response);
                    updateStatCounts(response);
                },
                error -> {
                    isLoading = false;
                });
        req.setShouldCache(false);
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 2, 1.5f));
        requestQueue.add(req);
    }

    // ── সাবস্টেশন অনুযায়ী গ্রুপ করে grid বানানো ──
    private void buildGrid(JSONArray response) {
        llFeederGrid.removeAllViews();

        // সাবস্টেশন → ফিডার তালিকা (LinkedHashMap — ক্রম বজায় থাকবে)
        LinkedHashMap<String, List<JSONObject>> subMap = new LinkedHashMap<>();

        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject o = response.getJSONObject(i);
                String sName = o.optString("sub_name", "অন্যান্য");
                if (!subMap.containsKey(sName)) {
                    subMap.put(sName, new ArrayList<>());
                }
                subMap.get(sName).add(o);
            } catch (JSONException ignored) {}
        }

        // প্রতিটা সাবস্টেশনের জন্য section
        for (Map.Entry<String, List<JSONObject>> entry : subMap.entrySet()) {
            addSubstationSection(entry.getKey(), entry.getValue());
        }
    }

    private void addSubstationSection(String subName, List<JSONObject> feeders) {
        // ── সাবস্টেশন হেডার ──
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerRow.setBackgroundResource(R.drawable.bg_sub_header);
        headerRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, dp(8), 0, dp(6));
        headerRow.setLayoutParams(headerParams);

        // অন/অফ count
        int on = 0, off = 0, scheduled = 0;
        for (JSONObject f : feeders) {
            String status = f.optString("status", "on");
            String autoOff = f.optString("auto_off_at", "");
            boolean hasAutoOff = !autoOff.isEmpty() && !autoOff.equals("null");
            if (hasAutoOff) scheduled++;
            else if ("on".equals(status)) on++;
            else off++;
        }

        TextView tvSubName = new TextView(requireContext());
        tvSubName.setText("🏭 " + subName + " সাবস্টেশন");
        tvSubName.setTextColor(0xFF003087);
        tvSubName.setTextSize(12);
        tvSubName.setTypeface(null, Typeface.BOLD);
        tvSubName.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        headerRow.addView(tvSubName);

        // মিনি stat
        TextView tvSubStat = new TextView(requireContext());
        tvSubStat.setText("✅" + on + "  ❌" + off + (scheduled > 0 ? "  ⏳" + scheduled : ""));
        tvSubStat.setTextColor(0xFF5C6E82);
        tvSubStat.setTextSize(11);
        headerRow.addView(tvSubStat);

        llFeederGrid.addView(headerRow);

        // ── ফিডার গ্রিড (2 কলাম) ──
        LinearLayout row = null;
        for (int i = 0; i < feeders.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dp(6));
                row.setLayoutParams(rowParams);
                llFeederGrid.addView(row);
            }

            JSONObject feeder = feeders.get(i);
            String fname  = feeder.optString("name", "—");
            String status = feeder.optString("status", "on");
            String autoOff = feeder.optString("auto_off_at", "");
            String eta     = feeder.optString("eta_at", "");
            boolean hasAutoOff = !autoOff.isEmpty() && !autoOff.equals("null");

            // ── ফিডার কার্ড ──
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i % 2 == 0) cardParams.setMargins(0, 0, dp(5), 0);
            card.setLayoutParams(cardParams);

            // রং অনুযায়ী background
            if (hasAutoOff) {
                card.setBackgroundResource(R.drawable.bg_grid_soon);
            } else if ("on".equals(status)) {
                card.setBackgroundResource(R.drawable.bg_grid_on);
            } else {
                card.setBackgroundResource(R.drawable.bg_grid_off);
            }

            // ফিডারের নাম
            TextView tvName = new TextView(requireContext());
            tvName.setText(fname + " ফিডার");
            tvName.setTextSize(12);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF0F1923);
            card.addView(tvName);

            // স্ট্যাটাস লেখা
            TextView tvStatus = new TextView(requireContext());
            if (hasAutoOff) {
                tvStatus.setText("⏳ Auto OFF নির্ধারিত");
                tvStatus.setTextColor(0xFFD97706);
            } else if ("on".equals(status)) {
                tvStatus.setText("✅ চালু");
                tvStatus.setTextColor(0xFF16A34A);
            } else {
                tvStatus.setText("❌ বন্ধ");
                tvStatus.setTextColor(0xFFDC2626);
            }
            tvStatus.setTextSize(11);
            tvStatus.setTypeface(null, Typeface.BOLD);
            card.addView(tvStatus);

            // ETA দেখানো (বন্ধ থাকলে)
            if (!"on".equals(status) && !eta.isEmpty() && !eta.equals("null")) {
                TextView tvEta = new TextView(requireContext());
                tvEta.setText("⏰ " + formatShortDateTime(eta));
                tvEta.setTextColor(0xFF92400E);
                tvEta.setTextSize(10);
                card.addView(tvEta);
            }

            row.addView(card);

            // বিজোড় সংখ্যা হলে খালি view দিয়ে ব্যালেন্স করা
            if (i == feeders.size() - 1 && i % 2 == 0) {
                View empty = new View(requireContext());
                LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                ep.setMargins(dp(5), 0, 0, 0);
                empty.setLayoutParams(ep);
                row.addView(empty);
            }
        }

        // সাবস্টেশনের শেষে divider
        View divider = new View(requireContext());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dp.setMargins(0, dp(4), 0, dp(4));
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(0xFFE2E8F0);
        llFeederGrid.addView(divider);
    }

    // ══════════════════════════════════════════
    // ── STATS (Overview API থেকে অথবা grid ডাটা থেকে সরাসরি গণনা) ──
    // ══════════════════════════════════════════
    private void updateStatCounts(JSONArray feeders) {
        int on = 0, off = 0;
        for (int i = 0; i < feeders.length(); i++) {
            try {
                JSONObject o = feeders.getJSONObject(i);
                if ("on".equals(o.optString("status", "on"))) on++; else off++;
            } catch (JSONException ignored) {}
        }
        if (tvStatOn != null) tvStatOn.setText(toBn(on));
        if (tvStatOff != null) tvStatOff.setText(toBn(off));
    }

    private void loadOverviewStats() {
        String url = OVERVIEW_URL;
        if ("sba".equals(role) && subId != null && !subId.isEmpty()) {
            url += "?sub_id=" + subId;
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded() || response.length() == 0) return;
                    try {
                        JSONObject o = response.getJSONObject(0);
                        if (tvStatComplaints != null) tvStatComplaints.setText(toBn(o.optInt("complaints_new", 0)));
                        if (tvStatSolved != null)     tvStatSolved.setText(toBn(o.optInt("complaints_solved", 0)));
                    } catch (JSONException ignored) {}
                },
                error -> {});
        req.setShouldCache(false);
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 1, 1f));
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // ── HELPER ──
    // ══════════════════════════════════════════
    private String formatShortDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("null")) return "—";
        try {
            String[] parts = raw.trim().split("[ T]");
            if (parts.length < 2) return raw;
            String[] timeParts = parts[1].split(":");
            int h = Integer.parseInt(timeParts[0]);
            int m = Integer.parseInt(timeParts[1]);
            String period = h < 12 ? "সকাল" : h < 15 ? "দুপুর" : h < 18 ? "বিকাল" : h < 20 ? "সন্ধ্যা" : "রাত";
            int hr12 = h % 12;
            if (hr12 == 0) hr12 = 12;
            return period + " " + toBn(hr12) + ":" + (m < 10 ? "০" + toBn(m) : toBn(m));
        } catch (Exception e) {
            return raw;
        }
    }

    private String toBn(int num) {
        String[] bn = {"০","১","২","৩","৪","৫","৬","৭","৮","৯"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(num).toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(bn[c - '0']); else sb.append(c);
        }
        return sb.toString();
    }

    private int dp(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}