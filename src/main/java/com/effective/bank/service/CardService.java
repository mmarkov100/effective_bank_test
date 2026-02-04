package com.effective.bank.service;

import com.effective.bank.dto.CardRequest;
import com.effective.bank.dto.CardResponse;
import com.effective.bank.dto.CardUpdateRequest;
import com.effective.bank.entity.Card;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.entity.User;
import com.effective.bank.exception.CardAccessDeniedException;
import com.effective.bank.exception.CardNotFoundException;
import com.effective.bank.repository.CardRepository;
import com.effective.bank.repository.UserRepository;
import com.effective.bank.util.CardSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardSecurityUtil cardSecurityUtil;

    @Transactional
    public CardResponse createCard(CardRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String encrypted = cardSecurityUtil.encrypt(request.getCardNumber());
        String lastFour = cardSecurityUtil.extractLastFour(request.getCardNumber());

        Card card = Card.builder()
                .cardNumberEncrypted(encrypted)
                .lastFourDigits(lastFour)
                .owner(owner)
                .expirationDate(request.getExpirationDate())
                .status(request.getStatus())
                .balance(request.getBalance())
                .build();

        Card saved = cardRepository.save(card);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCards(Authentication auth, CardStatus status, Pageable pageable) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<Card> cards;

        if (isAdmin) {
            cards = (status != null)
                    ? cardRepository.findByStatus(status, pageable)
                    : cardRepository.findAll(pageable);
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            cards = (status != null)
                    ? cardRepository.findByOwnerIdAndStatus(user.getId(), status, pageable)
                    : cardRepository.findByOwnerId(user.getId(), pageable);
        }

        return cards.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id, Authentication auth) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        checkAccess(card, auth);
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse updateCard(Long id, CardUpdateRequest request, Authentication auth) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            checkAccess(card, auth);
            if (request.getStatus() != null && request.getStatus() != CardStatus.BLOCKED) {
                throw new CardAccessDeniedException("Users can only request to block their cards");
            }
        }

        if (request.getStatus() != null) {
            card.setStatus(request.getStatus());
        }
        if (request.getBalance() != null) {
            card.setBalance(request.getBalance());
        }

        Card updated = cardRepository.save(card);
        return mapToResponse(updated);
    }
    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        if (card.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete card with non-zero balance");
        }

        cardRepository.delete(card);
    }

    private void checkAccess(Card card, Authentication auth) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !card.getOwner().getUsername().equals(username)) {
            throw new CardAccessDeniedException("Access denied to this card");
        }
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardNumber(cardSecurityUtil.mask(card.getCardNumberEncrypted()))
                .ownerUsername(card.getOwner().getUsername())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
