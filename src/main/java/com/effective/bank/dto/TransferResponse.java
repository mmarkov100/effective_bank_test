package com.effective.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private Long transactionId;
    private Long fromCardId;
    private String fromCardNumber;
    private Long toCardId;
    private String toCardNumber;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String message;
}
