package com.iqra.amarbidyutadmin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class NoticeFragment extends Fragment {

    private static final String ARG_ROLE     = "arg_role";
    private static final String ARG_SUB_ID   = "arg_sub_id";
    private static final String ARG_SUB_NAME = "arg_sub_name";
    private static final String ARG_NAME     = "arg_name";

    static final String BASE = "https://dainikbhorerbarta.com/bidyut_apps/";
    static final String BASE2 = "https://dainikbhorerbarta.com/bidyut_super_admin/";
    static final String SUBSTATION_URL    = BASE + "get_substation.php";
    static final String FEEDER_URL        = BASE + "get_feeder.php?substation_id=";
    static final String AREA_URL          = BASE + "get_areas.php?feeder_id=";
    static final String SEND_NOTICE_URL   = BASE2 + "send_notice.php";
    static final String GET_NOTICES_URL   = BASE2 + "get_notices.php";
    static final String UPDATE_NOTICE_URL = BASE2 + "update_notice.php";
    static final String DELETE_NOTICE_URL = BASE2 + "delete_notice.php";

    // ── Views ──
    LinearLayout pickSub, pickFeed, pickArea;
    LinearLayout pickStartDate, pickStartTime, pickEndDate, pickEndTime;
    LinearLayout llNoticeList;
    TextView tvSubVal, tvFeedVal, tvAreaVal;
    TextView tvStartDateVal, tvStartTimeVal, tvEndDateVal, tvEndTimeVal;
    TextView tvNoticeToast, tvNoNotices;
    EditText etMw, etMessage;
    Spinner spType;
    Button btnSubmit;

    String role, subId, subName, staffName;

    String selSubId = "", selSubName = "";
    String selFeedId = "", selFeedName = "সব ফিডার";
    String selAreaId = "", selAreaName = "সব এলাকা";

    // ── শুরু ও শেষ সময়ের জন্য Calendar ──
    Calendar startCalendar = null;
    int startHour = -1, startMinute = -1;
    Calendar endCalendar = null;
    int endHour = -1, endMinute = -1;

    RequestQueue requestQueue;
    Handler toastHandler = new Handler(Looper.getMainLooper());
    Runnable toastHideRunnable;
    Handler noticeRefreshHandler = new Handler(Looper.getMainLooper());
    Runnable noticeRefreshRunnable;
    public static NoticeFragment newInstance(String role, String subId, String subName, String name) {
        NoticeFragment f = new NoticeFragment();
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
        View v = inflater.inflate(R.layout.fragment_notice, container, false);

        pickSub       = v.findViewById(R.id.pickSub);
        pickFeed      = v.findViewById(R.id.pickFeed);
        pickArea      = v.findViewById(R.id.pickArea);
        pickStartDate = v.findViewById(R.id.pickStartDate);
        pickStartTime = v.findViewById(R.id.pickStartTime);
        pickEndDate   = v.findViewById(R.id.pickEndDate);
        pickEndTime   = v.findViewById(R.id.pickEndTime);
        tvSubVal      = v.findViewById(R.id.tvSubVal);
        tvFeedVal     = v.findViewById(R.id.tvFeedVal);
        tvAreaVal     = v.findViewById(R.id.tvAreaVal);
        tvStartDateVal = v.findViewById(R.id.tvStartDateVal);
        tvStartTimeVal = v.findViewById(R.id.tvStartTimeVal);
        tvEndDateVal   = v.findViewById(R.id.tvEndDateVal);
        tvEndTimeVal   = v.findViewById(R.id.tvEndTimeVal);
        tvNoticeToast = v.findViewById(R.id.tvNoticeToast);
        tvNoNotices   = v.findViewById(R.id.tvNoNotices);
        etMw          = v.findViewById(R.id.etMw);
        etMessage     = v.findViewById(R.id.etMessage);
        spType        = v.findViewById(R.id.spType);
        btnSubmit     = v.findViewById(R.id.btnSubmitNotice);
        llNoticeList  = v.findViewById(R.id.llNoticeList);

        if (getArguments() != null) {
            role      = getArguments().getString(ARG_ROLE, "sba");
            subId     = getArguments().getString(ARG_SUB_ID, "");
            subName   = getArguments().getString(ARG_SUB_NAME, "");
            staffName = getArguments().getString(ARG_NAME, "");
        }

        requestQueue = Volley.newRequestQueue(requireContext());

        setupTypeSpinner();
        setupTargetRows();
        setupStartEndPickers();

        pickFeed.setAlpha(0.5f);
        pickArea.setAlpha(0.5f);

        btnSubmit.setOnClickListener(view -> submitNotice(false));

        loadRecentNotices();
        startNoticeAutoRefresh();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toastHideRunnable != null) toastHandler.removeCallbacks(toastHideRunnable);
        noticeRefreshHandler.removeCallbacks(noticeRefreshRunnable);
    }


    private void startNoticeAutoRefresh() {
        noticeRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // ── ৬০ সেকেন্ড পরপর notice list রিফ্রেশ (expired notice সরে যাবে) ──
                if (isAdded()) loadRecentNotices();
                noticeRefreshHandler.postDelayed(this, 60000);
            }
        };
        noticeRefreshHandler.postDelayed(noticeRefreshRunnable, 60000);
    }

    private void setupTypeSpinner() {
        String[] types = {"শিডিউল লোডশেডিং", "যান্ত্রিক ত্রুটি", "জরুরি মেরামত", "সাধারণ বিজ্ঞপ্তি"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);
    }

    private void setupTargetRows() {

        if ("sba".equals(role)) {
            selSubId = subId;
            selSubName = subName;
            tvSubVal.setText("🔒 " + (subName != null ? subName : "—"));
            pickSub.setClickable(false);
            pickSub.setAlpha(0.7f);

            pickFeed.setAlpha(1f);
            pickFeed.setOnClickListener(view -> {
                if (selSubId != null && !selSubId.isEmpty()) loadFeeders(selSubId);
            });

            pickArea.setOnClickListener(view -> {
                if (!selFeedId.isEmpty()) loadAreas(selFeedId);
                else Toast.makeText(requireContext(), "আগে ফিডার বাছাই করুন", Toast.LENGTH_SHORT).show();
            });

        } else {
            pickSub.setOnClickListener(view -> loadSubstations());

            pickFeed.setOnClickListener(view -> {
                if (!selSubId.isEmpty()) loadFeeders(selSubId);
                else Toast.makeText(requireContext(), "আগে সাব-স্টেশন বাছাই করুন", Toast.LENGTH_SHORT).show();
            });

            pickArea.setOnClickListener(view -> {
                if (!selFeedId.isEmpty()) loadAreas(selFeedId);
                else Toast.makeText(requireContext(), "আগে ফিডার বাছাই করুন", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // ══════════════════════════════════════════
    // ── শুরু ও শেষ — তারিখ ও সময় picker ──
    // ══════════════════════════════════════════
    private void setupStartEndPickers() {

        pickStartDate.setOnClickListener(v -> {
            Calendar c = startCalendar != null ? (Calendar) startCalendar.clone() : Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        if (startCalendar == null) startCalendar = Calendar.getInstance();
                        startCalendar.set(Calendar.YEAR, year);
                        startCalendar.set(Calendar.MONTH, month);
                        startCalendar.set(Calendar.DAY_OF_MONTH, day);
                        tvStartDateVal.setText(bengaliDate(day, month, year));
                        tvStartDateVal.setTextColor(0xFF0F1923);
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        pickStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int h = startHour >= 0 ? startHour : c.get(Calendar.HOUR_OF_DAY);
            int m = startMinute >= 0 ? startMinute : c.get(Calendar.MINUTE);
            TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                    (tp, hourOfDay, minute) -> {
                        startHour = hourOfDay;
                        startMinute = minute;
                        tvStartTimeVal.setText(bengaliTime(hourOfDay, minute));
                        tvStartTimeVal.setTextColor(0xFF0F1923);
                    }, h, m, false);
            dialog.show();
        });

        pickEndDate.setOnClickListener(v -> {
            Calendar c = endCalendar != null ? (Calendar) endCalendar.clone() : Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        if (endCalendar == null) endCalendar = Calendar.getInstance();
                        endCalendar.set(Calendar.YEAR, year);
                        endCalendar.set(Calendar.MONTH, month);
                        endCalendar.set(Calendar.DAY_OF_MONTH, day);
                        tvEndDateVal.setText(bengaliDate(day, month, year));
                        tvEndDateVal.setTextColor(0xFF0F1923);
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        pickEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int h = endHour >= 0 ? endHour : c.get(Calendar.HOUR_OF_DAY);
            int m = endMinute >= 0 ? endMinute : c.get(Calendar.MINUTE);
            TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                    (tp, hourOfDay, minute) -> {
                        endHour = hourOfDay;
                        endMinute = minute;
                        tvEndTimeVal.setText(bengaliTime(hourOfDay, minute));
                        tvEndTimeVal.setTextColor(0xFF0F1923);
                    }, h, m, false);
            dialog.show();
        });
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

    // ── MySQL DATETIME ফরম্যাট তৈরি ──
    private String buildDateTime(Calendar cal, int hour, int minute) {
        if (cal == null || hour < 0 || minute < 0) return "";
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d %02d:%02d:00",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private void loadSubstations() {
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, SUBSTATION_URL, null,
                response -> {
                    ArrayList<String[]> items = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            items.add(new String[]{o.getString("name"), o.getString("id")});
                        } catch (JSONException ignored) {}
                    }
                    showSelectSheet("সাব-স্টেশন বাছাই করুন", items, (name, id) -> {
                        selSubId = id;
                        selSubName = name;
                        tvSubVal.setText(name);

                        selFeedId = "";
                        selFeedName = "সব ফিডার";
                        tvFeedVal.setText(selFeedName);
                        selAreaId = "";
                        selAreaName = "সব এলাকা";
                        tvAreaVal.setText(selAreaName);

                        pickFeed.setAlpha(1f);
                        pickArea.setAlpha(0.5f);
                    });
                },
                error -> Toast.makeText(requireContext(), "ডাটা লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show());
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void loadFeeders(String subIdToUse) {
        String url = FEEDER_URL + subIdToUse;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    ArrayList<String[]> items = new ArrayList<>();
                    items.add(new String[]{"সব ফিডার (পুরো সাব-স্টেশন)", ""});
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            items.add(new String[]{o.getString("name") + " ফিডার", o.getString("id")});
                        } catch (JSONException ignored) {}
                    }
                    showSelectSheet("ফিডার বাছাই করুন", items, (name, id) -> {
                        selFeedId = id;
                        selFeedName = name;
                        tvFeedVal.setText(name);

                        selAreaId = "";
                        selAreaName = "সব এলাকা";
                        tvAreaVal.setText(selAreaName);

                        pickArea.setAlpha(id.isEmpty() ? 0.5f : 1f);
                    });
                },
                error -> Toast.makeText(requireContext(), "ফিডার লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show());
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void loadAreas(String feedIdToUse) {
        String url = AREA_URL + feedIdToUse;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    ArrayList<String[]> items = new ArrayList<>();
                    items.add(new String[]{"সব এলাকা", ""});
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            items.add(new String[]{o.getString("name"), o.getString("id")});
                        } catch (JSONException ignored) {}
                    }
                    showSelectSheet("এলাকা বাছাই করুন", items, (name, id) -> {
                        selAreaId = id;
                        selAreaName = name;
                        tvAreaVal.setText(name);
                    });
                },
                error -> Toast.makeText(requireContext(), "এলাকা লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show());
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    interface OnPickListener { void onPick(String name, String id); }

    private void showSelectSheet(String title, ArrayList<String[]> items, OnPickListener listener) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_select, null);
        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        ListView lv = sheetView.findViewById(R.id.lvSheet);
        tvTitle.setText(title);

        ArrayAdapter<String[]> adapter = new ArrayAdapter<String[]>(requireContext(), R.layout.item_bottom_sheet, items) {
            @NonNull
            @Override
            public View getView(int pos, View cv, @NonNull ViewGroup parent) {
                if (cv == null) cv = LayoutInflater.from(requireContext()).inflate(R.layout.item_bottom_sheet, parent, false);
                String[] item = items.get(pos);
                ((TextView) cv.findViewById(R.id.tvItemName)).setText(item[0]);
                TextView tvSub = cv.findViewById(R.id.tvItemSub);
                if (tvSub != null) tvSub.setText("");
                return cv;
            }
        };
        lv.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        dialog.show();

        lv.setOnItemClickListener((p, view, pos, id) -> {
            listener.onPick(items.get(pos)[0], items.get(pos)[1]);
            dialog.dismiss();
        });
    }

    // ══════════════════════════════════════════
    // ── সাবমিট নোটিশ ──
    // ══════════════════════════════════════════
    private void submitNotice(boolean forceReplace) {

        if ("admin".equals(role) && selSubId.isEmpty()) {
            showToastMsg("⚠️ সাব-স্টেশন বাছাই করুন", false);
            return;
        }

        String type = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "";
        String message = etMessage.getText().toString().trim();

        if (message.isEmpty()) {
            showToastMsg("⚠️ বার্তা লিখুন", false);
            return;
        }
        if (startCalendar == null || startHour < 0) {
            showToastMsg("⚠️ শুরুর তারিখ ও সময় বাছাই করুন", false);
            return;
        }
        if (endCalendar == null || endHour < 0) {
            showToastMsg("⚠️ শেষের তারিখ ও সময় বাছাই করুন", false);
            return;
        }

        String startsAt = buildDateTime(startCalendar, startHour, startMinute);
        String endsAt   = buildDateTime(endCalendar, endHour, endMinute);

        if (endsAt.compareTo(startsAt) <= 0) {
            showToastMsg("⚠️ শেষের সময় অবশ্যই শুরুর সময়ের পরে হতে হবে", false);
            return;
        }

        String startDisplay = tvStartDateVal.getText() + ", " + tvStartTimeVal.getText();
        String endDisplay   = tvEndDateVal.getText() + ", " + tvEndTimeVal.getText();
        String mw = etMw.getText().toString().trim();

        JSONObject params = new JSONObject();
        try {
            params.put("type", type);
            params.put("sub_id", "sba".equals(role) ? subId : selSubId);
            params.put("feeder_id", selFeedId);
            params.put("area_id", selAreaId);
            params.put("notice_date", startDisplay);
            params.put("notice_time", endDisplay);
            params.put("starts_at", startsAt);
            params.put("expires_at", endsAt);
            params.put("start_display", startDisplay);
            params.put("end_display", endDisplay);
            params.put("mw", mw);
            params.put("message", message);
            params.put("created_by", staffName);
            params.put("force_replace", forceReplace);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("পাঠানো হচ্ছে...");

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, SEND_NOTICE_URL, params,
                response -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("নোটিশ প্রকাশ ও Notify করুন");

                    boolean success = response.optBoolean("success", false);
                    boolean duplicate = response.optBoolean("duplicate", false);

                    if (success) {
                        boolean replaced = response.optBoolean("replaced", false);
                        showToastMsg(replaced
                                ? "✓ আগের নোটিশটি আপডেট (Replace) হয়েছে। গ্রাহকরা notify হয়েছেন।"
                                : "✓ নোটিশ পাঠানো হয়েছে। গ্রাহকরা notify হয়েছেন।", true);
                        resetForm();
                        loadRecentNotices();

                    } else if (duplicate) {
                        String existingType = response.optString("existing_type", "");
                        new AlertDialog.Builder(requireContext())
                                .setTitle("আগে থেকে নোটিশ আছে")
                                .setMessage("এই এলাকায় ইতিমধ্যে একটি \"" + existingType + "\" নোটিশ চলমান আছে।\n\nনতুনটি দিয়ে সেটা Replace (আপডেট) করতে চান?")
                                .setPositiveButton("হ্যাঁ, Replace করুন", (d, w) -> submitNotice(true))
                                .setNegativeButton("বাতিল", null)
                                .show();

                    } else {
                        showToastMsg("⚠️ নোটিশ পাঠাতে সমস্যা হয়েছে", false);
                    }
                },
                error -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("নোটিশ প্রকাশ ও Notify করুন");
                    showToastMsg("⚠️ নেটওয়ার্ক সমস্যা — আবার চেষ্টা করুন", false);
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void resetForm() {
        etMessage.setText("");
        etMw.setText("");
        tvStartDateVal.setText("তারিখ");
        tvStartDateVal.setTextColor(0xFF5C6E82);
        tvStartTimeVal.setText("সময়");
        tvStartTimeVal.setTextColor(0xFF5C6E82);
        tvEndDateVal.setText("তারিখ");
        tvEndDateVal.setTextColor(0xFF5C6E82);
        tvEndTimeVal.setText("সময়");
        tvEndTimeVal.setTextColor(0xFF5C6E82);
        startCalendar = null; startHour = -1; startMinute = -1;
        endCalendar = null; endHour = -1; endMinute = -1;
    }

    private void showToastMsg(String msg, boolean ok) {
        tvNoticeToast.setVisibility(View.VISIBLE);
        tvNoticeToast.setText(msg);
        if (ok) {
            tvNoticeToast.setTextColor(0xFF15803D);
            tvNoticeToast.setBackgroundResource(R.drawable.bg_chip);
        } else {
            tvNoticeToast.setTextColor(0xFFDC2626);
            tvNoticeToast.setBackgroundResource(R.drawable.bg_chip_amber);
        }

        if (toastHideRunnable != null) toastHandler.removeCallbacks(toastHideRunnable);
        toastHideRunnable = () -> {
            if (tvNoticeToast != null) tvNoticeToast.setVisibility(View.GONE);
        };
        toastHandler.postDelayed(toastHideRunnable, 30000);
    }

    // ══════════════════════════════════════════
    // ── সাম্প্রতিক নোটিশ লোড ──
    // ══════════════════════════════════════════
    private void loadRecentNotices() {

        String url = GET_NOTICES_URL;
        if ("sba".equals(role) && subId != null && !subId.isEmpty()) {
            url += "?sub_id=" + subId;
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    llNoticeList.removeAllViews();

                    if (response.length() == 0) {
                        tvNoNotices.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvNoNotices.setVisibility(View.GONE);

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            View item = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.item_notice, llNoticeList, false);

                            TextView tvType    = item.findViewById(R.id.tvNoticeType);
                            TextView tvTarget  = item.findViewById(R.id.tvNoticeTarget);
                            TextView tvMessage = item.findViewById(R.id.tvNoticeMessage);
                            TextView chipStart = item.findViewById(R.id.chipStart);
                            TextView chipEnd   = item.findViewById(R.id.chipEnd);
                            TextView chipMw    = item.findViewById(R.id.chipMw);
                            TextView btnEdit   = item.findViewById(R.id.btnEditNotice);
                            TextView btnDelete = item.findViewById(R.id.btnDeleteNotice);

                            final int noticeId = o.optInt("id", 0);
                            final String type   = o.optString("type", "নোটিশ");
                            final String message = o.optString("message", "");
                            // notice_date/notice_time এখন শুরু/শেষের ডিসপ্লে টেক্সট হিসেবে ব্যবহৃত
                            final String startDisplay = o.optString("notice_date", "—");
                            final String endDisplay   = o.optString("notice_time", "—");
                            final String mw      = o.optString("mw", "");
                            final String startsAtRaw = o.optString("starts_at", "");
                            final String endsAtRaw   = o.optString("expires_at", "");

                            tvType.setText(type);
                            tvMessage.setText(message);

                            String subN  = o.optString("sub_name", "");
                            String feedN = o.optString("feeder_name", "");
                            String areaN = o.optString("area_name", "");
                            StringBuilder target = new StringBuilder(subN + " সাবস্টেশন");
                            if (!feedN.isEmpty() && !feedN.equals("null")) target.append(" · ").append(feedN).append(" ফিডার");
                            if (!areaN.isEmpty() && !areaN.equals("null")) target.append(" · ").append(areaN);
                            tvTarget.setText(target.toString());

                            chipStart.setText(startDisplay);
                            chipEnd.setText(endDisplay);

                            if (mw.isEmpty() || mw.equals("0")) {
                                chipMw.setVisibility(View.GONE);
                            } else {
                                chipMw.setText("⚡ " + mw + " মেগাওয়াট");
                                chipMw.setVisibility(View.VISIBLE);
                            }

                            btnEdit.setOnClickListener(v ->
                                    openEditSheet(noticeId, type, message, startsAtRaw, endsAtRaw, startDisplay, endDisplay, mw));

                            btnDelete.setOnClickListener(v ->
                                    confirmDelete(noticeId));

                            llNoticeList.addView(item);
                        } catch (JSONException ignored) {}
                    }
                },
                error -> { /* চুপচাপ ignore */ });

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void confirmDelete(int noticeId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("নোটিশ ডিলিট করবেন?")
                .setMessage("এই নোটিশটি স্থায়ীভাবে মুছে যাবে এবং গ্রাহকের অ্যাপ থেকেও সরে যাবে। আপনি কি নিশ্চিত?")
                .setPositiveButton("হ্যাঁ, ডিলিট করুন", (dialog, which) -> deleteNotice(noticeId))
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void deleteNotice(int noticeId) {
        JSONObject params = new JSONObject();
        try {
            params.put("id", noticeId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, DELETE_NOTICE_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(requireContext(), "✓ নোটিশ ডিলিট হয়েছে", Toast.LENGTH_SHORT).show();
                        loadRecentNotices();
                    } else {
                        Toast.makeText(requireContext(), "⚠️ ডিলিট করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(requireContext(), "⚠️ নেটওয়ার্ক সমস্যা", Toast.LENGTH_SHORT).show()) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // ── এডিট বটম শীট (শুরু+শেষ দুটোই) ──
    // ══════════════════════════════════════════
    Calendar editStartCal = null;
    int editStartHour = -1, editStartMinute = -1;
    Calendar editEndCal = null;
    int editEndHour = -1, editEndMinute = -1;

    private void openEditSheet(int noticeId, String type, String message,
                                String startsAtRaw, String endsAtRaw,
                                String startDisplay, String endDisplay, String mw) {

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_notice, null);

        Spinner spEditType = sheetView.findViewById(R.id.spEditType);
        LinearLayout pickEditStartDate = sheetView.findViewById(R.id.pickEditStartDate);
        LinearLayout pickEditStartTime = sheetView.findViewById(R.id.pickEditStartTime);
        LinearLayout pickEditEndDate   = sheetView.findViewById(R.id.pickEditEndDate);
        LinearLayout pickEditEndTime   = sheetView.findViewById(R.id.pickEditEndTime);
        TextView tvEditStartDateVal = sheetView.findViewById(R.id.tvEditStartDateVal);
        TextView tvEditStartTimeVal = sheetView.findViewById(R.id.tvEditStartTimeVal);
        TextView tvEditEndDateVal   = sheetView.findViewById(R.id.tvEditEndDateVal);
        TextView tvEditEndTimeVal   = sheetView.findViewById(R.id.tvEditEndTimeVal);
        EditText etEditMw      = sheetView.findViewById(R.id.etEditMw);
        EditText etEditMessage = sheetView.findViewById(R.id.etEditMessage);
        Button btnUpdate       = sheetView.findViewById(R.id.btnUpdateNotice);

        String[] types = {"শিডিউল লোডশেডিং", "যান্ত্রিক ত্রুটি", "জরুরি মেরামত", "সাধারণ বিজ্ঞপ্তি"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spEditType.setAdapter(typeAdapter);
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(type)) { spEditType.setSelection(i); break; }
        }

        // ── পুরনো DB মান (YYYY-MM-DD HH:MM:SS) থেকে Calendar বানানো — চেষ্টা করে, না পারলে এখনকার সময়
        editStartCal = parseDbDateTime(startsAtRaw);
        editEndCal   = parseDbDateTime(endsAtRaw);
        if (editStartCal != null) { editStartHour = editStartCal.get(Calendar.HOUR_OF_DAY); editStartMinute = editStartCal.get(Calendar.MINUTE); }
        if (editEndCal != null)   { editEndHour = editEndCal.get(Calendar.HOUR_OF_DAY); editEndMinute = editEndCal.get(Calendar.MINUTE); }

        tvEditStartDateVal.setText(startDisplay.isEmpty() ? "তারিখ" : startDisplay.split(",")[0]);
        tvEditStartTimeVal.setText(editStartHour >= 0 ? bengaliTime(editStartHour, editStartMinute) : "সময়");
        tvEditEndDateVal.setText(endDisplay.isEmpty() ? "তারিখ" : endDisplay.split(",")[0]);
        tvEditEndTimeVal.setText(editEndHour >= 0 ? bengaliTime(editEndHour, editEndMinute) : "সময়");
        etEditMw.setText(mw);
        etEditMessage.setText(message);

        pickEditStartDate.setOnClickListener(v -> {
            Calendar c = editStartCal != null ? editStartCal : Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        if (editStartCal == null) editStartCal = Calendar.getInstance();
                        editStartCal.set(Calendar.YEAR, year);
                        editStartCal.set(Calendar.MONTH, month);
                        editStartCal.set(Calendar.DAY_OF_MONTH, day);
                        tvEditStartDateVal.setText(bengaliDate(day, month, year));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        pickEditStartTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                    (tp, hourOfDay, minute) -> {
                        editStartHour = hourOfDay; editStartMinute = minute;
                        tvEditStartTimeVal.setText(bengaliTime(hourOfDay, minute));
                    }, Math.max(editStartHour, 0), Math.max(editStartMinute, 0), false);
            dialog.show();
        });

        pickEditEndDate.setOnClickListener(v -> {
            Calendar c = editEndCal != null ? editEndCal : Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        if (editEndCal == null) editEndCal = Calendar.getInstance();
                        editEndCal.set(Calendar.YEAR, year);
                        editEndCal.set(Calendar.MONTH, month);
                        editEndCal.set(Calendar.DAY_OF_MONTH, day);
                        tvEditEndDateVal.setText(bengaliDate(day, month, year));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        pickEditEndTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                    (tp, hourOfDay, minute) -> {
                        editEndHour = hourOfDay; editEndMinute = minute;
                        tvEditEndTimeVal.setText(bengaliTime(hourOfDay, minute));
                    }, Math.max(editEndHour, 0), Math.max(editEndMinute, 0), false);
            dialog.show();
        });

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        dialog.show();

        btnUpdate.setOnClickListener(v -> {
            String newType = spEditType.getSelectedItem().toString();
            String newMessage = etEditMessage.getText().toString().trim();
            String newMw = etEditMw.getText().toString().trim();

            if (newMessage.isEmpty()) {
                Toast.makeText(requireContext(), "বার্তা লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (editStartCal == null || editStartHour < 0) {
                Toast.makeText(requireContext(), "শুরুর তারিখ ও সময় বাছাই করুন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (editEndCal == null || editEndHour < 0) {
                Toast.makeText(requireContext(), "শেষের তারিখ ও সময় বাছাই করুন", Toast.LENGTH_SHORT).show();
                return;
            }

            String newStartsAt = buildDateTime(editStartCal, editStartHour, editStartMinute);
            String newEndsAt   = buildDateTime(editEndCal, editEndHour, editEndMinute);

            if (newEndsAt.compareTo(newStartsAt) <= 0) {
                Toast.makeText(requireContext(), "শেষের সময় শুরুর সময়ের পরে হতে হবে", Toast.LENGTH_SHORT).show();
                return;
            }

            String newStartDisplay = tvEditStartDateVal.getText() + ", " + tvEditStartTimeVal.getText();
            String newEndDisplay   = tvEditEndDateVal.getText() + ", " + tvEditEndTimeVal.getText();

            updateNotice(noticeId, newType, newStartDisplay, newEndDisplay, newStartsAt, newEndsAt, newMw, newMessage, dialog);
        });
    }

    // ── DB থেকে আসা "YYYY-MM-DD HH:MM:SS" কে Calendar এ রূপান্তর ──
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

    private void updateNotice(int id, String type, String startDisplay, String endDisplay,
                               String startsAt, String endsAt, String mw,
                               String message, BottomSheetDialog dialog) {

        JSONObject params = new JSONObject();
        try {
            params.put("id", id);
            params.put("type", type);
            params.put("notice_date", startDisplay);
            params.put("notice_time", endDisplay);
            params.put("starts_at", startsAt);
            params.put("expires_at", endsAt);
            params.put("mw", mw);
            params.put("message", message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, UPDATE_NOTICE_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(requireContext(), "✓ নোটিশ আপডেট হয়েছে — মেয়াদ পরিবর্তন হয়েছে", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadRecentNotices();
                    } else {
                        Toast.makeText(requireContext(), "⚠️ আপডেট করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(requireContext(), "⚠️ নেটওয়ার্ক সমস্যা", Toast.LENGTH_SHORT).show()) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }
}
