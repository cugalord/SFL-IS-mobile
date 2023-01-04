package com.example.sfl_is.ui.scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.camera.lifecycle.ProcessCameraProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

//import com.google.android.gms.vision.barcode.Barcode;
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
import com.example.sfl_is.ui.employee.EmployeeActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.sfl_is.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScannerActivity extends AppCompatActivity {

    private final int CAMERA_REQUEST_PERMISSION = 201;
    private final String TAG = "MLKit Barcode";

    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;

    private boolean success;

    private String username = "";
    private String parcelID = "";
    private String jobID = "";
    private String role = "";

    private RequestQueue requestQueue;
    private String urlJobs = "https://sfl-dev.azurewebsites.net/api/v1/Jobs/{id}/{jobStatusID}";
    private String urlParcels = "https://sfl-dev.azurewebsites.net/api/v1/Parcels/{id}";

    private String putResponseCode = "";
    private String putRawData = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        previewView = findViewById(R.id.previewView);

        success = false;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("username");
            parcelID = extras.getString("parcelID");
            jobID = extras.getString("jobID");
            role = extras.getString("role");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    public void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        }
        else {
            getPermission();
        }
    }

    private void getPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (requestCode == CAMERA_REQUEST_PERMISSION) {
            setupCamera();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture =
                ProcessCameraProvider.getInstance(this);

        int lensFacing = CameraSelector.LENS_FACING_BACK;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderListenableFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderListenableFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetRotation(getRotation());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int getRotation() throws NullPointerException {
        return previewView.getDisplay().getRotation();
    }


    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        if (image.getImage() == null) {
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(this::onSuccessListener)
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> image.close());
    }

    private boolean verifying = false;

    private void onSuccessListener(List<Barcode> barcodes) {
        if (barcodes.size() < 1) {
            return;
        }

        Toast.makeText(this, barcodes.get(0).getDisplayValue(), Toast.LENGTH_SHORT).show();

        if (!barcodes.get(0).getDisplayValue().equals(parcelID)) {
            return;
        }

        if (!verifying) {
            verifying = true;
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this::verifyBarcodeValue);
            if (!executorService.isTerminated()) {
                executorService.shutdown();
                try {
                    if (executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                        Toast.makeText(this, "Barcode scanning successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Barcode scanning unsuccessful", Toast.LENGTH_SHORT).show();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (success) {
            Intent i = new Intent(ScannerActivity.this, EmployeeActivity.class);
            i.putExtra("username", username);
            i.putExtra("role", role);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            this.setResult(100);
            this.finish();
        }
    }

    private void verifyBarcodeValue() {
        /*requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            String filledUrlParcels = urlParcels.replace("{id}", username);
            JsonArrayRequest request = new JsonArrayRequest(filledUrlParcels, jsonArrayListenerParcels, errorListener);
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("id", jobID);
            jsonBody.put("jobStatusID", 2);
            String filledUrlJobs = urlJobs.replace("{id}", jobID);
            filledUrlJobs = filledUrlJobs.replace("{jobStatusID}", "2");

            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(
                    Request.Method.PUT,
                    filledUrlJobs,
                    jsonArrayListenerJobs,
                    errorListener) {
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
                        try {
                            putRawData = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                        } catch (UnsupportedEncodingException e) {
                            putRawData = new String(response.data);
                        }
                        putResponseCode = String.valueOf(response.statusCode);
                    }
                    return Response.success(putResponseCode, HttpHeaderParser.parseCacheHeaders(response));
                }

                @Override
                protected VolleyError parseNetworkError(VolleyError volleyError) {
                    putResponseCode = "403";
                    Log.e("LOG_VOLLEY", volleyError.toString());
                    return volleyError;
                }
            };
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response.Listener<JSONArray> jsonArrayListenerParcels = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            success = true;
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("id", jobID);
                jsonBody.put("jobStatusID", 2);
                String filledUrlJobs = urlJobs.replace("{id}", jobID);
                filledUrlJobs = filledUrlJobs.replace("{jobStatusID}", "2");

                final String requestBody = jsonBody.toString();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PUT,
                        filledUrlJobs,
                        jsonArrayListenerJobs,
                        errorListener) {
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
                            try {
                                putRawData = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            } catch (UnsupportedEncodingException e) {
                                putRawData = new String(response.data);
                            }
                            putResponseCode = String.valueOf(response.statusCode);
                        }
                        return Response.success(putResponseCode, HttpHeaderParser.parseCacheHeaders(response));
                    }

                    @Override
                    protected VolleyError parseNetworkError(VolleyError volleyError) {
                        putResponseCode = "403";
                        Log.e("LOG_VOLLEY", volleyError.toString());
                        return volleyError;
                    }
                };
                requestQueue.add(stringRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Response.Listener<String> jsonArrayListenerJobs = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            success = true;
            putResponseCode = response;
            Log.i("LOG_VOLLEY", response);
        }
    };

    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            success = false;
            Log.e("LOG_VOLLEY", error.toString());
        }
    };

}