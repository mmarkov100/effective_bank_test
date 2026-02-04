package com.effective.bank.controller;

import com.effective.bank.dto.TransferRequest;
import com.effective.bank.dto.TransferResponse;
import com.effective.bank.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transfers", description = "Переводы между картами")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Выполнить перевод", description = "Перевод между своими картами")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication auth
    ) {
        log.info("Request: {}", request.toString());
        return ResponseEntity.ok(transferService.transfer(request, auth));
    }

    @GetMapping("/card/{cardId}")
    @Operation(summary = "История переводов карты", description = "Все транзакции с участием карты")
    public ResponseEntity<Page<TransferResponse>> getCardTransactions(
            @PathVariable Long cardId,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        log.info("CardID: {}", cardId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(transferService.getCardTransactions(cardId, auth, pageable));
    }
}
