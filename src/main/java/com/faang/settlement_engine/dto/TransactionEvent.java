package com.faang.settlement_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private UUID idempotencyKey;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String status;
}
