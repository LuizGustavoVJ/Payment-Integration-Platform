package com.pip.model;

public enum AmbienteApiKey {
    SANDBOX("sandbox", "Ambiente de testes"),
    PRODUCTION("production", "Ambiente de produção");

    private final String code;
    private final String description;

    AmbienteApiKey(String code, String description) {
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
