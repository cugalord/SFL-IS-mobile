package com.example.sfl_is.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    private String role;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayName, String role) {
        this.displayName = displayName;
        this.role = role;
    }

    String getDisplayName() {
        return displayName;
    }

    String getRole() {
        return role;
    }
}