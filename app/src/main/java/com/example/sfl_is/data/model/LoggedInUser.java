package com.example.sfl_is.data.model;

import com.example.sfl_is.Common;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private String apiKey;
    private String role;

    public LoggedInUser(String userId, String displayName, String apiKey, String roleID) {
        this.userId = userId;
        this.displayName = displayName;
        this.apiKey = apiKey;
        role = Common.roleIDToName.get(Integer.parseInt(roleID));
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }
}