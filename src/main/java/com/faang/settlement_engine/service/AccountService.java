package com.faang.settlement_engine.service;

import com.faang.settlement_engine.dto.PaymentRequest;
import com.faang.settlement_engine.dto.TransactionEvent;
import com.faang.settlement_engine.model.Account;
import com.faang.settlement_engine.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private static final String TOPIC_SETTLEMENT_EVENTS = "settlement-events";

    @Retryable(
            retryFor = {OptimisticLockingFailureException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 100, maxDelay = 2000, multiplier = 2.0, random = true)
    )
    @Transactional
    public void processInternalTransfer(PaymentRequest request) {
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        TransactionEvent event = TransactionEvent.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status("LEDGER_UPDATED")
                .build();

        kafkaTemplate.send(TOPIC_SETTLEMENT_EVENTS, event.getIdempotencyKey().toString(), event);
        log.info("Internal transfer completed for key: {}, emitting settlement event", request.getIdempotencyKey());
    }
}
