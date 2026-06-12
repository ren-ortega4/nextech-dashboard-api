package cl.nextech.dashboard.controller;

import cl.nextech.dashboard.dto.LiorenDteDto;
import cl.nextech.dashboard.service.LiorenService;
import cl.nextech.dashboard.service.LiorenSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/lioren")
@RequiredArgsConstructor
@Slf4j
public class LiorenController {

    private final LiorenService     liorenService;
    private final LiorenSyncService liorenSyncService;

    /** Test de conexión con Lioren */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(liorenService.testConexion());
    }

    /** Sync completo — importa todos los DTEs desde folio 1 */
    @PostMapping("/sync/full")
    public ResponseEntity<Map<String, Object>> syncFull() {
        log.info("Sync completo Lioren solicitado manualmente");
        int total = liorenSyncService.fullSync();
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "synced",  total,
            "mensaje", "Sync completo Lioren: " + total + " DTEs importados"
        ));
    }

    /** Sync incremental — importa solo DTEs nuevos desde el último folio */
    @PostMapping("/sync/incremental")
    public ResponseEntity<Map<String, Object>> syncIncremental() {
        log.info("Sync incremental Lioren solicitado manualmente");
        int total = liorenSyncService.incrementalSync();
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "synced",  total,
            "mensaje", "Sync incremental Lioren: " + total + " DTEs nuevos"
        ));
    }

    /** Consultar DTE de una factura del dashboard por su invoiceId */
    @GetMapping("/facturas/{id}/dte")
    public ResponseEntity<Map<String, Object>> consultarPorFactura(@PathVariable Long id) {
        return ResponseEntity.ok(liorenService.consultarDtePorFactura(id));
    }

    /** Consultar DTE directamente por folio */
    @GetMapping("/dtes/{folio}")
    public ResponseEntity<LiorenDteDto> consultarPorFolio(@PathVariable String folio) {
        LiorenDteDto dte = liorenService.consultarDte(folio);
        if (dte == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dte);
    }

    /** Consultar DTE con PDF incluido (base64) */
    @GetMapping("/dtes/{folio}/pdf")
    public ResponseEntity<LiorenDteDto> consultarConPdf(@PathVariable String folio) {
        LiorenDteDto dte = liorenService.consultarDteConPdf(folio);
        if (dte == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dte);
    }
}
