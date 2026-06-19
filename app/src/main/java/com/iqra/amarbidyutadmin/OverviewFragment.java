package com.iqra.amarbidyutadmin;

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
import java.util.Calendar;
import java.util.Locale;

public class OverviewFragment extends Fragment {

    private static final String ARG_ROLE    = "arg_role";
    private static final String ARG_SUB_ID  = "arg_sub_id";
    private static final String ARG_SUB_NAME = "arg_sub_name";

    TextView tvStatOn, tvStatOff, tvStatComplaints, tvStatSolved, tvCurrentTime;
    LinearLayout llSubSummary;

    String role, subId, subName;

    Handler clockHandler = new Handler(Looper.getMainLooper());
    Runnable clockRunnable;

    RequestQueue requestQueue;

    // ── API URL (পরে আপনার সার্ভারের সাথে মিলিয়ে নিন) ──
    static final String OVERVIEW_URL = "https://dainikbhorerbarta.com/bidyut_super_admin/get_overview.php";

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
        llSubSummary     = v.findViewById(R.id.llSubSummary);

        if (getArguments() != null) {
            role    = getArguments().getString(ARG_ROLE, "sba");
            subId   = getArguments().getString(ARG_SUB_ID, "");
            subName = getArguments().getString(ARG_SUB_NAME, "");
        }

        requestQueue = Volley.newRequestQueue(getContext());

        startClock();
        loadOverviewData();

        return v;
    }

    // ══════════════════════════════════════════
    // ── লাইভ সময় (প্রতি সেকেন্ডে আপডেট) ──
    // ══════════════════════════════════════════
    private void startClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                tvCurrentTime.setText("📋 বর্তমান সময়: " + nowBengaliTime());
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private String nowBengaliTime() {
        Calendar c = Calendar.getInstance();
        String[] months = {"জানুয়ারি","ফেব্রুয়ারি","মার্চ","এপ্রিল","মে","জুন","জুলাই","আগস্ট","সেপ্টেম্বর","অক্টোবর","নভেম্বর","ডিসেম্বর"};
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return day + " " + months[month] + " " + year + " " + tf.format(c.getTime());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clockHandler.removeCallbacks(clockRunnable);
    }

    // ══════════════════════════════════════════
    // ── সারসংক্ষেপ ডাটা লোড ──
    // role==admin হলে sub_id খালি পাঠাবে (সব সাবস্টেশনের ডাটা),
    // role==sba হলে নিজের sub_id পাঠাবে (শুধু তার সাবস্টেশন)
    // ══════════════════════════════════════════
    private void loadOverviewData() {

        String url = OVERVIEW_URL;
        if ("sba".equals(role) && subId != null && !subId.isEmpty()) {
            url += "?sub_id=" + subId;
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    // প্রত্যাশিত format:
                    // [{"total_on":9,"total_off":3,"complaints_new":7,"complaints_solved":5,
                    //   "substations":[{"name":"গোয়ালচামট","on":3,"off":1}, ...]}]
                    try {
                        if (response.length() > 0) {
                            JSONObject o = response.getJSONObject(0);

                            tvStatOn.setText(toBn(o.optInt("total_on", 0)));
                            tvStatOff.setText(toBn(o.optInt("total_off", 0)));
                            tvStatComplaints.setText(toBn(o.optInt("complaints_new", 0)));
                            tvStatSolved.setText(toBn(o.optInt("complaints_solved", 0)));

                            renderSubSummary(o.optJSONArray("substations"));
                        }
                    } catch (JSONException e) {
                        // চুপচাপ ignore — UI তে ডিফল্ট মান থাকবে
                    }
                },
                error -> {
                    // নেটওয়ার্ক সমস্যা হলে ডিফল্ট মান (XML এ যা আছে) থেকে যাবে
                });

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ── সাবস্টেশন-ভিত্তিক চালু/বন্ধ সারসংক্ষেপ দেখানো (শুধু admin এর জন্য বেশি প্রয়োজন) ──
    private void renderSubSummary(@Nullable JSONArray subs) {
        if (llSubSummary == null) return;
        llSubSummary.removeAllViews();

        if (subs == null || getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < subs.length(); i++) {
            try {
                JSONObject s = subs.getJSONObject(i);
                View row = inflater.inflate(R.layout.item_sub_summary, llSubSummary, false);

                TextView tvName = row.findViewById(R.id.tvSubName);
                TextView tvOn   = row.findViewById(R.id.tvSubOnCount);
                TextView tvOff  = row.findViewById(R.id.tvSubOffCount);

                tvName.setText(s.optString("name", "—"));
                tvOn.setText(toBn(s.optInt("on", 0)) + " চালু");
                tvOff.setText(toBn(s.optInt("off", 0)) + " বন্ধ");

                llSubSummary.addView(row);
            } catch (JSONException ignored) {}
        }
    }

    // ── ইংরেজি সংখ্যাকে বাংলা সংখ্যায় রূপান্তর ──
    private String toBn(int num) {
        String[] bn = {"০","১","২","৩","৪","৫","৬","৭","৮","৯"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(num).toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(bn[c - '0']);
            else sb.append(c);
        }
        return sb.toString();
    }
}
