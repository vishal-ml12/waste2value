package com.waste2value.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
