package com.effective.bank.service;

import com.effective.bank.dto.TransferRequest;
import com.effective.bank.dto.TransferResponse;
import com.effective.bank.entity.Card;
import com.effective.bank.entity.CardStatus;
import com.effective.bank.entity.Transaction;
import com.effective.bank.exception.CardAccessDeniedException;
import com.effective.bank.exception.CardNotFoundException;
import com.effective.bank.exception.InsufficientFundsException;
import com.effective.bank.repository.CardRepository;
import com.effective.bank.repository.TransactionRepository;
import com.effective.bank.util.CardSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class TransferService {



    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final CardSecurityUtil cardSecurityUtil;

    @Transactional
    public TransferResponse transfer(TransferRequest request, Authentication auth) {


        String username = auth.getName();

        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new IllegalArgumentException("Cannot transfer to the same card");
        }

        Card fromCard = cardRepository.findByIdWithLock(request.getFromCardId())
                .orElseThrow(() -> new CardNotFoundException("From card not found with id: " + request.getFromCardId()));

        Card toCard = cardRepository.findByIdWithLock(request.getToCardId())
                .orElseThrow(() -> new CardNotFoundException("To card not found with id: " + request.getToCardId()));

        if (!fromCard.getOwner().getUsername().equals(username)) {
            throw new CardAccessDeniedException("You don't own the source card");
        }
        if (!toCard.getOwner().getUsername().equals(username)) {
            throw new CardAccessDeniedException("You don't own the destination card");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active (status: " + fromCard.getStatus() + ")");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Destination card is not active (status: " + toCard.getStatus() + ")");
        }

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: %.2f, Requested: %.2f",
                            fromCard.getBalance(), request.getAmount())
            );
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transaction transaction = Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransferResponse.builder()
                .transactionId(savedTransaction.getId())
                .fromCardId(fromCard.getId())
                .fromCardNumber(cardSecurityUtil.mask(fromCard.getCardNumberEncrypted()))
                .toCardId(toCard.getId())
                .toCardNumber(cardSecurityUtil.mask(toCard.getCardNumberEncrypted()))
                .amount(request.getAmount())
                .timestamp(savedTransaction.getCreatedAt())
                .message("Transfer completed successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getCardTransactions(Long cardId, Authentication auth, Pageable pageable) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !card.getOwner().getUsername().equals(username)) {
            throw new CardAccessDeniedException("Access denied to this card's transactions");
        }

        Page<Transaction> transactions = transactionRepository
                .findByFromCardIdOrToCardId(cardId, cardId, pageable);

        return transactions.map(t -> TransferResponse.builder()
                .transactionId(t.getId())
                .fromCardId(t.getFromCard().getId())
                .fromCardNumber(cardSecurityUtil.mask(t.getFromCard().getCardNumberEncrypted()))
                .toCardId(t.getToCard().getId())
                .toCardNumber(cardSecurityUtil.mask(t.getToCard().getCardNumberEncrypted()))
                .amount(t.getAmount())
                .timestamp(t.getCreatedAt())
                .build());
    }
}
