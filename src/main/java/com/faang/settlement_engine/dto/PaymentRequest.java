package com.faang.settlement_engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PaymentRequest {
    @NotNull(message = "Idempotency key is required")
    private UUID idempotencyKey;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "From account ID is required")
    private Long fromAccountId;

    @NotNull(message = "To account ID is required")
    private Long toAccountId;
}
