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

    /** Fecha de creación de la orden en WC */
    private LocalDateTime fechaCreacion;

    /** Fecha de vencimiento (fechaCreacion + 30 días por defecto) */
    private LocalDateTime fechaVencimiento;

    /** Nombre del mes en español: "Enero", "Febrero"… */
    private String mes;

    /** Monto total (IVA incluido) */
    @Column(nullable = false)
    private Double monto;

    /** Estado NIT: pendiente | pagada | vencida | revision | anulada */
    @Column(name = "nit_status", nullable = false)
    private String nitStatus;

    /** Estado original de WooCommerce (processing, completed, etc.) */
    @Column(name = "wc_status")
    private String wcStatus;

    /** Flag de entrega física del producto */
    @Builder.Default
    private Boolean entregado = false;

    /**
     * Ítems de la orden almacenados como JSONB.
     * Estructura: [{"desc":"...", "qty":1, "price":50000, "total":50000}]
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String items;

    /** Última vez que se actualizó en WC (para sync incremental) */
    private LocalDateTime wcUpdatedAt;

    /** Última sincronización desde WC */
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();

    /** Última modificación en nuestra app */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Folio del DTE en Lioren */
    @Column(name = "lioren_folio")
    private String liorenFolio;

    /** Tipo de documento Lioren: 33=Factura, 39=Boleta */
    @Column(name = "lioren_tipodoc")
    private String liorenTipodoc;

    /** Fuente del registro: woocommerce | lioren */
    @Builder.Default
    @Column(name = "source", nullable = false)
    private String source = "woocommerce";

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
