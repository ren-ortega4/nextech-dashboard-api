package cl.nextech.dashboard.service;

import cl.nextech.dashboard.dto.LiorenDteDto;
import cl.nextech.dashboard.entity.Invoice;
import cl.nextech.dashboard.entity.InvoiceHistory;
import cl.nextech.dashboard.repository.InvoiceHistoryRepository;
import cl.nextech.dashboard.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiorenSyncService {

    private final LiorenService            liorenService;
    private final InvoiceRepository        invoiceRepo;
    private final InvoiceHistoryRepository historyRepo;

    @Value("${app.lioren.sync-start-folio:1}")
    private int syncStartFolio;

    @Value("${app.lioren.sync-tipodoc:33}")
    private String syncTipodoc;

    private static final String[] MONTHS_ES = {
        "", "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    // ── Sync completo desde folio 1 (o startFolio) ────────────────────

    public int fullSync() {
        log.info("Iniciando sync completo desde Lioren (tipodoc={}, desde folio {})…",
            syncTipodoc, syncStartFolio);

        int folio  = syncStartFolio;
        int total  = 0;
        int noFoundConsecutivos = 0;

        while (noFoundConsecutivos < 3) {
            LiorenDteDto dte = liorenService.consultarDte(syncTipodoc, String.valueOf(folio));

            if (dte == null) {
                noFoundConsecutivos++;
                log.debug("Folio {} no encontrado ({}/3)", folio, noFoundConsecutivos);
            } else {
                noFoundConsecutivos = 0;
                upsertFromDte(dte);
                total++;
                if (total % 100 == 0) {
                    log.info("Sync Lioren: {} DTEs procesados (último folio: {})", total, folio);
                }
            }
            folio++;
        }

        log.info("Sync completo Lioren finalizado: {} DTEs importados.", total);
        return total;
    }

    // ── Sync al arrancar ──────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("Sync inicial Lioren al arrancar…");
        incrementalSync();
    }

    // ── Sync incremental (desde último folio conocido) ─────────────────

    @Scheduled(fixedDelayString = "${app.lioren.sync-interval-ms:300000}")
    public int incrementalSync() {
        Integer lastFolio = invoiceRepo.findMaxLiorenFolio();

        if (lastFolio == null) {
            log.info("No hay DTEs de Lioren en BD — iniciando sync completo.");
            return fullSync();
        }

        int folio  = lastFolio + 1;
        int total  = 0;
        int noFoundConsecutivos = 0;

        log.debug("Sync incremental Lioren desde folio {}", folio);

        while (noFoundConsecutivos < 3) {
            LiorenDteDto dte = liorenService.consultarDte(syncTipodoc, String.valueOf(folio));

            if (dte == null) {
                noFoundConsecutivos++;
            } else {
                noFoundConsecutivos = 0;
                upsertFromDte(dte);
                total++;
            }
            folio++;
        }

        if (total > 0) log.info("Sync incremental Lioren: {} DTEs nuevos.", total);
        return total;
    }

    // ── Crear o actualizar Invoice desde un DTE de Lioren ─────────────

    @Transactional
    public void upsertFromDte(LiorenDteDto dte) {
        // Usamos el id interno de Lioren como PK
        Long invoiceId = dte.id();
        boolean isNew  = !invoiceRepo.existsById(invoiceId);

        Invoice inv = invoiceRepo.findById(invoiceId)
            .orElse(Invoice.builder()
                .id(invoiceId)
                .source("lioren")
                .nitStatus("pendiente")
                .entregado(false)
                .build());

        // ── Datos del receptor ───────────────────────────────────────
        inv.setCliente(dte.rs() != null ? dte.rs() : "Sin nombre");
        inv.setEmpresa(dte.rs());
        inv.setRut(dte.rut());

        // ── Folio y tipo ─────────────────────────────────────────────
        inv.setLiorenFolio(String.valueOf(dte.folio()));
        inv.setLiorenTipodoc(dte.tipodoc());
        inv.setNumero("DTE-" + dte.tipodoc() + "-" + String.format("%05d", dte.folio()));

        // ── Monto ────────────────────────────────────────────────────
        inv.setMonto(dte.montoTotal() != null ? dte.montoTotal() : 0.0);

        // ── Fecha ────────────────────────────────────────────────────
        LocalDateTime fecha = parseFecha(dte.fecha());
        inv.setFechaCreacion(fecha);
        inv.setFechaVencimiento(fecha != null ? fecha.plusDays(30) : null);
        if (fecha != null) {
            inv.setMes(MONTHS_ES[fecha.getMonthValue()]);
        }

        // ── Estado del DTE ───────────────────────────────────────────
        inv.setDteEstado(dte.estado());
        if (inv.getNitStatus() == null) {
            inv.setNitStatus("pendiente");
        }

        // ── Items desde detalles ─────────────────────────────────────
        if (dte.detalles() != null && !dte.detalles().isEmpty()) {
            inv.setItems(buildItemsJson(dte.detalles()));
        }

        inv.setSyncedAt(LocalDateTime.now());
        Invoice saved = invoiceRepo.save(inv);

        // ── Historial: solo al crear ──────────────────────────────────
        if (isNew) {
            historyRepo.save(InvoiceHistory.builder()
                .invoiceId(saved.getId())
                .text("DTE importado desde Lioren — Folio: <strong>"
                    + dte.folio() + "</strong> | Tipo: " + dte.tipodoc())
                .actor("lioren-sync")
                .build());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private LocalDateTime parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildItemsJson(java.util.List<LiorenDteDto.LiorenDetalleDto> detalles) {
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < detalles.size(); i++) {
                LiorenDteDto.LiorenDetalleDto d = detalles.get(i);
                int qty = d.cantidad() != null ? d.cantidad() : 1;
                double price = d.precio() != null ? d.precio() : 0.0;
                sb.append("{")
                  .append("\"desc\":\"").append(escape(d.nombre())).append("\",")
                  .append("\"qty\":").append(qty).append(",")
                  .append("\"price\":").append(price).append(",")
                  .append("\"total\":").append(price * qty)
                  .append("}");
                if (i < detalles.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
