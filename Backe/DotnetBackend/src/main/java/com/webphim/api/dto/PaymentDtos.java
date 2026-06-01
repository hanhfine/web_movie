package com.webphim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record SePayWebhookRequest(
            @JsonProperty("id") Long id,
            @JsonProperty("gateway") String gateway,
            @JsonProperty("transactionDate") String transactionDate,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("transferAmount") BigDecimal transferAmount,
            @JsonProperty("content") String content,
            @JsonProperty("referenceCode") String referenceCode) {}
}
