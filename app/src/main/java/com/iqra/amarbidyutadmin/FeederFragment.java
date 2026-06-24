package com.iqra.amarbidyutadmin;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class FeederFragment extends Fragment {

    private static final String ARG_ROLE     = "arg_role";
    private static final String ARG_SUB_ID   = "arg_sub_id";
    private static final String ARG_SUB_NAME = "arg_sub_name";
    private static final String ARG_NAME     = "arg_name";

    static final String BASE = "https://dainikbhorerbarta.com/bidyut_super_admin/";
    static final String GET_FEEDERS_URL   = BASE + "get_feeders.php";
    static final String TOGGLE_FEEDER_URL = BASE + "toggle_feeder.php";
    static final String MODIFY_FEEDER_URL = BASE + "modify_feeder.php";

    LinearLayout llFeederList;
    TextView tvNoFeeders, tvFeederOnCount, tvFeederOffCount;
    SwipeRefreshLayout swipeRefresh;

    String role, subId, subName, staffName;

    RequestQueue requestQueue;
    Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    Runnable autoRefreshRunnable;

    public static FeederFragment newInstance(String role, String subId, String subName, String name) {
        FeederFragment f = new FeederFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ROLE, role);
        b.putString(ARG_SUB_ID, subId);
        b.putString(ARG_SUB_NAME, subName);
        b.putString(ARG_NAME, name);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_feeder, container, false);

        llFeederList     = v.findViewById(R.id.llFeederList);
        tvNoFeeders      = v.findViewById(R.id.tvNoFeeders);
        tvFeederOnCount  = v.findViewById(R.id.tvFeederOnCount);
        tvFeederOffCount = v.findViewById(R.id.tvFeederOffCount);
        swipeRefresh     = v.findViewById(R.id.swipeRefresh);

        if (getArguments() != null) {
            role      = getArguments().getString(ARG_ROLE, "sba");
            subId     = getArguments().getString(ARG_SUB_ID, "");
            subName   = getArguments().getString(ARG_SUB_NAME, "");
            staffName = getArguments().getString(ARG_NAME, "");
        }

        requestQueue = Volley.newRequestQueue(requireContext());

        swipeRefresh.setOnRefreshListener(this::loadFeeders);

        loadFeeders();

        // ── প্রতি ৩০ সেকেন্ডে নিজে থেকে রিফ্রেশ (auto-off হলে যেন দেখা যায়) ──
        autoRefreshRunnable = () -> {
            loadFeeders();
            autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    // ══════════════════════════════════════════
    // ── ফিডার লিস্ট লোড ──
    // ══════════════════════════════════════════
    private void loadFeeders() {
        String url = GET_FEEDERS_URL;
        if ("sba".equals(role) && subId != null && !subId.isEmpty()) {
            url += "?sub_id=" + subId;
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    llFeederList.removeAllViews();

                    if (response.length() == 0) {
                        tvNoFeeders.setVisibility(View.VISIBLE);
                        tvFeederOnCount.setText("০");
                        tvFeederOffCount.setText("০");
                        return;
                    }
                    tvNoFeeders.setVisibility(View.GONE);

                    int onCount = 0, offCount = 0;
                    String lastSub = "";

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            String subN = o.optString("sub_name", "");

                            // ── Admin দেখলে সাবস্টেশন অনুযায়ী section header ──
                            if ("admin".equals(role) && !subN.equals(lastSub)) {
                                addSectionHeader(subN);
                                lastSub = subN;
                            }

                            boolean isOn = "on".equals(o.optString("status", "on"));
                            if (isOn) onCount++; else offCount++;

                            addFeederRow(o);

                        } catch (JSONException ignored) {}
                    }

                    tvFeederOnCount.setText(toBn(onCount));
                    tvFeederOffCount.setText(toBn(offCount));
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "⚠️ ফিডার লোড করতে সমস্যা হয়েছে"+error.toString(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "loadFeeders2: "+error.getMessage());
                });
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void addSectionHeader(String subName) {
        TextView header = new TextView(requireContext());
        header.setText(subName + " সাবস্টেশন");
        header.setTextColor(0xFF5C6E82);
        header.setTextSize(11);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(2, 18, 2, 8);
        llFeederList.addView(header);
    }

    // ══════════════════════════════════════════
    // ── একটা ফিডারের সারি বানানো ──
    // ══════════════════════════════════════════
    private void addFeederRow(JSONObject o) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_feeder, llFeederList, false);

        TextView tvName     = item.findViewById(R.id.tvFeederName);
        TextView tvSub      = item.findViewById(R.id.tvFeederSub);
        TextView tvBadge    = item.findViewById(R.id.tvFeederBadge);
        SwitchCompat sw     = item.findViewById(R.id.swFeeder);
        LinearLayout llTimerInfo = item.findViewById(R.id.llTimerInfo);
        TextView tvOffReason   = item.findViewById(R.id.tvOffReason);
        TextView tvAutoOffInfo = item.findViewById(R.id.tvAutoOffInfo);
        TextView tvEtaInfo     = item.findViewById(R.id.tvEtaInfo);
        TextView tvLastUpdated = item.findViewById(R.id.tvLastUpdated);
        TextView btnModify     = item.findViewById(R.id.btnModifyFeeder);

        final int feederId   = o.optInt("id", 0);
        final String name    = o.optString("name", "—");
        final String subN    = o.optString("sub_name", "");
        final String status  = o.optString("status", "on");
        final String offReason = o.optString("off_reason", "");
        final String offMw     = o.optString("off_mw", "");
        final String autoOffAt = o.optString("auto_off_at", "");
        final String etaAt     = o.optString("eta_at", "");
        final String lastUpdated = o.optString("last_updated", "");
        final String updatedBy   = o.optString("updated_by", "");

        boolean isOn = "on".equals(status);
        boolean hasScheduledOff = isOn && autoOffAt != null && !autoOffAt.isEmpty() && !autoOffAt.equals("null");

        tvName.setText(name + " ফিডার");
        tvSub.setText(subN + " সাবস্টেশন");

        // ── টগল ও ব্যাজ ──
        sw.setOnCheckedChangeListener(null); // আগের listener সরানো (recycling এর কারণে দরকার)
        sw.setChecked(isOn && !hasScheduledOff);

        if (isOn && !hasScheduledOff) {
            tvBadge.setText("চালু");
            tvBadge.setTextColor(0xFF16A34A);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_on);
        } else if (hasScheduledOff) {
            tvBadge.setText("OFF নির্ধারিত");
            tvBadge.setTextColor(0xFFD97706);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_off);
        } else {
            tvBadge.setText("বন্ধ");
            tvBadge.setTextColor(0xFFDC2626);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_off);
        }

        // ── টাইমার তথ্য বক্স (off হলে বা scheduled হলে দেখাবে) ──
        boolean showTimerBox = !isOn || hasScheduledOff;
        llTimerInfo.setVisibility(showTimerBox ? View.VISIBLE : View.GONE);

        if (showTimerBox) {
            if (offReason != null && !offReason.isEmpty() && !offReason.equals("null")) {
                tvOffReason.setText("🔧 কারণ: " + offReason + (offMw != null && !offMw.isEmpty() && !offMw.equals("null") && !offMw.equals("0") ? " · " + offMw + " মেগাওয়াট" : ""));
                tvOffReason.setVisibility(View.VISIBLE);
            } else {
                tvOffReason.setVisibility(View.GONE);
            }

            if (hasScheduledOff) {
                tvAutoOffInfo.setText("⏳ Auto OFF: " + formatDbDateTime(autoOffAt));
                tvAutoOffInfo.setVisibility(View.VISIBLE);
            } else {
                tvAutoOffInfo.setVisibility(View.GONE);
            }

            if (etaAt != null && !etaAt.isEmpty() && !etaAt.equals("null")) {
                tvEtaInfo.setText("⏰ সম্ভাব্য চালু: " + formatDbDateTime(etaAt));
                tvEtaInfo.setVisibility(View.VISIBLE);
            } else {
                tvEtaInfo.setVisibility(View.GONE);
            }

            if (lastUpdated != null && !lastUpdated.isEmpty() && !lastUpdated.equals("null")) {
                tvLastUpdated.setText("🕐 সর্বশেষ আপডেট: " + formatDbDateTime(lastUpdated) + (updatedBy != null && !updatedBy.isEmpty() ? " · " + updatedBy : ""));
                tvLastUpdated.setVisibility(View.VISIBLE);
            } else {
                tvLastUpdated.setVisibility(View.GONE);
            }

            btnModify.setOnClickListener(v -> openModifySheet(feederId, name, autoOffAt, etaAt));
        }

        // ── টগল ক্লিক ──
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // ── চালু করা ──
                turnOnFeeder(feederId, name);
            } else {
                // ── বন্ধ করার ফর্ম খোলা ──
                sw.setOnCheckedChangeListener(null);
                sw.setChecked(true); // ফর্ম জমা না দেওয়া পর্যন্ত UI আগের অবস্থায় রাখা
                openOffSheet(feederId, name);
            }
        });

        llFeederList.addView(item);
    }

    // ══════════════════════════════════════════
    // ── ফিডার চালু করা ──
    // ══════════════════════════════════════════
    private void turnOnFeeder(int feederId, String name) {
        JSONObject params = new JSONObject();
        try {
            params.put("feeder_id", feederId);
            params.put("action", "on");
            params.put("updated_by", staffName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, TOGGLE_FEEDER_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(requireContext(), "✓ " + name + " ফিডার চালু। গ্রাহকরা notify হয়েছেন।", Toast.LENGTH_SHORT).show();
                        loadFeeders();
                    } else {
                        Toast.makeText(requireContext(), "⚠️ সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                        loadFeeders();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(), "⚠️ নেটওয়ার্ক সমস্যা", Toast.LENGTH_SHORT).show();
                    loadFeeders();
                }) {
            @Override
            public String getBodyContentType() { return "application/json; charset=utf-8"; }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // ── ফিডার বন্ধ করার বটম শীট ──
    // ══════════════════════════════════════════
    Calendar offAutoCal = null;
    int offAutoHour = -1, offAutoMinute = -1;
    Calendar offEtaCal = null;
    int offEtaHour = -1, offEtaMinute = -1;

    private void openOffSheet(int feederId, String feederName) {

        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_off_feeder, null);

        TextView tvTitle = sheetView.findViewById(R.id.tvOffSheetTitle);
        Spinner spReason  = sheetView.findViewById(R.id.spOffReason);
        EditText etMw     = sheetView.findViewById(R.id.etOffMw);
        LinearLayout pickAutoOffDate = sheetView.findViewById(R.id.pickAutoOffDate);
        LinearLayout pickAutoOffTime = sheetView.findViewById(R.id.pickAutoOffTime);
        LinearLayout pickEtaDate = sheetView.findViewById(R.id.pickEtaDate);
        LinearLayout pickEtaTime = sheetView.findViewById(R.id.pickEtaTime);
        TextView tvAutoOffDateVal = sheetView.findViewById(R.id.tvAutoOffDateVal);
        TextView tvAutoOffTimeVal = sheetView.findViewById(R.id.tvAutoOffTimeVal);
        TextView tvEtaDateVal = sheetView.findViewById(R.id.tvEtaDateVal);
        TextView tvEtaTimeVal = sheetView.findViewById(R.id.tvEtaTimeVal);
        Button btnApply = sheetView.findViewById(R.id.btnApplyOff);

        tvTitle.setText("⚙️ " + feederName + " ফিডার বন্ধ করুন");

        String[] reasons = {"লোডশেডিং", "যান্ত্রিক ত্রুটি", "লাইনের কাজ চলছে", "ঝড়-বৃষ্টি", "অন্যান্য"};
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons);
        spReason.setAdapter(reasonAdapter);

        offAutoCal = null; offAutoHour = -1; offAutoMinute = -1;
        offEtaCal = null; offEtaHour = -1; offEtaMinute = -1;

        pickAutoOffDate.setOnClickListener(v -> {
            Calendar c = offAutoCal != null ? offAutoCal : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                if (offAutoCal == null) offAutoCal = Calendar.getInstance();
                offAutoCal.set(Calendar.YEAR, y); offAutoCal.set(Calendar.MONTH, m); offAutoCal.set(Calendar.DAY_OF_MONTH, d);
                tvAutoOffDateVal.setText(bengaliDate(d, m, y));
                tvAutoOffDateVal.setTextColor(0xFF0F1923);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        pickAutoOffTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int h = offAutoHour >= 0 ? offAutoHour : c.get(Calendar.HOUR_OF_DAY);
            int m = offAutoMinute >= 0 ? offAutoMinute : c.get(Calendar.MINUTE);
            new TimePickerDialog(requireContext(), (tp, hh, mm) -> {
                offAutoHour = hh; offAutoMinute = mm;
                tvAutoOffTimeVal.setText(bengaliTime(hh, mm));
                tvAutoOffTimeVal.setTextColor(0xFF0F1923);
            }, h, m, false).show();
        });

        pickEtaDate.setOnClickListener(v -> {
            Calendar c = offEtaCal != null ? offEtaCal : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                if (offEtaCal == null) offEtaCal = Calendar.getInstance();
                offEtaCal.set(Calendar.YEAR, y); offEtaCal.set(Calendar.MONTH, m); offEtaCal.set(Calendar.DAY_OF_MONTH, d);
                tvEtaDateVal.setText(bengaliDate(d, m, y));
                tvEtaDateVal.setTextColor(0xFF0F1923);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        pickEtaTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int h = offEtaHour >= 0 ? offEtaHour : c.get(Calendar.HOUR_OF_DAY);
            int m = offEtaMinute >= 0 ? offEtaMinute : c.get(Calendar.MINUTE);
            new TimePickerDialog(requireContext(), (tp, hh, mm) -> {
                offEtaHour = hh; offEtaMinute = mm;
                tvEtaTimeVal.setText(bengaliTime(hh, mm));
                tvEtaTimeVal.setTextColor(0xFF0F1923);
            }, h, m, false).show();
        });

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        dialog.setOnDismissListener(d -> loadFeeders()); // বন্ধ না করলেও UI ঠিক করার জন্য রিফ্রেশ
        dialog.show();

        btnApply.setOnClickListener(v -> {
            String reason = spReason.getSelectedItem().toString();
            String mw = etMw.getText().toString().trim();

            String autoOffAt = (offAutoCal != null && offAutoHour >= 0) ? buildDateTime(offAutoCal, offAutoHour, offAutoMinute) : "";
            String etaAt = (offEtaCal != null && offEtaHour >= 0) ? buildDateTime(offEtaCal, offEtaHour, offEtaMinute) : "";

            applyOff(feederId, reason, mw, autoOffAt, etaAt, dialog);
        });
    }

    private void applyOff(int feederId, String reason, String mw, String autoOffAt, String etaAt, BottomSheetDialog dialog) {
        JSONObject params = new JSONObject();
        try {
            params.put("feeder_id", feederId);
            params.put("action", "off");
            params.put("reason", reason);
            params.put("mw", mw);
            params.put("auto_off_at", autoOffAt);
            params.put("eta_at", etaAt);
            params.put("updated_by", staffName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, TOGGLE_FEEDER_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    boolean scheduled = response.optBoolean("scheduled", false);
                    if (success) {
                        Toast.makeText(requireContext(),
                                scheduled ? "⏰ নির্ধারিত সময়ে Auto OFF হবে। গ্রাহকরা জানতে পারবেন।"
                                          : "✓ ফিডার বন্ধ করা হয়েছে। গ্রাহকরা notify হয়েছেন।",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadFeeders();
                    } else {
                        Toast.makeText(requireContext(), "⚠️ সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(requireContext(), "⚠️ নেটওয়ার্ক সমস্যা", Toast.LENGTH_SHORT).show()) {
            @Override
            public String getBodyContentType() { return "application/json; charset=utf-8"; }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // ── তথ্য পরিবর্তন (Modify) বটম শীট ──
    // ══════════════════════════════════════════
    Calendar modAutoCal = null;
    int modAutoHour = -1, modAutoMinute = -1;
    Calendar modEtaCal = null;
    int modEtaHour = -1, modEtaMinute = -1;

    private void openModifySheet(int feederId, String feederName, String currentAutoOff, String currentEta) {

        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_modify_feeder, null);

        TextView tvTitle = sheetView.findViewById(R.id.tvModSheetTitle);
        LinearLayout pickAutoOffDate = sheetView.findViewById(R.id.pickModAutoOffDate);
        LinearLayout pickAutoOffTime = sheetView.findViewById(R.id.pickModAutoOffTime);
        LinearLayout pickEtaDate = sheetView.findViewById(R.id.pickModEtaDate);
        LinearLayout pickEtaTime = sheetView.findViewById(R.id.pickModEtaTime);
        TextView tvAutoOffDateVal = sheetView.findViewById(R.id.tvModAutoOffDateVal);
        TextView tvAutoOffTimeVal = sheetView.findViewById(R.id.tvModAutoOffTimeVal);
        TextView tvEtaDateVal = sheetView.findViewById(R.id.tvModEtaDateVal);
        TextView tvEtaTimeVal = sheetView.findViewById(R.id.tvModEtaTimeVal);
        EditText etReason = sheetView.findViewById(R.id.etModReason);
        Button btnApply = sheetView.findViewById(R.id.btnApplyModify);

        tvTitle.setText("✏️ " + feederName + " ফিডার — তথ্য পরিবর্তন");

        modAutoCal = parseDbDateTime(currentAutoOff);
        modEtaCal  = parseDbDateTime(currentEta);
        if (modAutoCal != null) { modAutoHour = modAutoCal.get(Calendar.HOUR_OF_DAY); modAutoMinute = modAutoCal.get(Calendar.MINUTE); }
        if (modEtaCal != null)  { modEtaHour = modEtaCal.get(Calendar.HOUR_OF_DAY); modEtaMinute = modEtaCal.get(Calendar.MINUTE); }

        tvAutoOffDateVal.setText(modAutoCal != null ? bengaliDate(modAutoCal.get(Calendar.DAY_OF_MONTH), modAutoCal.get(Calendar.MONTH), modAutoCal.get(Calendar.YEAR)) : "তারিখ");
        tvAutoOffTimeVal.setText(modAutoHour >= 0 ? bengaliTime(modAutoHour, modAutoMinute) : "সময়");
        tvEtaDateVal.setText(modEtaCal != null ? bengaliDate(modEtaCal.get(Calendar.DAY_OF_MONTH), modEtaCal.get(Calendar.MONTH), modEtaCal.get(Calendar.YEAR)) : "তারিখ");
        tvEtaTimeVal.setText(modEtaHour >= 0 ? bengaliTime(modEtaHour, modEtaMinute) : "সময়");

        pickAutoOffDate.setOnClickListener(v -> {
            Calendar c = modAutoCal != null ? modAutoCal : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                if (modAutoCal == null) modAutoCal = Calendar.getInstance();
                modAutoCal.set(Calendar.YEAR, y); modAutoCal.set(Calendar.MONTH, m); modAutoCal.set(Calendar.DAY_OF_MONTH, d);
                tvAutoOffDateVal.setText(bengaliDate(d, m, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        pickAutoOffTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (tp, hh, mm) -> {
                modAutoHour = hh; modAutoMinute = mm;
                tvAutoOffTimeVal.setText(bengaliTime(hh, mm));
            }, Math.max(modAutoHour, 0), Math.max(modAutoMinute, 0), false).show();
        });

        pickEtaDate.setOnClickListener(v -> {
            Calendar c = modEtaCal != null ? modEtaCal : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                if (modEtaCal == null) modEtaCal = Calendar.getInstance();
                modEtaCal.set(Calendar.YEAR, y); modEtaCal.set(Calendar.MONTH, m); modEtaCal.set(Calendar.DAY_OF_MONTH, d);
                tvEtaDateVal.setText(bengaliDate(d, m, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        pickEtaTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (tp, hh, mm) -> {
                modEtaHour = hh; modEtaMinute = mm;
                tvEtaTimeVal.setText(bengaliTime(hh, mm));
            }, Math.max(modEtaHour, 0), Math.max(modEtaMinute, 0), false).show();
        });

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        dialog.show();

        btnApply.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "পরিবর্তনের কারণ লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }

            String newAutoOffAt = (modAutoCal != null && modAutoHour >= 0) ? buildDateTime(modAutoCal, modAutoHour, modAutoMinute) : "";
            String newEtaAt = (modEtaCal != null && modEtaHour >= 0) ? buildDateTime(modEtaCal, modEtaHour, modEtaMinute) : "";

            applyModify(feederId, newAutoOffAt, newEtaAt, reason, dialog);
        });
    }

    private void applyModify(int feederId, String autoOffAt, String etaAt, String reason, BottomSheetDialog dialog) {
        JSONObject params = new JSONObject();
        try {
            params.put("feeder_id", feederId);
            params.put("auto_off_at", autoOffAt);
            params.put("eta_at", etaAt);
            params.put("reason", reason);
            params.put("updated_by", staffName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, MODIFY_FEEDER_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(requireContext(), "✓ আপডেট হয়েছে। গ্রাহকরা জানতে পারবেন।", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadFeeders();
                    } else {
                        Toast.makeText(requireContext(), "⚠️ সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(requireContext(), "⚠️ নেটওয়ার্ক সমস্যা", Toast.LENGTH_SHORT).show()) {
            @Override
            public String getBodyContentType() { return "application/json; charset=utf-8"; }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // ── হেল্পার ফাংশন ──
    // ══════════════════════════════════════════
    private String buildDateTime(Calendar cal, int hour, int minute) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d %02d:%02d:00",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private Calendar parseDbDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("null")) return null;
        try {
            String[] parts = raw.trim().split("[ T]");
            String[] dateParts = parts[0].split("-");
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            c.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
            if (parts.length > 1) {
                String[] timeParts = parts[1].split(":");
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            }
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDbDateTime(String raw) {
        Calendar c = parseDbDateTime(raw);
        if (c == null) return "—";
        return bengaliDate(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH), c.get(Calendar.YEAR))
                + ", " + bengaliTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private String bengaliDate(int day, int month, int year) {
        String[] months = {"জানুয়ারি","ফেব্রুয়ারি","মার্চ","এপ্রিল","মে","জুন","জুলাই","আগস্ট","সেপ্টেম্বর","অক্টোবর","নভেম্বর","ডিসেম্বর"};
        return toBn(day) + " " + months[month] + " " + toBn(year);
    }

    private String bengaliTime(int hour, int minute) {
        String period = hour < 12 ? "সকাল" : hour < 15 ? "দুপুর" : hour < 18 ? "বিকাল" : hour < 20 ? "সন্ধ্যা" : "রাত";
        int hr12 = hour % 12;
        if (hr12 == 0) hr12 = 12;
        return period + " " + toBn(hr12) + ":" + toBn2(minute);
    }

    private String toBn(int num) {
        String[] bn = {"০","১","২","৩","৪","৫","৬","৭","৮","৯"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(num).toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(bn[c - '0']); else sb.append(c);
        }
        return sb.toString();
    }

    private String toBn2(int num) {
        return toBn(num < 10 ? 0 : num / 10) + toBn(num % 10);
    }
}
