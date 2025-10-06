package com.pip.model;

public enum PlanoLojista {
    STARTER("starter", "Plano Starter", 10000000L, 290, 39L), // R$ 100k, 2.9%, R$ 0.39
    BUSINESS("business", "Plano Business", 50000000L, 250, 29L), // R$ 500k, 2.5%, R$ 0.29
    ENTERPRISE("enterprise", "Plano Enterprise", 200000000L, 190, 19L); // R$ 2M, 1.9%, R$ 0.19

    private final String code;
    private final String nome;
    private final Long limitePadrao; // Em centavos
    private final Integer taxaPercentual; // Em basis points
    private final Long taxaFixa; // Em centavos

    PlanoLojista(String code, String nome, Long limitePadrao, Integer taxaPercentual, Long taxaFixa) {
        this.code = code;
        this.nome = nome;
        this.limitePadrao = limitePadrao;
        this.taxaPercentual = taxaPercentual;
        this.taxaFixa = taxaFixa;
    }
    public String getCode() {
        return code;
    }

    public String getNome() {
        return nome;
    }

    public Long getLimitePadrao() {
        return limitePadrao;
    }

    public Integer getTaxaPercentual() {
        return taxaPercentual;
    }

    public Long getTaxaFixa() {
        return taxaFixa;
    }
}
