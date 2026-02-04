package com.effective.bank.dto;

import com.effective.bank.entity.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String cardNumber;
    private String ownerUsername;
    private LocalDate expirationDate;
    private CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
