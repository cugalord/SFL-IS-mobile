package com.example.sfl_is.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.sfl_is.Common;
import com.example.sfl_is.data.model.LoggedInUser;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private RequestQueue requestQueue;
    private String url = "https://sfl-dev.azurewebsites.net/api/v1/Login";

    String responseCode = "";
    String rawData = "";

    public Result<LoggedInUser> login(String username, String password, Context context) {

        requestQueue = Volley.newRequestQueue(context);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("Username", username);
            jsonBody.put("Password", password);

            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    url,
                    responseListener,
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
                        try {
                            rawData = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                        } catch (UnsupportedEncodingException e) {
                            rawData = new String(response.data);
                        }
                        responseCode = String.valueOf(response.statusCode);
                    }
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

            while (responseCode.equals("")) {
                // Loop to wait until response.
            }

            if (!responseCode.equals("202")) {
                return new Result.Error(new IOException("Error logging in"));
            }

            Common.apiKey = rawData.split(";")[0];

            LoggedInUser user =
                    new LoggedInUser(
                            username,
                            username,
                            rawData.split(";")[0],
                            rawData.split(";")[1]);
            return new Result.Success<>(user);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        Common.apiKey = "";
        // TODO: revoke authentication
    }

    private Response.Listener<String> responseListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            responseCode = response;
            Log.i("LOG_VOLLEY", response);
        }
    };

    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            responseCode = "403";
            Log.e("LOG_VOLLEY", error.toString());
        }
    };
}