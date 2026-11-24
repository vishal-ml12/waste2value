package com.waste2value.receipt.dto;

import com.waste2value.receipt.ProcessorReceipt;
import com.waste2value.receipt.ProcessorReceiptStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ProcessorReceiptResponse(
        Long id,
        Long processorId,
        String companyName,
        Long collectorId,
        String collectorName,
        Long categoryId,
        String categoryName,
        Long listingId,
        Long pickupRequestId,
        Long transactionId,
        Long generatorId,
        String generatorName,
        BigDecimal quantity,
        String unit,
        BigDecimal agreedPrice,
        BigDecimal totalAmount,
        ProcessorReceiptStatus status,
        String notes,
        Instant receivedDate,
        Instant createdAt
) {
    public static ProcessorReceiptResponse from(ProcessorReceipt r) {
        String colName = r.getCollector() != null 
                ? (r.getCollector().getFirstName() + " " + r.getCollector().getLastName())
                : "Unknown Collector";

        Long txId = r.getTransaction() != null ? r.getTransaction().getId() : null;
        Long genId = null;
        String genName = null;
        if (r.getListing() != null && r.getListing().getGenerator() != null) {
            genId = r.getListing().getGenerator().getId();
            genName = r.getListing().getGenerator().getFirstName() + " " + r.getListing().getGenerator().getLastName();
        } else if (r.getTransaction() != null && r.getTransaction().getGenerator() != null) {
            genId = r.getTransaction().getGenerator().getId();
            genName = r.getTransaction().getGenerator().getFirstName() + " " + r.getTransaction().getGenerator().getLastName();
        }

        return new ProcessorReceiptResponse(
                r.getId(),
                r.getProcessor().getId(),
                r.getProcessor().getCompanyName(),
                r.getCollector() != null ? r.getCollector().getId() : null,
                colName,
                r.getCategory().getId(),
                r.getCategory().getName(),
                r.getListing() != null ? r.getListing().getId() : null,
                r.getPickupRequest() != null ? r.getPickupRequest().getId() : null,
                txId,
                genId,
                genName,
                r.getQuantity(),
                r.getUnit(),
                r.getAgreedPrice(),
                r.getTotalAmount(),
                r.getStatus(),
                r.getNotes(),
                r.getReceivedDate(),
                r.getCreatedAt()
        );
    }
}
