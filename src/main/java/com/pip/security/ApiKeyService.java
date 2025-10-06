package com.pip.security;

import com.pip.audit.SecurityAuditLogger;
import com.pip.dto.ApiKeyInfo;
import com.pip.dto.ApiKeyValidationResult;
import com.pip.exception.SecurityException;
import com.pip.repository.ApiKeyRepository;
import com.pip.model.ApiKey;
import com.pip.model.StatusApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço de autenticação por API Keys com rotação automática
 * Implementa controles de segurança PCI DSS para autenticação
 */
@Service
public class ApiKeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String LIVE_PREFIX = "pip_live_";
    private static final String TEST_PREFIX = "pip_test_";
    private static final int KEY_LENGTH = 64;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int ROTATION_DAYS = 90;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private SecurityAuditLogger auditLogger;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, AtomicInteger> rateLimitMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> rateLimitResetMap = new ConcurrentHashMap<>();
    
    /**
     * Valida API Key e retorna informações do merchant
     * @param apiKey Chave de API fornecida
     * @return Informações de validação e merchant
     */
    public ApiKeyValidationResult validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            auditLogger.logInvalidApiKeyAttempt(null, "Empty API key");
            return ApiKeyValidationResult.invalid("API key is required");
        }
        
        try {
            // Calcular hash da API key
            String hashedKey = hashApiKey(apiKey);
            
            // Buscar no banco de dados
            ApiKey storedKey = apiKeyRepository.findByKeyHash(hashedKey).orElse(null);
            
            if (storedKey == null) {
                auditLogger.logInvalidApiKeyAttempt(apiKey, "API key not found");
                return ApiKeyValidationResult.invalid("Invalid API key");
            }
            
            // Verificar se a chave está ativa
            if (storedKey.getStatus() != StatusApiKey.ACTIVE) {
                auditLogger.logInvalidApiKeyAttempt(apiKey, "API key is inactive");
                return ApiKeyValidationResult.invalid("API key is inactive");
            }
            
            // Verificar expiração
            if (storedKey.getExpiresAt() != null && toInstant(storedKey.getExpiresAt()).isBefore(Instant.now())) {
                auditLogger.logInvalidApiKeyAttempt(apiKey, "API key expired");
                return ApiKeyValidationResult.invalid("API key expired");
            }
            
            // Verificar rate limiting
            String merchantId = storedKey.getLojista().getId().toString();
            if (!checkRateLimit(merchantId)) {
                auditLogger.logRateLimitExceeded(apiKey, merchantId);
                return ApiKeyValidationResult.rateLimited("Rate limit exceeded");
            }
            
            // Verificar se precisa de rotação
            if (needsRotation(storedKey)) {
                auditLogger.logApiKeyRotationNeeded(merchantId);
                // Notificar merchant sobre necessidade de rotação
                notifyRotationNeeded(merchantId);
            }
            
            // Atualizar último uso
            storedKey.setLastUsedAt(toZonedDateTime(Instant.now()));
            apiKeyRepository.save(storedKey);
            
            // Log de uso bem-sucedido
            auditLogger.logApiKeyUsage(merchantId, apiKey);
            
            // Retornar informações da chave
            ApiKeyInfo keyInfo = new ApiKeyInfo();
            keyInfo.setLojistaId(merchantId);
            keyInfo.setLojistaNome(storedKey.getLojista().getNomeFantasia());
            keyInfo.setKeyId(storedKey.getId().toString());
            keyInfo.setEnvironment(storedKey.getAmbiente().getCode());
            keyInfo.setActive(storedKey.getStatus() == StatusApiKey.ACTIVE);
            keyInfo.setExpiresAt(storedKey.getExpiresAt());
            
            return ApiKeyValidationResult.valid(keyInfo);
            
        } catch (Exception e) {
            auditLogger.logApiKeyValidationError(apiKey, e);
            return ApiKeyValidationResult.error("Validation error occurred");
        }
    }
    
    /**
     * Gera nova API Key para um merchant
     * @param merchantId ID do merchant
     * @param environment Ambiente (LIVE ou TEST)
     * @return Nova API Key gerada
     */
    public String generateApiKey(String merchantId, String environment) {
        try {
            // Gerar chave aleatória
            String apiKey = generateSecureKey(environment);
            
            // Calcular hash para armazenamento
            String hashedKey = hashApiKey(apiKey);
            
            // Criar registro no banco
            ApiKey keyRecord = new ApiKey();
            keyRecord.setKeyHash(hashedKey);
            keyRecord.setEscopo("PAYMENT_PROCESSING,TRANSACTION_QUERY");
            keyRecord.setExpiresAt(toZonedDateTime(Instant.now().plus(ROTATION_DAYS, ChronoUnit.DAYS)));
            
            apiKeyRepository.save(keyRecord);
            
            // Log de criação
            auditLogger.logApiKeyGeneration(merchantId, environment);
            
            return apiKey;
            
        } catch (Exception e) {
            auditLogger.logApiKeyGenerationError(merchantId, environment, e);
            throw new SecurityException("Failed to generate API key", e);
        }
    }
    
    /**
     * Rotaciona API Key existente
     * @param merchantId ID do merchant
     * @param currentApiKey Chave atual
     * @return Nova API Key
     */
    public String rotateApiKey(String merchantId, String currentApiKey) {
        try {
            // Validar chave atual
            ApiKeyValidationResult validation = validateApiKey(currentApiKey);
            if (!validation.isValid()) {
                throw new SecurityException("Cannot rotate invalid API key");
            }
            
            // Desativar chave atual
            String hashedKey = hashApiKey(currentApiKey);
            ApiKey currentKey = apiKeyRepository.findByKeyHash(hashedKey)
                .orElseThrow(() -> new SecurityException("API key not found"));
            currentKey.setStatus(StatusApiKey.REVOKED);
            currentKey.setRotatedAt(toZonedDateTime(Instant.now()));
            apiKeyRepository.save(currentKey);
            
            // Gerar nova chave
            String newApiKey = generateApiKey(merchantId, currentKey.getAmbiente().getCode());
            
            // Log de rotação
            auditLogger.logApiKeyRotation(merchantId, currentApiKey);
            
            return newApiKey;
            
        } catch (Exception e) {
            auditLogger.logApiKeyRotationError(merchantId, e);
            throw new SecurityException("Failed to rotate API key", e);
        }
    }
    
    /**
     * Revoga API Key
     * @param merchantId ID do merchant
     * @param apiKey Chave a ser revogada
     */
    public void revokeApiKey(String merchantId, String apiKey) {
        try {
            String hashedKey = hashApiKey(apiKey);
            ApiKey keyRecord = apiKeyRepository.findByKeyHash(hashedKey).orElse(null);
            
            if (keyRecord != null) {
                keyRecord.setStatus(StatusApiKey.REVOKED);
                keyRecord.setRotatedAt(toZonedDateTime(Instant.now()));
                apiKeyRepository.save(keyRecord);
                
                auditLogger.logApiKeyRevocation(keyRecord.getLojista().getId().toString(), apiKey);
            }
            
        } catch (Exception e) {
            auditLogger.logApiKeyRevocationError(merchantId, e);
            throw new SecurityException("Failed to revoke API key", e);
        }
    }
    
    private String generateSecureKey(String environment) {
        String prefix = "LIVE".equals(environment) ? LIVE_PREFIX : TEST_PREFIX;
        
        // Gerar bytes aleatórios
        byte[] randomBytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        
        // Codificar em Base64 URL-safe
        String randomPart = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
        
        return prefix + randomPart;
    }
    
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes("UTF-8"));
            
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new SecurityException("Failed to hash API key", e);
        }
    }
    
    private boolean checkRateLimit(String merchantId) {
        Instant now = Instant.now();
        Instant resetTime = rateLimitResetMap.get(merchantId);
        
        // Reset contador se passou 1 minuto
        if (resetTime == null || now.isAfter(resetTime)) {
            rateLimitMap.put(merchantId, new AtomicInteger(0));
            rateLimitResetMap.put(merchantId, now.plus(1, ChronoUnit.MINUTES));
        }
        
        // Verificar se está dentro do limite
        AtomicInteger counter = rateLimitMap.get(merchantId);
        if (counter == null) {
            counter = new AtomicInteger(0);
            rateLimitMap.put(merchantId, counter);
        }
        
        return counter.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }
    
    private boolean needsRotation(ApiKey apiKey) {
        ZonedDateTime rotationThreshold = ZonedDateTime.now().minus(ROTATION_DAYS - 7, ChronoUnit.DAYS);
        return apiKey.getCreatedAt() != null && apiKey.getCreatedAt().isBefore(rotationThreshold);
    }
    
    private void notifyRotationNeeded(String merchantId) {
        // Implementar notificação por email/webhook
        logger.warn("API key rotation needed for merchant: {}", merchantId);
        
        // Em produção, enviar notificação real
        // notificationService.sendRotationNotification(merchantId);
    }
    
    private ZonedDateTime toZonedDateTime(Instant instant) {
        return instant.atZone(ZoneId.systemDefault());
    }
    
    private Instant toInstant(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant();
    }
}

