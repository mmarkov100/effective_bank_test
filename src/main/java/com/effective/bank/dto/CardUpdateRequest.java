package com.effective.bank.dto;

import com.effective.bank.entity.CardStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardUpdateRequest {
    private CardStatus status;
    
    @DecimalMin(value = "0.0", message = "Balance must be non-negative")
    private BigDecimal balance;
}
