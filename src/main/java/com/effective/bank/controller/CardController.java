package com.effective.bank.controller;

import com.effective.bank.dto.CardRequest;
import com.effective.bank.dto.CardResponse;
import com.effective.bank.dto.CardUpdateRequest;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.service.CardService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Управление банковскими картами")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать карту", description = "Доступно только администраторам")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardRequest request) {
        log.info("Create Card");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.createCard(request));
    }

    @GetMapping
    @Operation(summary = "Получить список карт", description = "USER видит свои карты, ADMIN — все")
    public ResponseEntity<Page<CardResponse>> getCards(
            Authentication auth,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(cardService.getCards(auth, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить карту по ID", description = "USER видит только свои")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cardService.getCardById(id, auth));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить карту", description = "ADMIN может всё, USER только блокировку")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(cardService.updateCard(id, request, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить карту", description = "Только ADMIN, только при балансе = 0")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
