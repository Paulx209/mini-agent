package com.getian.permission;

import lombok.Data;

@Data
public class PermissionDecision {
    private boolean allowed;
    private String message;

    private PermissionDecision(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public static PermissionDecision allow() {
        return new PermissionDecision(true, "Allowed");
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(false, reason);
    }


}
