package com.example.sfl_is.ui.manager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.sfl_is.Common;
import com.example.sfl_is.R;
import com.example.sfl_is.databinding.ActivityEmployeeBinding;
import com.example.sfl_is.databinding.ActivityManagerBinding;
import com.example.sfl_is.ui.manager.employees.WarehouseActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ManagerActivity extends AppCompatActivity {

    private TextView userView;
    private ImageView logoView;

    private Button employeesButton;
    private Button createButton;

    private Spinner employeesSpinner;
    private Spinner jobTypesSpinner;

    private TextView parcelsView;

    private ActivityManagerBinding binding;

    private String username;
    private String role;

    private ArrayList<String> parcels = new ArrayList<>();
    private ArrayList<String> employees = new ArrayList<>();
    private ArrayList<String> jobTypes = new ArrayList<String>() {{
        add("Check in");
        add("Cargo departing confirmation");
        add("Cargo arrival confirmation");
        add("Delivery cargo confirmation");
        add("Parcel handover");
    }};

    private boolean[] selectedParcels;
    private ArrayList<Integer> selectedParcelsIndices = new ArrayList<>();

    private RequestQueue requestQueue;
    private String urlParcels = "https://sfl-dev.azurewebsites.net/api/v1/Parcels";
    private String urlStaff = "https://sfl-dev.azurewebsites.net/api/v1/Staff/{username}";
    private String urlJobs = "https://sfl-dev.azurewebsites.net/api/v1/Jobs";

    private String responseCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        binding = ActivityManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getExtras();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::populateDataParcels);
        if (!executorService.isTerminated()) {
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(50, TimeUnit.SECONDS)) {
                    Log.i("EXEC_LOG", "Service terminated successfully.");
                }
                else {
                    Log.i("EXEC_LOG", "Service terminated unsuccessfully.");
                }
            } catch (InterruptedException e) {
                Log.e("EXEC_LOG", e.toString());
            }
        }

        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::populateDataEmployees);
        if (!executorService.isTerminated()) {
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(50, TimeUnit.SECONDS)) {
                    Log.i("EXEC_LOG", "Service terminated successfully.");
                }
                else {
                    Log.i("EXEC_LOG", "Service terminated unsuccessfully.");
                }
            } catch (InterruptedException e) {
                Log.e("EXEC_LOG", e.toString());
            }
        }

        initViews();
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
        employeesButton = binding.employeesButton;
        createButton = binding.buttonCreate;
        jobTypesSpinner = binding.spinnerJobType;
        employeesSpinner = binding.spinnerEmployee;
        parcelsView = binding.parcelsTextView;

        setResources();
        initHandlers();
        applyStyling();
    }

    private void setResources() {
        userView.setText(username);
        parcelsView.setText("parcels");
    }

    private void initHandlers() {
        employeesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ManagerActivity.this, WarehouseActivity.class);
                i.putExtra("username", username);
                i.putExtra("role", role);
                startActivity(i);
                finish();
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String staffUsername = (String) employeesSpinner.getSelectedItem();
                String jobType = (String) jobTypesSpinner.getSelectedItem();
                String[] parcels = parcelsView.getText().toString().split(", ");
                Log.i("VOLLEY_LOG", "Creating job for " + staffUsername +
                        " with type " + jobType +
                        " with parcels " + parcels.toString());
                // TODO: Implement

                requestQueue = Volley.newRequestQueue(getApplicationContext());
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("StaffUsername", (String) employeesSpinner.getSelectedItem());
                    jsonBody.put("JobTypeID", Common.typeNameToID.get((String) jobTypesSpinner.getSelectedItem()));

                    JSONArray jsonArray = new JSONArray();
                    for (String s : parcelsView.getText().toString().split(", ")) {
                        jsonArray.put(s);
                    }
                    jsonBody.put("ParcelIDs", jsonArray);

                    final String requestBody = jsonBody.toString();

                    Log.i("VOLLEY_LOG", "Request body: " + requestBody);

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            urlJobs,
                            responseListenerString,
                            errorListener)
                    {
                        @Override
                        public String getBodyContentType() {
                            return "application/json; charset=utf-8";
                        }

                        @Override
                        public byte[] getBody() {
                            return requestBody == null ? null : requestBody.getBytes(StandardCharsets.UTF_8);
                        }

                        @Override
                        protected Response<String> parseNetworkResponse(NetworkResponse response) {
                            if (response != null) {
                                responseCode = String.valueOf(response.statusCode);
                            }
                            //Toast.makeText(getApplicationContext(), "Job created succesfully.", Toast.LENGTH_LONG).show();
                            return Response.success(responseCode, HttpHeaderParser.parseCacheHeaders(response));
                        }

                        @Override
                        protected VolleyError parseNetworkError(VolleyError volleyError) {
                            responseCode = "403";
                            Log.e("LOG_VOLLEY", volleyError.toString());
                            return volleyError;
                        }
                    };

                    requestQueue.add(stringRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while(responseCode.equals("")) {
                    // Ignore.
                }

                if (responseCode.equals("403")) {
                    Toast.makeText(getApplicationContext(), "Failed creating job.", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Job created successfully.", Toast.LENGTH_LONG).show();
                }
            }
        });

        parcelsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ManagerActivity.this);
                builder.setTitle("Select parcels:");
                builder.setCancelable(false);

                String[] parcelsArray = new String[parcels.size()];
                for (int i = 0; i < parcelsArray.length; i++) {
                    parcelsArray[i] = parcels.get(i);
                }

                builder.setMultiChoiceItems(parcelsArray, selectedParcels, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            selectedParcelsIndices.add(which);
                            Collections.sort(selectedParcelsIndices);
                        }
                        else {
                            selectedParcelsIndices.remove(Integer.valueOf(which));
                        }
                    }
                });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < selectedParcelsIndices.size(); i++) {
                            stringBuilder.append(parcels.get(selectedParcelsIndices.get(i)));
                            if (i != selectedParcelsIndices.size() - 1) {
                                stringBuilder.append(", ");
                            }
                        }
                        parcelsView.setText(stringBuilder.toString());
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < selectedParcels.length; i++) {
                            selectedParcels[i] = false;
                            selectedParcelsIndices.clear();
                            parcelsView.setText("parcels");
                        }
                    }
                });

                builder.show();
            }
        });
        employeesSpinner.setOnItemSelectedListener(new EmployeesOnClickListener());
        ArrayAdapter<String> employeesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, employees);
        employeesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        employeesSpinner.setAdapter(employeesArrayAdapter);

        jobTypesSpinner.setOnItemSelectedListener(new JobTypesOnClickListener());
        ArrayAdapter<String> jobTypesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, jobTypes);
        jobTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        jobTypesSpinner.setAdapter(jobTypesArrayAdapter);

    }

    private void applyStyling() {
        userView.setTextSize(15f);
        userView.setTypeface(null, Typeface.BOLD);
        userView.setGravity(Gravity.CENTER);

        parcelsView.setGravity(Gravity.CENTER);

        jobTypesSpinner.setGravity(Gravity.CENTER);
        employeesSpinner.setGravity(Gravity.CENTER);
    }

    private void populateDataParcels() {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            JsonArrayRequest request = new JsonArrayRequest(urlParcels, jsonArrayListenerParcels, errorListener);
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateDataEmployees() {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            String filledUrlStaff = urlStaff.replace("{username}", username);
            JsonArrayRequest request = new JsonArrayRequest(filledUrlStaff, jsonArrayListenerStaff, errorListener) {
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

    private Response.Listener<JSONArray> jsonArrayListenerParcels = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            ArrayList<String> data = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject object = response.getJSONObject(i);
                    String parcelID = object.getString("id");

                    data.add(parcelID);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }

            parcels.addAll(data);
            selectedParcels = new boolean[parcels.size()];
        }
    };

    private Response.Listener<JSONArray> jsonArrayListenerStaff = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            ArrayList<String> data = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject object = response.getJSONObject(i);
                    String username = object.getString("username");

                    data.add(username);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }

            employees.addAll(data);
            // Workaround due to data not being loaded yet at initViews();
            employeesSpinner.setOnItemSelectedListener(new EmployeesOnClickListener());
            ArrayAdapter<String> employeesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, employees);
            employeesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            employeesSpinner.setAdapter(employeesArrayAdapter);
        }
    };

    private Response.Listener<String> responseListenerString = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.i("LOG_VOLLEY", response);
        }
    };

    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("LOG_VOLLEY", error.toString());
        }
    };

    private class JobTypesOnClickListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.i("VOLLEY_LOG", "IN JOB TYPES SPINNER");
            //Toast.makeText(getApplicationContext(), jobTypes.get(position), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Ignore.
        }
    }

    private class EmployeesOnClickListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.i("VOLLEY_LOG", "IN EMPLOYEES SPINNER");
            parent.setSelection(position);
            //Toast.makeText(getApplicationContext(), employees.get(position), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Ignore.
        }
    }
}