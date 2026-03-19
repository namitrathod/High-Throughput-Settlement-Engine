package com.faang.settlement_engine.service;

import com.faang.settlement_engine.model.Account;
import com.faang.settlement_engine.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() < 500) {
            log.info("Seeding initial accounts...");
            for (int i = 1; i <= 500; i++) {
                if (!accountRepository.existsById((long) i)) {
                    accountRepository.save(Account.builder()
                            .accountNumber("ACC" + String.format("%03d", i))
                            .balance(new java.math.BigDecimal("1000000.00"))
                            .build());
                }
            }
            log.info("Seeded 500 accounts with IDs 1 to 500.");
        }
    }
}
