package com.pip.model;

public enum LojistaStatus {
    ATIVO("ativo", "Ativo"),
    INATIVO("inativo", "Inativo"),
    SUSPENSO("suspenso", "Suspenso"),
    PENDING("pending", "Pendente"),
    ACTIVE("active", "Ativo"),
    SUSPENDED("suspended", "Suspenso"),
    BLOCKED("blocked", "Bloqueado");

    private final String code;
    private final String description;

    LojistaStatus(String code, String description) {
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
