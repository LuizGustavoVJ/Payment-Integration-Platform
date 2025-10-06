package com.pip.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO para requisição de autorização de pagamento
 * 
 * @author Luiz Gustavo Finotello
 */
public class AuthorizationRequest {

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private Long amount;

    @NotBlank(message = "A moeda é obrigatória")
    private String currency;

    @NotBlank(message = "O token do cartão é obrigatório")
    private String cardToken;

    private Boolean capture = true;

    private Integer installments = 1;

    private String description;

    private Customer customer;
    
    private String softDescriptor;
    
    private Long serviceTaxAmount;
    
    private String solutionType;
    
    private Boolean saveCard = false;

    // Construtores
    public AuthorizationRequest() {}

    public AuthorizationRequest(Long amount, String currency, String cardToken, Boolean capture) {
        this.amount = amount;
        this.currency = currency;
        this.cardToken = cardToken;
        this.capture = capture;
    }

    // Getters e Setters
    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public Boolean getCapture() {
        return capture;
    }

    public void setCapture(Boolean capture) {
        this.capture = capture;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public String getSoftDescriptor() {
        return softDescriptor;
    }
    
    public void setSoftDescriptor(String softDescriptor) {
        this.softDescriptor = softDescriptor;
    }
    
    public Long getServiceTaxAmount() {
        return serviceTaxAmount;
    }
    
    public void setServiceTaxAmount(Long serviceTaxAmount) {
        this.serviceTaxAmount = serviceTaxAmount;
    }
    
    public String getSolutionType() {
        return solutionType;
    }
    
    public void setSolutionType(String solutionType) {
        this.solutionType = solutionType;
    }
    
    public Boolean getSaveCard() {
        return saveCard;
    }
    
    public void setSaveCard(Boolean saveCard) {
        this.saveCard = saveCard;
    }
    
    public String getCustomerName() {
        return customer != null ? customer.getName() : null;
    }
    
    public String getCustomerEmail() {
        return customer != null ? customer.getEmail() : null;
    }
    
    public String getCustomerDocument() {
        return customer != null ? customer.getDocument() : null;
    }
    
    // Campos adicionais para gateways específicos
    private String externalAuthentication;
    private String initiatedTransactionIndicator;
    private String paymentMethod;
    
    public String getExternalAuthentication() {
        return externalAuthentication;
    }
    
    public void setExternalAuthentication(String externalAuthentication) {
        this.externalAuthentication = externalAuthentication;
    }
    
    public String getInitiatedTransactionIndicator() {
        return initiatedTransactionIndicator;
    }
    
    public void setInitiatedTransactionIndicator(String initiatedTransactionIndicator) {
        this.initiatedTransactionIndicator = initiatedTransactionIndicator;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    // Campos adicionais para Rede
    private String iata;
    private Map<String, Object> threeDS;
    
    public String getIata() {
        return iata;
    }
    
    public void setIata(String iata) {
        this.iata = iata;
    }
    
    public Map<String, Object> getThreeDS() {
        return threeDS;
    }
    
    public void setThreeDS(Map<String, Object> threeDS) {
        this.threeDS = threeDS;
    }
    
    // Campos adicionais para Cielo e outros
    private String cardBrand;
    private String notificationUrl;
    
    public String getCardBrand() {
        return cardBrand;
    }
    
    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }
    
    public String getNotificationUrl() {
        return notificationUrl;
    }
    
    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }
}

