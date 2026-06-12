package cl.nextech.dashboard.controller;

import cl.nextech.dashboard.service.WooCommerceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final WooCommerceService wooCommerceService;

    /**
     * Sincronización completa: trae TODAS las órdenes de WooCommerce.
     * Útil para el primer arranque o para forzar un re-sync total.
     */
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> fullSync() {
        log.info("Full sync solicitado manualmente desde API");
        int total = wooCommerceService.fullSync();
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "synced",  total,
            "message", "Sincronización completa finalizada: " + total + " órdenes procesadas"
        ));
    }

    /**
     * Sincronización incremental: solo órdenes modificadas desde el último sync.
     * Es el que corre automáticamente cada 15 minutos.
     */
    @PostMapping("/incremental")
    public ResponseEntity<Map<String, Object>> incrementalSync() {
        log.info("Sync incremental solicitado manualmente desde API");
        int total = wooCommerceService.incrementalSync();
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "synced",  total,
            "message", "Sync incremental finalizado: " + total + " órdenes actualizadas"
        ));
    }
}
