package com.iqra.amarbidyutadmin;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {


    EditText etStaffId, etPass;
    LinearLayout idWrap, passWrap, errBox;
    TextView errMsg, tvForgot, fillAdmin, fillSba1, fillSba2;
    CheckBox cbRemember;
    Button btnLogin;
    ImageButton btnEye;

    boolean passwordVisible = false;

    RequestQueue requestQueue;

    // লগইন চেক করার API
    static final String LOGIN_URL = "https://dainikbhorerbarta.com/bidyut_super_admin/admin_login.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etStaffId  = findViewById(R.id.etStaffId);
        etPass     = findViewById(R.id.etPass);
        idWrap     = findViewById(R.id.idWrap);
        passWrap   = findViewById(R.id.passWrap);
        errBox     = findViewById(R.id.errBox);
        errMsg     = findViewById(R.id.errMsg);
        tvForgot   = findViewById(R.id.tvForgot);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin   = findViewById(R.id.btnLogin);
        btnEye     = findViewById(R.id.btnEye);

        requestQueue = Volley.newRequestQueue(this);

        // ── আগে লগইন করা থাকলে সরাসরি Dashboard এ যাও ──
        SharedPreferences prefs = getSharedPreferences("amarbidyut_admin", MODE_PRIVATE);
        if (prefs.getBoolean("logged_in", false)) {
            goToDashboard(
                    prefs.getString("staff_id", ""),
                    prefs.getString("name", ""),
                    prefs.getString("role", ""),
                    prefs.getString("sub_id", ""),
                    prefs.getString("sub_name", "")
            );
            return;
        }

        // ── focus হলে input বক্সের রং পরিবর্তন (নীল বর্ডার) ──
        etStaffId.setOnFocusChangeListener((v, hasFocus) ->
                idWrap.setBackgroundResource(hasFocus ? R.drawable.bg_input_focused : R.drawable.bg_input));

        etPass.setOnFocusChangeListener((v, hasFocus) ->
                passWrap.setBackgroundResource(hasFocus ? R.drawable.bg_input_focused : R.drawable.bg_input));

        btnEye.setOnClickListener(v -> togglePasswordVisibility());

        tvForgot.setOnClickListener(v ->
                android.widget.Toast.makeText(this,
                        "পাসওয়ার্ড রিসেট করতে অফিসে যোগাযোগ করুন",
                        android.widget.Toast.LENGTH_SHORT).show());

        btnLogin.setOnClickListener(v -> attemptLogin());

    }

    private void togglePasswordVisibility() {
        int cursor = etPass.getSelectionStart();
        if (passwordVisible) {
            etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnEye.setImageResource(R.drawable.ic_eye);
        } else {
            etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnEye.setImageResource(R.drawable.ic_eye_off);
        }
        passwordVisible = !passwordVisible;
        etPass.setSelection(Math.max(cursor, 0));
    }

    private void attemptLogin() {
        String id = etStaffId.getText().toString().trim();
        String pass = etPass.getText().toString();

        if (id.isEmpty() || pass.isEmpty()) {
            showError("⚠️ Staff ID এবং পাসওয়ার্ড দিন");
            return;
        }

        hideError();
        btnLogin.setEnabled(false);
        btnLogin.setText("লগইন হচ্ছে...");

        JSONObject params = new JSONObject();
        try {
            params.put("staff_id", id);
            params.put("password", pass);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                LOGIN_URL,
                params,

                response -> {

                    btnLogin.setEnabled(true);
                    btnLogin.setText("লগইন করুন   →");

                    try {
                        boolean success = response.getBoolean("success");

                        if (!success) {
                            showError("⚠️ ভুল Staff ID বা পাসওয়ার্ড");
                            return;
                        }

                        String name    = response.getString("name");
                        String role    = response.getString("role");      // "admin" বা "sba"
                        String subId   = response.optString("sub_id", "");
                        String subName = response.optString("sub_name", "");

                        // ── SharedPreferences এ session সেভ করা ──
                        SharedPreferences prefs = getSharedPreferences("amarbidyut_admin", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("logged_in", true);
                        editor.putString("staff_id", id);
                        editor.putString("name", name);
                        editor.putString("role", role);
                        editor.putString("sub_id", subId);
                        editor.putString("sub_name", subName);
                        editor.putBoolean("remember", cbRemember != null && cbRemember.isChecked());
                        editor.apply();

                        goToDashboard(id, name, role, subId, subName);

                    } catch (JSONException e) {
                        showError("⚠️ Server থেকে ভুল উত্তর এসেছে: " + e.getMessage());
                    }

                },

                error -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("লগইন করুন   →");
                    showError("⚠️ নেটওয়ার্ক সমস্যা — আবার চেষ্টা করুন");
                }
        ) {

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

        };

        // ── পুরনো cache হওয়া response ব্যবহার বন্ধ করা ──
        request.setShouldCache(false);
        requestQueue.getCache().clear();

        requestQueue.add(request);
    }

    private void hideError() {
        errBox.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        errMsg.setText(msg);
        errBox.setVisibility(View.VISIBLE);
    }

    private void goToDashboard(String staffId, String name, String role, String subId, String subName) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("staff_id", staffId);
        intent.putExtra("name", name);
        intent.putExtra("role", role);
        intent.putExtra("sub_id", subId);
        intent.putExtra("sub_name", subName);
        startActivity(intent);
        finish();
    }
}