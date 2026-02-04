package com.effective.bank.controller;

import com.effective.bank.dto.CardRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("CardController Integration Tests")
class CardControllerIntegrationTest {

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
    private String adminToken;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .role("ROLE_USER")
                .build();
        userRepository.save(testUser);

        adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role("ROLE_ADMIN")
                .build();
        userRepository.save(adminUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
        userToken = jwtService.generateToken(userDetails);

        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin");
        adminToken = jwtService.generateToken(adminDetails);
    }

    @Test
    @DisplayName("USER видит только свои карты")
    void getCards_AsUser_OnlyOwnCards() throws Exception {
        Card userCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(userCard);

        Card adminCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("9876543210987654"))
                .lastFourDigits("7654")
                .owner(adminUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        cardRepository.save(adminCard);

        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ownerUsername", is("testuser")));
    }

    @Test
    @DisplayName("ADMIN видит все карты")
    void getCards_AsAdmin_AllCards() throws Exception {
        Card userCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(userCard);

        Card adminCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("9876543210987654"))
                .lastFourDigits("7654")
                .owner(adminUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        cardRepository.save(adminCard);

        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("USER может получить свою карту по ID")
    void getCard_AsUser_OwnCard_Success() throws Exception {
        Card card = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(card);

        mockMvc.perform(get("/api/cards/" + card.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(card.getId().intValue())))
                .andExpect(jsonPath("$.ownerUsername", is("testuser")));
    }

    @Test
    @DisplayName("USER не может получить чужую карту")
    void getCard_AsUser_OtherCard_Forbidden() throws Exception {
        Card adminCard = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("9876543210987654"))
                .lastFourDigits("7654")
                .owner(adminUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        cardRepository.save(adminCard);

        mockMvc.perform(get("/api/cards/" + adminCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN может удалить карту с нулевым балансом")
    void deleteCard_AsAdmin_ZeroBalance_Success() throws Exception {
        Card card = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();
        cardRepository.save(card);

        mockMvc.perform(delete("/api/cards/" + card.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Ошибка при удалении карты с ненулевым балансом")
    void deleteCard_NonZeroBalance_Error() throws Exception {
        Card card = Card.builder()
                .cardNumberEncrypted(cardSecurityUtil.encrypt("1234567812345678"))
                .lastFourDigits("5678")
                .owner(testUser)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build();
        cardRepository.save(card);

        mockMvc.perform(delete("/api/cards/" + card.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Админ может создать карту")
    void createCard_AsAdmin_Success() throws Exception {
        CardRequest request = new CardRequest();
        request.setCardNumber("1234567812345678");
        request.setOwnerId(testUser.getId());
        request.setExpirationDate(LocalDate.now().plusYears(3));
        request.setStatus(CardStatus.ACTIVE);
        request.setBalance(BigDecimal.valueOf(1000));

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.cardNumber", containsString("****")))
                .andExpect(jsonPath("$.ownerUsername", is("testuser")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.balance", is(1000)));
    }

    @Test
    @DisplayName("USER не может создать карту")
    void createCard_AsUser_Forbidden() throws Exception {
        CardRequest request = new CardRequest();
        request.setCardNumber("1234567812345678");
        request.setOwnerId(testUser.getId());
        request.setExpirationDate(LocalDate.now().plusYears(3));
        request.setStatus(CardStatus.ACTIVE);
        request.setBalance(BigDecimal.valueOf(1000));

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение карт без токена запрещено")
    void getCards_NoToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isForbidden());
    }
}
