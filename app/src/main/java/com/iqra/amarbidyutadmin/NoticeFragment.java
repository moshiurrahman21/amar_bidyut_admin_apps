package com.iqra.amarbidyutadmin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
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

public class NoticeFragment extends Fragment {

    private static final String ARG_ROLE     = "arg_role";
    private static final String ARG_SUB_ID   = "arg_sub_id";
    private static final String ARG_SUB_NAME = "arg_sub_name";
    private static final String ARG_NAME     = "arg_name";

    // ── API URLs ──
    static final String BASE = "https://dainikbhorerbarta.com/bidyut_apps/";
    static final String BASE2 = "https://dainikbhorerbarta.com/bidyut_super_admin/";
    static final String SUBSTATION_URL  = BASE + "get_substation.php";
    static final String FEEDER_URL      = BASE + "get_feeder.php?substation_id=";
    static final String AREA_URL        = BASE + "get_areas.php?feeder_id=";
    static final String SEND_NOTICE_URL = BASE2 + "send_notice.php";
    static final String GET_NOTICES_URL = BASE2 + "get_notices.php";
    static final String UPDATE_NOTICE_URL = BASE2 + "update_notice.php";
    static final String DELETE_NOTICE_URL = BASE2 + "delete_notice.php";

    // ── Views --
    LinearLayout pickSub, pickFeed, pickArea, pickDate, llNoticeList;
    TextView tvSubVal, tvFeedVal, tvAreaVal, tvDateVal, tvNoticeToast, tvNoNotices;
    EditText etTime, etMw, etMessage;
    Spinner spType;
    Button btnSubmit;

    String role, subId, subName, staffName;

    String selSubId = "", selSubName = "";
    String selFeedId = "", selFeedName = "সব ফিডার";
    String selAreaId = "", selAreaName = "সব এলাকা";
    String selDate = "";

    // ── এডিট এর জন্য সাময়িক তারিখ ──
    String editingDate = "";

    RequestQueue requestQueue;

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
        pickDate      = v.findViewById(R.id.pickDate);
        tvSubVal      = v.findViewById(R.id.tvSubVal);
        tvFeedVal     = v.findViewById(R.id.tvFeedVal);
        tvAreaVal     = v.findViewById(R.id.tvAreaVal);
        tvDateVal     = v.findViewById(R.id.tvDateVal);
        tvNoticeToast = v.findViewById(R.id.tvNoticeToast);
        tvNoNotices   = v.findViewById(R.id.tvNoNotices);
        etTime        = v.findViewById(R.id.etTime);
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
        setupDatePicker();

        pickFeed.setAlpha(0.5f);
        pickArea.setAlpha(0.5f);

        btnSubmit.setOnClickListener(view -> submitNotice());

        loadRecentNotices();

        return v;
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

