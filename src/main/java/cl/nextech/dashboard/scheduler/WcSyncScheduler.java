package cl.nextech.dashboard.scheduler;

import cl.nextech.dashboard.service.WooCommerceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
public class WcSyncScheduler {

    private final WooCommerceService wcService;

    /**
     * Sync incremental cada 15 minutos.
     * En el primer arranque (si no hay datos en DB) se hace fullSync automáticamente.
     */
    @Scheduled(fixedDelayString = "${app.woocommerce.sync-interval-ms:900000}",
               initialDelayString = "${app.woocommerce.sync-initial-delay-ms:30000}")
    public void syncOrders() {
        try {
            int updated = wcService.incrementalSync();
            if (updated > 0) {
                log.info("[Scheduler] Sync completado: {} órdenes actualizadas.", updated);
            }
        } catch (Exception e) {
            log.error("[Scheduler] Error durante sync incremental: {}", e.getMessage(), e);
        }
    }
}
