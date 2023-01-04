package com.example.sfl_is.ui.manager.employees;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.sfl_is.Common;
import com.example.sfl_is.R;
import com.example.sfl_is.databinding.ActivityManagerBinding;
import com.example.sfl_is.databinding.ActivityWarehouseBinding;
import com.example.sfl_is.ui.manager.ManagerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WarehouseActivity extends AppCompatActivity {

    private TextView userView;
    private ImageView logoView;

    private ScrollView employeesView;
    private LinearLayout employeesLayout;

    private String username;
    private String role;

    private Button backButton;

    private ActivityWarehouseBinding binding;

    private RequestQueue requestQueue;
    private String urlStaff = "https://sfl-dev.azurewebsites.net/api/v1/Staff/{username}";

    private ArrayList<String> employees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse);

        binding = ActivityWarehouseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getExtras();
        initViews();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::populateData);
        if (!executorService.isTerminated()) {
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(50, TimeUnit.SECONDS)) {
                    Log.i("EXEC_LOG", "Service terminated successfully1.");
                }
                else {
                    Log.i("EXEC_LOG", "Service terminated unsuccessfully.");
                }
            } catch (InterruptedException e) {
                Log.e("EXEC_LOG", e.toString());
            }
        }


    }

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("username");
            role = extras.getString("role");
        }
    }

    private void initViews() {
        userView = binding.userTextView;
        logoView = binding.logoImageView;
        employeesLayout = binding.employeesLayout;
        employeesView = binding.employeesView;
        backButton = binding.buttonBack;

        setResources();
        initHandlers();
        applyStyling();
    }

    private void setResources() {
        userView.setText(username);
    }

    private void initHandlers() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WarehouseActivity.this, ManagerActivity.class);
                i.putExtra("username", username);
                i.putExtra("role", role);
                startActivity(i);
                finish();
            }
        });
    }

    private void applyStyling() {
        userView.setTextSize(15f);
        userView.setTypeface(null, Typeface.BOLD);
        userView.setGravity(Gravity.CENTER);
    }

    private void populateData() {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            String filledUrlJobs = urlStaff.replace("{username}", username);
            JsonArrayRequest request = new JsonArrayRequest(filledUrlJobs, jsonArrayListenerStaff, errorListener)
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    Log.i("VOLLEY_LOG", "APIKEY: " + Common.apiKey);
                    params.put("ApiKey", Common.apiKey);
                    return params;
                }
            };
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDisplay() {
        employeesLayout.removeAllViews();

        for (String employeeData : employees) {
            RelativeLayout relativeLayout = new RelativeLayout(WarehouseActivity.this);
            LinearLayout textLayout = new LinearLayout(WarehouseActivity.this);

            TextView usernameView = new TextView(WarehouseActivity.this);
            TextView firstNameView = new TextView(WarehouseActivity.this);
            TextView lastNameView = new TextView(WarehouseActivity.this);

            // Set text.
            String [] employeeAttributes = employeeData.split(" ");
            usernameView.setText(employeeAttributes[0]);
            firstNameView.setText(employeeAttributes[1]);

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 2; i < employeeAttributes.length; i++) {
                stringBuilder.append(" " + employeeAttributes[i]);
            }
            lastNameView.setText(stringBuilder.toString());

            // Style text.
            usernameView.setPadding(40, 15, 25, 0);
            usernameView.setTextSize(15f);
            usernameView.setTypeface(null, Typeface.BOLD);
            usernameView.setWidth(250);
            usernameView.setGravity(Gravity.CENTER);

            firstNameView.setPadding(10, 15, 25, 0);
            firstNameView.setTextSize(15f);
            firstNameView.setTypeface(null, Typeface.BOLD);
            firstNameView.setWidth(400);
            firstNameView.setGravity(Gravity.CENTER);

            lastNameView.setPadding(10, 15, 25, 0);
            lastNameView.setTextSize(15f);
            lastNameView.setTypeface(null, Typeface.BOLD);
            lastNameView.setGravity(Gravity.CENTER);

            // Add text views to text layout.
            textLayout.addView(usernameView);
            textLayout.addView(firstNameView);
            textLayout.addView(lastNameView);

            // Add text layout and button to relative layout.
            relativeLayout.addView(textLayout);

            // Align text to the left side of page.
            RelativeLayout.LayoutParams textViewLayoutParams =
                    (RelativeLayout.LayoutParams) textLayout.getLayoutParams();
            textViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            employeesLayout.addView(relativeLayout);
        }
    }

    private Response.Listener<JSONArray> jsonArrayListenerStaff = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            ArrayList<String> data = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject object = response.getJSONObject(i);
                    String username = object.getString("name");
                    String surname = object.getString("surname");
                    String roleID = object.getString("roleID");

                    data.add(username + " " + surname + " " + Common.roleIDToName.get(Integer.parseInt(roleID)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }

            employees.addAll(data);
            setDisplay();
        }
    };

    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("LOG_VOLLEY", error.toString());
        }
    };
}