    private void setupDatePicker() {
        pickDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (datePicker, year, month, dayOfMonth) -> {
                        selDate = bengaliDate(dayOfMonth, month, year);
                        tvDateVal.setText(selDate);
                        tvDateVal.setTextColor(0xFF0F1923);
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private String bengaliDate(int day, int month, int year) {
        String[] months = {"জানুয়ারি","ফেব্রুয়ারি","মার্চ","এপ্রিল","মে","জুন","জুলাই","আগস্ট","সেপ্টেম্বর","অক্টোবর","নভেম্বর","ডিসেম্বর"};
        return toBn(day) + " " + months[month] + " " + toBn(year);
    }

    private String toBn(int num) {
        String[] bn = {"০","১","২","৩","৪","৫","৬","৭","৮","৯"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(num).toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(bn[c - '0']); else sb.append(c);
        }
        return sb.toString();
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

    private void submitNotice() {

        if ("admin".equals(role) && selSubId.isEmpty()) {
            showToast("⚠️ সাব-স্টেশন বাছাই করুন", false);
            return;
        }

        String type = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "";
        String message = etMessage.getText().toString().trim();

        if (message.isEmpty()) {
            showToast("⚠️ বার্তা লিখুন", false);
            return;
        }
        if (selDate.isEmpty()) {
            showToast("⚠️ তারিখ বাছাই করুন", false);
            return;
        }

        String time = etTime.getText().toString().trim();
        String mw = etMw.getText().toString().trim();

        JSONObject params = new JSONObject();
        try {
            params.put("type", type);
            params.put("sub_id", "sba".equals(role) ? subId : selSubId);
            params.put("feeder_id", selFeedId);
            params.put("area_id", selAreaId);
            params.put("notice_date", selDate);
            params.put("notice_time", time);
            params.put("mw", mw);
            params.put("message", message);
            params.put("created_by", staffName);
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
                    if (success) {
                        showToast("✓ নোটিশ পাঠানো হয়েছে। গ্রাহকরা notify হয়েছেন।", true);
                        etMessage.setText("");
                        etTime.setText("");
                        etMw.setText("");
                        loadRecentNotices();
                    } else {
                        showToast("⚠️ নোটিশ পাঠাতে সমস্যা হয়েছে", false);
                    }
                },
                error -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("নোটিশ প্রকাশ ও Notify করুন");
                    showToast("⚠️ নেটওয়ার্ক সমস্যা — আবার চেষ্টা করুন", false);
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
        req.setShouldCache(false);
        requestQueue.add(req);
    }

    private void showToast(String msg, boolean ok) {
        tvNoticeToast.setVisibility(View.VISIBLE);
        tvNoticeToast.setText(msg);
        if (ok) {
            tvNoticeToast.setTextColor(0xFF15803D);
            tvNoticeToast.setBackgroundResource(R.drawable.bg_chip);
        } else {
            tvNoticeToast.setTextColor(0xFFDC2626);
            tvNoticeToast.setBackgroundResource(R.drawable.bg_chip_amber);
        }
    }

    // ══════════════════════════════════════════
    // ── সাম্প্রতিক নোটিশ লোড (এখন Edit/Delete বাটনসহ) ──
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
                            TextView chipDate  = item.findViewById(R.id.chipDate);
                            TextView chipTime  = item.findViewById(R.id.chipTime);
                            TextView chipMw    = item.findViewById(R.id.chipMw);
                            TextView btnEdit   = item.findViewById(R.id.btnEditNotice);
                            TextView btnDelete = item.findViewById(R.id.btnDeleteNotice);

                            final int noticeId = o.optInt("id", 0);
                            final String type   = o.optString("type", "নোটিশ");
                            final String message = o.optString("message", "");
                            final String date    = o.optString("notice_date", "");
                            final String time    = o.optString("notice_time", "");
                            final String mw      = o.optString("mw", "");

                            tvType.setText(type);
                            tvMessage.setText(message);

                            String subN  = o.optString("sub_name", "");
                            String feedN = o.optString("feeder_name", "");
                            String areaN = o.optString("area_name", "");
                            StringBuilder target = new StringBuilder(subN + " সাবস্টেশন");
                            if (!feedN.isEmpty() && !feedN.equals("null")) target.append(" · ").append(feedN).append(" ফিডার");
                            if (!areaN.isEmpty() && !areaN.equals("null")) target.append(" · ").append(areaN);
                            tvTarget.setText(target.toString());

                            chipDate.setText("📅 " + (date.isEmpty() ? "—" : date));

                            if (time.isEmpty()) {
                                chipTime.setVisibility(View.GONE);
                            } else {
                                chipTime.setText("⏰ " + time);
                                chipTime.setVisibility(View.VISIBLE);
                            }

                            if (mw.isEmpty() || mw.equals("0")) {
                                chipMw.setVisibility(View.GONE);
                            } else {
                                chipMw.setText("⚡ " + mw + " মেগাওয়াট");
                                chipMw.setVisibility(View.VISIBLE);
                            }

                            // ── এডিট বাটন ──
                            btnEdit.setOnClickListener(v ->
                                    openEditSheet(noticeId, type, message, date, time, mw));

                            // ── ডিলিট বাটন (নিশ্চিত করে নেবে) ──
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

    // ══════════════════════════════════════════
    // ── ডিলিট নিশ্চিতকরণ ──
    // ══════════════════════════════════════════
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
    // ── এডিট বটম শীট ──
    // ══════════════════════════════════════════
    private void openEditSheet(int noticeId, String type, String message, String date, String time, String mw) {

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_notice, null);

        Spinner spEditType        = sheetView.findViewById(R.id.spEditType);
        LinearLayout pickEditDate = sheetView.findViewById(R.id.pickEditDate);
        TextView tvEditDateVal    = sheetView.findViewById(R.id.tvEditDateVal);
        EditText etEditTime       = sheetView.findViewById(R.id.etEditTime);
        EditText etEditMw         = sheetView.findViewById(R.id.etEditMw);
        EditText etEditMessage    = sheetView.findViewById(R.id.etEditMessage);
        Button btnUpdate          = sheetView.findViewById(R.id.btnUpdateNotice);

        // ── বর্তমান মান দিয়ে ফর্ম পূরণ ──
        String[] types = {"শিডিউল লোডশেডিং", "যান্ত্রিক ত্রুটি", "জরুরি মেরামত", "সাধারণ বিজ্ঞপ্তি"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spEditType.setAdapter(typeAdapter);
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(type)) { spEditType.setSelection(i); break; }
        }

        editingDate = date;
        tvEditDateVal.setText(date.isEmpty() ? "তারিখ বাছাই করুন" : date);
        etEditTime.setText(time);
        etEditMw.setText(mw);
        etEditMessage.setText(message);

        pickEditDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (datePicker, year, month, dayOfMonth) -> {
                        editingDate = bengaliDate(dayOfMonth, month, year);
                        tvEditDateVal.setText(editingDate);
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        dialog.show();

        btnUpdate.setOnClickListener(v -> {
            String newType = spEditType.getSelectedItem().toString();
            String newMessage = etEditMessage.getText().toString().trim();
            String newTime = etEditTime.getText().toString().trim();
            String newMw = etEditMw.getText().toString().trim();

            if (newMessage.isEmpty()) {
                Toast.makeText(requireContext(), "বার্তা লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }

            updateNotice(noticeId, newType, editingDate, newTime, newMw, newMessage, dialog);
        });
    }

    private void updateNotice(int id, String type, String date, String time, String mw,
                               String message, BottomSheetDialog dialog) {

        JSONObject params = new JSONObject();
        try {
            params.put("id", id);
            params.put("type", type);
            params.put("notice_date", date);
            params.put("notice_time", time);
            params.put("mw", mw);
            params.put("message", message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, UPDATE_NOTICE_URL, params,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(requireContext(), "✓ নোটিশ আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
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
