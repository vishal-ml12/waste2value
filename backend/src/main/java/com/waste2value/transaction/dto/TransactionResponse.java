package com.waste2value.transaction.dto;

import com.waste2value.transaction.Transaction;
import com.waste2value.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long listingId,
        String listingTitle,
        Long pickupRequestId,
        Long generatorId,
        String generatorName,
        Long collectorId,
        String collectorName,
        Long processorId,
        String processorCompanyName,
        BigDecimal quantity,
        String unit,
        BigDecimal agreedPrice,
        BigDecimal totalAmount,
        TransactionStatus status,
        Instant transactionDate,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        String genName = t.getGenerator().getFirstName() + " " + t.getGenerator().getLastName();
        String colName = t.getCollector().getFirstName() + " " + t.getCollector().getLastName();
        Long procId = t.getProcessor() != null ? t.getProcessor().getId() : null;
        String procName = t.getProcessor() != null ? t.getProcessor().getCompanyName() : null;

        return new TransactionResponse(
                t.getId(),
                t.getListing().getId(),
                t.getListing().getTitle(),
                t.getPickupRequest().getId(),
                t.getGenerator().getId(),
                genName,
                t.getCollector().getId(),
                colName,
                procId,
                procName,
                t.getQuantity(),
                t.getUnit(),
                t.getAgreedPrice(),
                t.getTotalAmount(),
                t.getStatus(),
                t.getTransactionDate(),
                t.getCreatedAt()
        );
    }
}
