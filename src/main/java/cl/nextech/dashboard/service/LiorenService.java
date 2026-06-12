package cl.nextech.dashboard.service;

import cl.nextech.dashboard.dto.LiorenDteDto;
import cl.nextech.dashboard.entity.Invoice;
import cl.nextech.dashboard.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class LiorenService {

    private final InvoiceRepository invoiceRepo;
    private final WebClient         liorenClient;

    @Value("${app.lioren.tipo-doc:33}")
    private String tipodoc; // 33 = Factura electrónica

    public LiorenService(
        InvoiceRepository invoiceRepo,
        @Value("${app.lioren.base-url:https://cl.lioren.enterprises/api}") String baseUrl,
        @Value("${app.lioren.api-key:pendiente}") String apiKey
    ) {
        this.invoiceRepo  = invoiceRepo;
        this.liorenClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    // ── Consultar DTE por folio ───────────────────────────────────────

    public LiorenDteDto consultarDte(String folio) {
        return consultarDte(tipodoc, folio);
    }

    public LiorenDteDto consultarDte(String tipodocParam, String folio) {
        try {
            return liorenClient.get()
                .uri(u -> u.path("/dtes")
                    .queryParam("tipodoc", tipodocParam)
                    .queryParam("folio",   folio)
                    .build())
                .retrieve()
                .bodyToMono(LiorenDteDto.class)
                .block();
        } catch (Exception e) {
            log.debug("DTE folio {} no encontrado: {}", folio, e.getMessage());
            return null;
        }
    }

    // ── Consultar DTE con PDF incluido (base64) ───────────────────────

    public LiorenDteDto consultarDteConPdf(String folio) {
        try {
            return liorenClient.get()
                .uri(u -> u.path("/dtes")
                    .queryParam("tipodoc", tipodoc)
                    .queryParam("folio",   folio)
                    .queryParam("expects", "pdf")
                    .build())
                .retrieve()
                .bodyToMono(LiorenDteDto.class)
                .block();
        } catch (Exception e) {
            log.error("Error al obtener PDF del DTE folio {}: {}", folio, e.getMessage());
            return null;
        }
    }

    // ── Consultar DTE de una factura del dashboard ────────────────────

    public Map<String, Object> consultarDtePorFactura(Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + invoiceId));

        if (inv.getLiorenFolio() == null || inv.getLiorenFolio().isBlank()) {
            return Map.of(
                "invoiceId", invoiceId,
                "folio",     "",
                "estado",    "sin_dte",
                "mensaje",   "Esta factura no tiene DTE asociado en Lioren"
            );
        }

        LiorenDteDto dte = consultarDte(inv.getLiorenFolio());

        if (dte == null) {
            return Map.of(
                "invoiceId", invoiceId,
                "folio",     inv.getLiorenFolio(),
                "estado",    "error",
                "mensaje",   "No se pudo obtener el DTE desde Lioren"
            );
        }

        return Map.of(
            "invoiceId",  invoiceId,
            "folio",      dte.folio(),
            "tipodoc",    dte.tipodoc(),
            "fecha",      dte.fecha() != null ? dte.fecha() : "",
            "rut",        dte.rut()   != null ? dte.rut()   : "",
            "rs",         dte.rs()    != null ? dte.rs()    : "",
            "montoTotal", dte.montoTotal() != null ? dte.montoTotal() : 0,
            "estado",     dte.estado() != null ? dte.estado() : "desconocido",
            "trackid",    dte.trackid() != null ? dte.trackid() : ""
        );
    }

    // ── Test de conexión ──────────────────────────────────────────────

    public Map<String, Object> testConexion() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> whoami = liorenClient.get()
                .uri("/whoami")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return Map.of(
                "conectado", true,
                "usuario",   whoami != null ? whoami.getOrDefault("nombre", "—") : "—",
                "email",     whoami != null ? whoami.getOrDefault("email",  "—") : "—",
                "mensaje",   "Conexión con Lioren exitosa"
            );
        } catch (Exception e) {
            log.error("Error al conectar con Lioren: {}", e.getMessage());
            return Map.of(
                "conectado", false,
                "mensaje",   "Error al conectar con Lioren: " + e.getMessage()
            );
        }
    }
}
