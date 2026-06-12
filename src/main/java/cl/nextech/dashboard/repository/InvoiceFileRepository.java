package cl.nextech.dashboard.repository;

import cl.nextech.dashboard.entity.InvoiceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceFileRepository extends JpaRepository<InvoiceFile, UUID> {
    List<InvoiceFile> findByInvoiceIdOrderByUploadedAtAsc(Long invoiceId);
    List<InvoiceFile> findByInvoiceIdOrderByUploadedAtDesc(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
