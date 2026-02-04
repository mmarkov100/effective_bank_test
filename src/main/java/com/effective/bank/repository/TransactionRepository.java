package com.effective.bank.repository;

import com.effective.bank.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromCardIdOrToCardId(Long fromCardId, Long toCardId, Pageable pageable);
}
