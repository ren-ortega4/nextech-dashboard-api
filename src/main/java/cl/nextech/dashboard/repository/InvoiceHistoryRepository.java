package cl.nextech.dashboard.repository;

import cl.nextech.dashboard.entity.InvoiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistory, Long> {
    List<InvoiceHistory> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    void deleteByInvoiceIdIn(List<Long> invoiceIds);
}
