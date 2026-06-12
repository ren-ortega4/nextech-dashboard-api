package cl.nextech.dashboard.service;

import cl.nextech.dashboard.entity.Invoice;
import cl.nextech.dashboard.entity.InvoiceHistory;
import cl.nextech.dashboard.repository.InvoiceHistoryRepository;
import cl.nextech.dashboard.repository.InvoiceRepository;
import cl.nextech.dashboard.wc.WcOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WooCommerceService {

    private final WebClient wcClient;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceHistoryRepository historyRepo;
    private final ObjectMapper objectMapper;

    private static final String[] MONTHS_ES = {
        "", "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    @Value("${app.woocommerce.sync-page-size:100}")
    private int pageSize;

    @Value("${app.woocommerce.consumer-key}")
    private String consumerKey;

    @Value("${app.woocommerce.consumer-secret}")
    private String consumerSecret;

    // ── Sync completo (todas las órdenes) ────────────────────────────

    public int fullSync() {
        log.info("Iniciando sincronización completa con WooCommerce…");
        int page = 1;
        int total = 0;

        while (true) {
            List<WcOrder> orders = fetchPage(page, null);
            if (orders.isEmpty()) break;

            for (WcOrder order : orders) {
                upsertInvoice(order);
                total++;
            }
            log.info("Sync página {}: {} órdenes procesadas (acum: {})", page, orders.size(), total);

            if (orders.size() < pageSize) break;
            page++;
        }

        log.info("Sincronización completa: {} órdenes procesadas.", total);
        return total;
    }

    // ── Sync incremental (solo modificadas desde lastSync) ───────────

    public int incrementalSync() {
        LocalDateTime lastSync = invoiceRepo.findLastWcUpdatedAt();

        // Si no hay nada en DB, hace un fullSync
        if (lastSync == null) {
            return fullSync();
        }

        // Resta 2 min para asegurar que no se pierdan órdenes en el borde
        String afterDate = lastSync.minusMinutes(2)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.debug("Sync incremental desde: {}", afterDate);
        int page = 1;
        int total = 0;

        while (true) {
            List<WcOrder> orders = fetchPage(page, afterDate);
            if (orders.isEmpty()) break;

            for (WcOrder order : orders) {
                upsertInvoice(order);
                total++;
            }

            if (orders.size() < pageSize) break;
            page++;
        }

        if (total > 0) log.info("Sync incremental: {} órdenes actualizadas.", total);
        return total;
    }

    // ── Fetch una página de la API de WC ────────────────────────────

    private List<WcOrder> fetchPage(int page, String modifiedAfter) {
        var builder = wcClient.get()
            .uri(u -> {
                var b = u.path("/orders")
                    .queryParam("per_page", pageSize)
                    .queryParam("page", page)
                    .queryParam("orderby", "modified")
                    .queryParam("order", "desc");
                if (modifiedAfter != null) {
                    b = b.queryParam("modified_after", modifiedAfter);
                }
                return b.build();
            });

        try {
            WcOrder[] result = builder.retrieve()
                .bodyToMono(WcOrder[].class)
                .block();
            return result != null ? List.of(result) : List.of();
        } catch (Exception e) {
            log.error("Error al consultar WC API (página {}): {}", page, e.getMessage());
            return List.of();
        }
    }

    // ── Crear o actualizar una Invoice a partir de un WcOrder ────────

    @Transactional
    public Invoice upsertInvoice(WcOrder wco) {
        boolean isNew = !invoiceRepo.existsById(wco.id());

        Invoice inv = invoiceRepo.findById(wco.id())
            .orElse(Invoice.builder().id(wco.id()).build());

        // ── Datos de billing ─────────────────────────────────────────
        WcOrder.WcBilling billing = wco.billing();
        if (billing != null) {
            inv.setCliente(billing.firstName() + " " + billing.lastName());
            inv.setEmpresa(
                nullIfBlank(wco.getMeta("lioren_rs")) != null
                    ? wco.getMeta("lioren_rs")
                    : billing.company()
            );
            inv.setDireccion(billing.address1());
            inv.setEmail(billing.email());
            inv.setTelefono(billing.phone());
        }

        // ── Meta Lioren ──────────────────────────────────────────────
        inv.setRut(nullIfBlank(wco.getMeta("lioren_rut")));
        inv.setGiro(nullIfBlank(wco.getMeta("lioren_giro")));

        // ── Fechas ───────────────────────────────────────────────────
        LocalDateTime created = parseWcDate(wco.dateCreated());
        LocalDateTime modified = parseWcDate(wco.dateModified());
        inv.setFechaCreacion(created);
        inv.setWcUpdatedAt(modified);
        if (inv.getFechaVencimiento() == null && created != null) {
            inv.setFechaVencimiento(created.plusDays(30));
        }
        if (created != null) {
            inv.setMes(MONTHS_ES[created.getMonthValue()]);
        }

        // ── Número formateado ────────────────────────────────────────
        String year = created != null
            ? String.valueOf(created.getYear())
            : "0000";
        inv.setNumero(String.format("F-%s-%04d", year, wco.id()));

        // ── Monto ────────────────────────────────────────────────────
        try {
            inv.setMonto(Double.parseDouble(wco.total()));
        } catch (NumberFormatException ignored) {
            inv.setMonto(0.0);
        }

        // ── Estado WC ────────────────────────────────────────────────
        inv.setWcStatus(wco.status());

        // ── Estado NIT (solo si no tiene uno ya asignado) ────────────
        if (inv.getNitStatus() == null) {
            inv.setNitStatus("pendiente");
        }

        // ── Items como JSON ──────────────────────────────────────────
        if (wco.lineItems() != null && !wco.lineItems().isEmpty()) {
            inv.setItems(serializeItems(wco.lineItems()));
        }

        inv.setSyncedAt(LocalDateTime.now());
        Invoice saved = invoiceRepo.save(inv);

        // ── Historial: solo al crear ──────────────────────────────────
        if (isNew) {
            historyRepo.save(InvoiceHistory.builder()
                .invoiceId(saved.getId())
                .text("Factura importada desde WooCommerce (estado WC: <strong>"
                    + wco.status() + "</strong>)")
                .actor("sync")
                .build());
        }

        return saved;
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private LocalDateTime parseWcDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // WC devuelve "2024-01-15T10:30:00"
            return LocalDateTime.parse(dateStr.replace("T", "T").substring(0, 19),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String serializeItems(List<WcOrder.WcLineItem> items) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (WcOrder.WcLineItem item : items) {
                double total = parseDouble(item.total());
                int qty = item.quantity() != null ? item.quantity() : 1;
                list.add(Map.of(
                    "desc",  item.name(),
                    "qty",   qty,
                    "price", qty > 0 ? Math.round(total / qty) : 0.0,
                    "total", total
                ));
            }
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }
}
