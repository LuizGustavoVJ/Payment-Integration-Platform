package com.pip.model;

public enum StatusApiKey {
    ACTIVE("active", "Ativa"),
    INACTIVE("inactive", "Inativa"),
    EXPIRED("expired", "Expirada"),
    REVOKED("revoked", "Revogada"),
    SUSPENDED("suspended", "Suspensa");

    private final String code;
    private final String description;

    StatusApiKey(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
