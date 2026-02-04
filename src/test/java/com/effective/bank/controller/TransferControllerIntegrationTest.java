package com.effective.bank.controller;

import com.effective.bank.dto.TransferRequest;
import com.effective.bank.entity.Card;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.entity.User;
import com.effective.bank.repository.CardRepository;
import com.effective.bank.repository.UserRepository;
import com.effective.bank.security.JwtService;
import com.effective.bank.util.CardSecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CardSecurityUtil cardSecurityUtil;

    private String userToken;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .username("testuser_" + System.currentTimeMillis())
                .password(passwordEncoder.encode("password123"))
                .role("ROLE_USER")
                .build();
        testUser = userRepository.save(testUser);

        fromCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        fromCard = cardRepository.save(fromCard);

        toCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("9876543210987654"))
                .lastFourDigits("7654")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        toCard = cardRepository.save(toCard);

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());
        userToken = jwtService.generateToken(userDetails);
    }

    @Test
    @DisplayName("Успешный перевод между своими картами")
    void transfer_SuccessfulTransfer() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(200));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Transfer completed successfully")))
                .andExpect(jsonPath("$.transactionId").exists());

        fromCard = cardRepository.findById(fromCard.getId()).orElseThrow();
        toCard = cardRepository.findById(toCard.getId()).orElseThrow();

        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(toCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    @DisplayName("Ошибка: недостаточно средств")
    void transfer_InsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(2000));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Insufficient funds. Available: 1000,00, Requested: 2000,00")));
    }

    @Test
    @DisplayName("Ошибка: попытка перевода с чужой карты")
    void transfer_OtherUserCard_Forbidden() throws Exception {
        User otherUser = User.builder()
                .username("otheruser")
                .password(passwordEncoder.encode("password"))
                .role("ROLE_USER")
                .build();
        userRepository.save(otherUser);

        Card otherCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1111222233334444"))
                .lastFourDigits("4444")
                .owner(otherUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        cardRepository.save(otherCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(otherCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("don't own")));
    }

    @Test
    @DisplayName("Ошибка: перевод на заблокированную карту (to)")
    void transfer_ToBlockedCard_BadRequest() throws Exception {
        toCard.setStatus(CardStatus.BLOCKED);
        cardRepository.save(toCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Destination card is not active (status: BLOCKED)")));
    }

    @Test
    @DisplayName("Ошибка: перевод с заблокированной карты (from)")
    void transfer_FromBlockedCard_BadRequest() throws Exception {
        fromCard.setStatus(CardStatus.BLOCKED);
        cardRepository.save(fromCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Source card is not active (status: BLOCKED)")));
    }

    @Test
    @DisplayName("Ошибка: перевод на саму себя")
    void transfer_SameCard_BadRequest() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(fromCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same card")));
    }

    @Test
    @DisplayName("Ошибка: некорректный amount (null)")
    void transfer_InvalidAmount_Null_BadRequest() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(null);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка: некорректный amount (отрицательный)")
    void transfer_InvalidAmount_Negative_BadRequest() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(-100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка: несуществующая fromCard")
    void transfer_NonExistentFromCard_NotFound() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(999L);
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ошибка: несуществующая toCard")
    void transfer_NonExistentToCard_NotFound() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(999L);
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Проверка истории переводов")
    void getTransactions_HistoryReturned() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transfers/card/{id}", fromCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount", is(100)));
    }

    @Test
    @DisplayName("Без токена → 403 Forbidden")
    void transfer_NoToken_Unauthorized() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
