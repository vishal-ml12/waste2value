package com.waste2value.transaction;

import com.waste2value.pickup.PickupRequest;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.transaction.dto.TransactionResponse;
import com.waste2value.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProcessorProfileRepository processorProfileRepository;

    public TransactionService(TransactionRepository transactionRepository, ProcessorProfileRepository processorProfileRepository) {
        this.transactionRepository = transactionRepository;
        this.processorProfileRepository = processorProfileRepository;
    }

    @Transactional
    public Transaction createTransactionForPickup(PickupRequest pickupRequest, BigDecimal actualQuantity) {
        if (transactionRepository.findByPickupRequestId(pickupRequest.getId()).isPresent()) {
            throw new ConflictException("Transaction record already exists for pickup request id: " + pickupRequest.getId());
        }

        BigDecimal price = pickupRequest.getProposedPrice();
        BigDecimal totalAmount = actualQuantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

        Transaction transaction = new Transaction();
        transaction.setListing(pickupRequest.getListing());
        transaction.setPickupRequest(pickupRequest);
        transaction.setGenerator(pickupRequest.getListing().getGenerator());
        transaction.setCollector(pickupRequest.getCollector());
        transaction.setQuantity(actualQuantity);
        transaction.setUnit(pickupRequest.getListing().getUnit());
        transaction.setAgreedPrice(price);
        transaction.setTotalAmount(totalAmount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(Instant.now());

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getMyTransactions(Long userId, Role role, Pageable pageable) {
        if (role == Role.COLLECTOR) {
            return PageResponse.from(transactionRepository.findByCollectorId(userId, pageable).map(TransactionResponse::from));
        } else if (role == Role.GENERATOR) {
            return PageResponse.from(transactionRepository.findByGeneratorId(userId, pageable).map(TransactionResponse::from));
        } else if (role == Role.PROCESSOR) {
            return processorProfileRepository.findByUserId(userId)
                    .map(profile -> PageResponse.from(transactionRepository.findByProcessorId(profile.getId(), pageable).map(TransactionResponse::from)))
                    .orElse(PageResponse.from(org.springframework.data.domain.Page.empty()));
        } else if (role == Role.ADMIN) {
            return PageResponse.from(transactionRepository.findAll(pageable).map(TransactionResponse::from));
        } else {
            return PageResponse.from(transactionRepository.findByCollectorId(userId, pageable).map(TransactionResponse::from));
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long userId, Role role, Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        boolean isAuthorized = role == Role.ADMIN
                || transaction.getGenerator().getId().equals(userId)
                || transaction.getCollector().getId().equals(userId)
                || (transaction.getProcessor() != null && transaction.getProcessor().getUser().getId().equals(userId));

        if (!isAuthorized) {
            throw new ForbiddenException("You are not authorized to view this transaction");
        }

        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        return PageResponse.from(transactionRepository.findAll(pageable).map(TransactionResponse::from));
    }
}
