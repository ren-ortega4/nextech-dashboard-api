package cl.nextech.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    /** WC Order ID — usamos el mismo como PK para evitar duplicados en sync */
    @Id
    private Long id;

    /** Número formateado: F-2024-0001 */
    @Column(nullable = false)
    private String numero;

    /** Nombre del cliente (billing) */
    private String cliente;

    /** Razón social (meta lioren_rs o billing company) */
    private String empresa;

    /** RUT (meta lioren_rut) */
    private String rut;

    /** Giro comercial (meta lioren_giro) */
    private String giro;

    /** Dirección de facturación */
    private String direccion;

    /** Email del cliente */
    private String email;

    /** Teléfono */
    private String telefono;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaVencimiento;

    /** Nombre del mes en español: "Enero", "Febrero"… */
    private String mes;

    /** Monto total (IVA incluido) */
    @Column(nullable = false)
    private Double monto;

    @Column(name = "nit_status", nullable = false)
    private String nitStatus;

    /** Estado original del DTE en Lioren (acepta, rechaza, etc.) */
    @Column(name = "wc_status")
    private String dteEstado;

    @Builder.Default
    private Boolean entregado = false;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String items;

    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "lioren_folio")
    private String liorenFolio;

    @Column(name = "lioren_tipodoc")
    private String liorenTipodoc;

    @Builder.Default
    @Column(name = "source", nullable = false)
    private String source = "lioren";

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
