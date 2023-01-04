package com.example.sfl_is;

import java.util.HashMap;
import java.util.HashSet;

public class Common {
    public static String apiKey;

    public static HashMap<Integer, String> roleIDToName = new HashMap<Integer, String>() {{
        put(1, "Administrator");
        put(2, "Warehouse manager");
        put(3, "Warehouse agent");
        put(4, "Logistics agent");
        put(5, "Delivery driver");
    }};

    public static HashSet<String> allowedRoles = new HashSet<String>() {{
        add("Warehouse manager");
        add("Warehouse agent");
        add("Delivery driver");
    }};

    public static HashMap<String, String> typeIDToName = new HashMap<String, String>() {{
        put("1", "Order processing");
        put("2", "Handover");
        put("3", "Check in");
        put("4", "Check out");
        put("5", "Cargo departing confirmation");
        put("6", "Cargo arrival confirmation");
        put("7", "Delivery cargo confirmation");
        put("8", "Parcel handover");
    }};

    public static HashMap<String, String> typeNameToID = new HashMap<String, String>() {{
        put("Order processing", "1");
        put("Handover", "2");
        put("Check in", "3");
        put("Check out", "4");
        put("Cargo departing confirmation", "5");
        put("Cargo arrival confirmation", "6");
        put("Delivery cargo confirmation", "7");
        put("Parcel handover", "8");
    }};
}
