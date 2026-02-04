package com.effective.bank.service;

import com.effective.bank.dto.TransferRequest;
import com.effective.bank.dto.TransferResponse;
import com.effective.bank.entity.Card;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.entity.Transaction;
import com.effective.bank.entity.User;
import com.effective.bank.exception.CardAccessDeniedException;
import com.effective.bank.exception.CardNotFoundException;
import com.effective.bank.exception.InsufficientFundsException;
import com.effective.bank.repository.CardRepository;
import com.effective.bank.repository.TransactionRepository;
import com.effective.bank.util.CardSecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardSecurityUtil cardSecurityUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role("ROLE_USER")
                .build();

        fromCard = Card.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted123")
                .lastFourDigits("1234")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumberEncrypted("encrypted456")
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(200));
    }

    @Test
    @DisplayName("Успешный перевод между картами")
    void transfer_Success() {
        when(authentication.getName()).thenReturn("testuser");
        when(cardRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toCard));
        when(cardSecurityUtil.mask(anyString())).thenReturn("**** **** **** 1234");

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(BigDecimal.valueOf(200))
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransferResponse response = transferService.transfer(transferRequest, authentication);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.getMessage()).contains("successfully");

        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(toCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));

        verify(cardRepository, times(1)).findByIdWithLock(1L);
        verify(cardRepository, times(1)).findByIdWithLock(2L);
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Ошибка: перевод на ту же карту")
    void transfer_SameCard_ThrowsException() {
        transferRequest.setToCardId(1L);

        assertThatThrownBy(() -> transferService.transfer(transferRequest, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to the same card");

        verify(cardRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Ошибка: недостаточно средств")
    void transfer_InsufficientFunds_ThrowsException() {
        transferRequest.setAmount(BigDecimal.valueOf(2000));
        when(authentication.getName()).thenReturn("testuser");
        when(cardRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transferService.transfer(transferRequest, authentication))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ошибка: попытка перевода с чужой карты")
    void transfer_NotOwner_ThrowsException() {
        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .build();
        fromCard.setOwner(anotherUser);

        when(authentication.getName()).thenReturn("testuser");
        when(cardRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transferService.transfer(transferRequest, authentication))
                .isInstanceOf(CardAccessDeniedException.class)
                .hasMessageContaining("don't own the source card");
    }

    @Test
    @DisplayName("Ошибка: карта не найдена")
    void transfer_CardNotFound_ThrowsException() {
        when(authentication.getName()).thenReturn("testuser");
        when(cardRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(transferRequest, authentication))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("From card not found");
    }

    @Test
    @DisplayName("Ошибка: перевод с заблокированной карты")
    void transfer_BlockedCard_ThrowsException() {
        fromCard.setStatus(CardStatus.BLOCKED);
        when(authentication.getName()).thenReturn("testuser");
        when(cardRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transferService.transfer(transferRequest, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source card is not active");
    }
}
