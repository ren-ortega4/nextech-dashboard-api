package cl.nextech.dashboard.controller;

import cl.nextech.dashboard.dto.InvoiceDetailDto;
import cl.nextech.dashboard.dto.InvoiceStatsDto;
import cl.nextech.dashboard.entity.Invoice;
import cl.nextech.dashboard.entity.InvoiceFile;
import cl.nextech.dashboard.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ── Stats ─────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<InvoiceStatsDto> stats(
        @RequestParam(required = false) String source
    ) {
        return ResponseEntity.ok(invoiceService.getStats(source));
    }

    // ── Listado paginado ──────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String mes,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String source,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Invoice> result = invoiceService.listInvoices(status, mes, search, source, page, size);
        return ResponseEntity.ok(Map.of(
            "content",       result.getContent(),
            "totalElements", result.getTotalElements(),
            "totalPages",    result.getTotalPages(),
            "page",          result.getNumber(),
            "size",          result.getSize()
        ));
    }

    // ── Detalle ───────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getDetail(id));
    }

    // ── Cambio de estado ──────────────────────────────────────────────────
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> patch(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body,
        @AuthenticationPrincipal UserDetails user
    ) {
        String  nitStatus  = (String) body.get("nitStatus");
        Boolean entregado  = body.get("entregado") instanceof Boolean b ? b : null;
        String  actor      = user != null ? user.getUsername() : "sistema";

        Invoice updated = invoiceService.updateStatus(id, nitStatus, entregado, actor);
        return ResponseEntity.ok(Map.of(
            "id",        updated.getId(),
            "nitStatus", updated.getNitStatus(),
            "entregado", Boolean.TRUE.equals(updated.getEntregado())
        ));
    }

    // ── Bulk update ───────────────────────────────────────────────────────
    @PatchMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulk(
        @RequestBody Map<String, Object> body,
        @AuthenticationPrincipal UserDetails user
    ) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids"))
            .stream().map(Number::longValue).toList();
        String nitStatus = (String) body.get("nitStatus");
        String actor     = user != null ? user.getUsername() : "sistema";

        int count = invoiceService.bulkUpdateStatus(ids, nitStatus, actor);
        return ResponseEntity.ok(Map.of("updated", count));
    }

    // ── Upload retiro ─────────────────────────────────────────────────────
    @PostMapping(value = "/{id}/retiro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadRetiro(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        String actor = user != null ? user.getUsername() : "sistema";
        InvoiceFile saved = invoiceService.uploadRetiro(id, file, actor);
        return ResponseEntity.ok(Map.of(
            "id",   saved.getId().toString(),
            "name", saved.getName(),
            "size", saved.getSize(),
            "url",  saved.getUrl(),
            "type", saved.getType()
        ));
    }

    // ── Delete retiro ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}/retiro/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteRetiro(
        @PathVariable Long   id,
        @PathVariable UUID   fileId,
        @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        String actor = user != null ? user.getUsername() : "sistema";
        invoiceService.deleteRetiro(id, fileId, actor);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ── Limpieza datos Lioren ─────────────────────────────────────────────
    @DeleteMapping("/lioren")
    public ResponseEntity<Map<String, Object>> deleteLioren() {
        int deleted = invoiceService.deleteLiorenData();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
