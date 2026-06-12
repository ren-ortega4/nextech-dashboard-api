package cl.nextech.dashboard.repository;

import cl.nextech.dashboard.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // ── Listado con filtros ──────────────────────────────────────────

    Page<Invoice> findByNitStatus(String nitStatus, Pageable pageable);

    Page<Invoice> findByMes(String mes, Pageable pageable);

    Page<Invoice> findByNitStatusAndMes(String nitStatus, String mes, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:status IS NULL OR i.nitStatus = :status)
          AND (:mes    IS NULL OR i.mes       = :mes)
          AND (CAST(:search AS String) IS NULL OR
               LOWER(i.cliente) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR
               LOWER(i.empresa) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR
               LOWER(i.numero)  LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR
               LOWER(i.rut)     LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
        ORDER BY i.fechaCreacion DESC
        """)
    Page<Invoice> findWithFilters(
        @Param("status") String status,
        @Param("mes")    String mes,
        @Param("search") String search,
        Pageable pageable
    );

    // ── Stats ─────────────────────────────────────────────────────────

    long countByNitStatus(String nitStatus);

    @Query("SELECT SUM(i.monto) FROM Invoice i WHERE i.nitStatus = 'pendiente'")
    Double sumMontoPendiente();

    // ── Sync WooCommerce ──────────────────────────────────────────────

    @Query("SELECT MAX(i.wcUpdatedAt) FROM Invoice i")
    LocalDateTime findLastWcUpdatedAt();

    @Query("SELECT i.id FROM Invoice i WHERE i.nitStatus = :status")
    List<Long> findIdsByNitStatus(@Param("status") String status);

    @Query("SELECT i.id FROM Invoice i WHERE i.source = :source")
    List<Long> findIdsBySource(@Param("source") String source);

    void deleteBySource(String source);

    // ── Sync Lioren ───────────────────────────────────────────────────

    @Query("SELECT MAX(CAST(i.liorenFolio AS int)) FROM Invoice i WHERE i.source = 'lioren' AND i.liorenFolio IS NOT NULL")
    Integer findMaxLiorenFolio();

    boolean existsByLiorenFolioAndSource(String liorenFolio, String source);
}
