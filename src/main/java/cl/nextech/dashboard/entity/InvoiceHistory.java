package cl.nextech.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → invoices.id */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /** Texto del evento. Puede incluir HTML básico (<strong>) */
    @Column(nullable = false, length = 500)
    private String text;

    /** Quién realizó el cambio */
    private String actor;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
