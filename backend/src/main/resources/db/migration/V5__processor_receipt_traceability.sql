-- V5: Processor Receipt Traceability and Indexes
-- Links processor receipts to the originating transaction for full traceability
-- from Generator -> Collector -> Processor.

ALTER TABLE processor_receipts 
    ADD COLUMN transaction_id BIGINT NULL AFTER pickup_request_id;

ALTER TABLE processor_receipts 
    ADD CONSTRAINT fk_receipt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL;

CREATE INDEX idx_receipt_transaction ON processor_receipts(transaction_id);
CREATE INDEX idx_receipt_listing ON processor_receipts(listing_id);
CREATE INDEX idx_receipt_pickup ON processor_receipts(pickup_request_id);
