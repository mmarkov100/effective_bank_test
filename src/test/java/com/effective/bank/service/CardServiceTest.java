package com.effective.bank.service;

import com.effective.bank.dto.CardRequest;
import com.effective.bank.dto.CardResponse;
import com.effective.bank.entity.Card;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.entity.User;
import com.effective.bank.exception.CardNotFoundException;
import com.effective.bank.repository.CardRepository;
import com.effective.bank.repository.UserRepository;
import com.effective.bank.util.CardSecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Unit Tests")
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardSecurityUtil cardSecurityUtil;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private CardRequest cardRequest;
    private Card card;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role("ROLE_USER")
                .build();

        cardRequest = new CardRequest();
        cardRequest.setCardNumber("1234567812345678");
        cardRequest.setOwnerId(1L);
        cardRequest.setExpirationDate(LocalDate.now().plusYears(3));
        cardRequest.setStatus(CardStatus.ACTIVE);
        cardRequest.setBalance(BigDecimal.valueOf(1000));

        card = Card.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted123")
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
    }

    @Test
    @DisplayName("Успешное создание карты")
    void createCard_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardSecurityUtil.encrypt("1234567812345678")).thenReturn("encrypted123");
        when(cardSecurityUtil.extractLastFour("1234567812345678")).thenReturn("5678");
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardSecurityUtil.mask("encrypted123")).thenReturn("**** **** **** 5678");

        // Act
        CardResponse response = cardService.createCard(cardRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCardNumber()).isEqualTo("**** **** **** 5678");
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);

        verify(userRepository, times(1)).findById(1L);
        verify(cardRepository, times(1)).save(any(Card.class));
        verify(cardSecurityUtil, times(1)).encrypt("1234567812345678");
    }

    @Test
    @DisplayName("Ошибка: пользователь не найден при создании карты")
    void createCard_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardService.createCard(cardRequest))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешное удаление карты с нулевым балансом")
    void deleteCard_ZeroBalance_Success() {
        // Arrange
        card.setBalance(BigDecimal.ZERO);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // Act
        cardService.deleteCard(1L);

        // Assert
        verify(cardRepository, times(1)).delete(card);
    }

    @Test
    @DisplayName("Ошибка: попытка удалить карту с ненулевым балансом")
    void deleteCard_NonZeroBalance_ThrowsException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // Act & Assert
        assertThatThrownBy(() -> cardService.deleteCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete card with non-zero balance");

        verify(cardRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Ошибка: карта не найдена при удалении")
    void deleteCard_CardNotFound_ThrowsException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardService.deleteCard(1L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");
    }
}
