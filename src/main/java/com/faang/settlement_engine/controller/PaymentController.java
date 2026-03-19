package com.faang.settlement_engine.controller;

import com.faang.settlement_engine.dto.PaymentRequest;
import com.faang.settlement_engine.service.AccountService;
import com.faang.settlement_engine.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final IdempotencyService idempotencyService;
    private final AccountService accountService;

    @PostMapping("/pay")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request with idempotency key: {}", request.getIdempotencyKey());

        // Check idempotency
        if (idempotencyService.isRequestHandled(request.getIdempotencyKey())) {
            String status = idempotencyService.getStatus(request.getIdempotencyKey());
            log.info("Request with key {} already handled. Status: {}", request.getIdempotencyKey(), status);
            return ResponseEntity.ok(Map.of(
                    "idempotencyKey", request.getIdempotencyKey(),
                    "status", status,
                    "message", "Request already handled."
            ));
        }

        // Mark as processing
        idempotencyService.markAsProcessing(request.getIdempotencyKey());

        try {
            // Process internal transfer
            accountService.processInternalTransfer(request);

            // Mark as completed
            idempotencyService.markAsCompleted(request.getIdempotencyKey());

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "idempotencyKey", request.getIdempotencyKey(),
                    "status", "COMPLETED",
                    "message", "Payment initiated and ledger updated."
            ));
        } catch (Exception e) {
            log.error("Payment processing failed for key {}: {}", request.getIdempotencyKey(), e.getMessage());
            // Need a more robust way to handle failed processing in Redis.
            // Maybe clear the "PROCESSING" status or set it to "FAILED".
            // For now, let's keep it simple.
            throw e; 
        }
    }
}
