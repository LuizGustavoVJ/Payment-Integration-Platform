package com.pip.model;

/**
 * Enum para eventos de webhook
 */
public enum EventoWebhook {
    PAYMENT_AUTHORIZED("payment.authorized", "Pagamento autorizado"),
    PAYMENT_CAPTURED("payment.captured", "Pagamento capturado"),
    PAYMENT_VOIDED("payment.voided", "Pagamento cancelado"),
    PAYMENT_FAILED("payment.failed", "Pagamento falhou"),
    PAYMENT_REFUNDED("payment.refunded", "Pagamento estornado"),
    ALL("*", "Todos os eventos");

    private final String code;
    private final String description;

    EventoWebhook(String code, String description) {
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
