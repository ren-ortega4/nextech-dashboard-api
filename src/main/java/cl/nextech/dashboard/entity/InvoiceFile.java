package cl.nextech.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK → invoices.id */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /** Nombre original del archivo */
    @Column(nullable = false)
    private String name;

    /** Tamaño formateado: "256 KB" */
    private String size;

    /** URL pública para acceder al archivo */
    @Column(nullable = false)
    private String url;

    /** Ruta en disco relativa al upload dir */
    private String path;

    /** "img" | "pdf" */
    @Column(nullable = false)
    private String type;

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
