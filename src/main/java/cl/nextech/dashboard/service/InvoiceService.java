package cl.nextech.dashboard.service;

import cl.nextech.dashboard.dto.InvoiceDetailDto;
import cl.nextech.dashboard.dto.InvoiceStatsDto;
import cl.nextech.dashboard.entity.Invoice;
import cl.nextech.dashboard.entity.InvoiceFile;
import cl.nextech.dashboard.entity.InvoiceHistory;
import cl.nextech.dashboard.repository.InvoiceFileRepository;
import cl.nextech.dashboard.repository.InvoiceHistoryRepository;
import cl.nextech.dashboard.repository.InvoiceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository      invoiceRepo;
    private final InvoiceHistoryRepository historyRepo;
    private final InvoiceFileRepository  fileRepo;
    private final ObjectMapper           objectMapper;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_STATUSES = Set.of(
        "pendiente", "pagada", "vencida", "revision", "anulada"
    );

    @Value("${app.upload.dir:uploads/retiro}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/files}")
    private String uploadBaseUrl;

    // ── Stats ───────────────────────────────────────────────────────────

    public InvoiceStatsDto getStats() {
        long total      = invoiceRepo.count();
        long pendiente  = invoiceRepo.countByNitStatus("pendiente");
        long pagada     = invoiceRepo.countByNitStatus("pagada");
        long vencida    = invoiceRepo.countByNitStatus("vencida");
        long revision   = invoiceRepo.countByNitStatus("revision");
        long anulada    = invoiceRepo.countByNitStatus("anulada");
        Double rawMonto = invoiceRepo.sumMontoPendiente();
        double montoPendiente = rawMonto != null ? rawMonto : 0.0;

        return new InvoiceStatsDto(total, pendiente, pagada, vencida, revision, anulada, montoPendiente);
    }

    // ── Listado paginado ────────────────────────────────────────────────

    public Page<Invoice> listInvoices(String status, String mes, String search,
                                      int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        return invoiceRepo.findWithFilters(
            (status  != null && !status.isBlank())  ? status  : null,
            (mes     != null && !mes.isBlank())      ? mes     : null,
            (search  != null && !search.isBlank())   ? search.toLowerCase() : null,
            pageable
        );
    }

    // ── Detalle ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InvoiceDetailDto getDetail(Long id) {
        Invoice inv = invoiceRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));

        List<InvoiceHistory> history = historyRepo.findByInvoiceIdOrderByCreatedAtDesc(id);
        List<InvoiceFile>    files   = fileRepo.findByInvoiceIdOrderByUploadedAtDesc(id);

        return toDetailDto(inv, history, files);
    }

    // ── Cambio de estado ─────────────────────────────────────────────────

    @Transactional
    public Invoice updateStatus(Long id, String nitStatus, Boolean entregado, String actor) {
        if (nitStatus != null && !ALLOWED_STATUSES.contains(nitStatus)) {
            throw new IllegalArgumentException(
                "Estado no válido: '" + nitStatus + "'. Los valores permitidos son: " +
                String.join(", ", ALLOWED_STATUSES)
            );
        }

        Invoice inv = invoiceRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));

        StringBuilder logMsg = new StringBuilder();

        if (nitStatus != null && !nitStatus.equals(inv.getNitStatus())) {
            logMsg.append("Estado cambiado de <strong>")
                  .append(inv.getNitStatus())
                  .append("</strong> a <strong>")
                  .append(nitStatus)
                  .append("</strong>");
            inv.setNitStatus(nitStatus);
        }

        if (entregado != null && !entregado.equals(inv.getEntregado())) {
            if (logMsg.length() > 0) logMsg.append(". ");
            logMsg.append("Retiro marcado como <strong>")
                  .append(entregado ? "entregado" : "no entregado")
                  .append("</strong>");
            inv.setEntregado(entregado);
        }

        Invoice saved = invoiceRepo.save(inv);

        if (logMsg.length() > 0) {
            historyRepo.save(InvoiceHistory.builder()
                .invoiceId(id)
                .text(logMsg.toString())
                .actor(actor != null ? actor : "sistema")
                .build());
        }

        return saved;
    }

    // ── Bulk update ──────────────────────────────────────────────────────

    @Transactional
    public int bulkUpdateStatus(List<Long> ids, String nitStatus, String actor) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("La lista de IDs no puede estar vacía");
        }
        if (nitStatus == null || nitStatus.isBlank()) {
            throw new IllegalArgumentException("El campo 'nitStatus' es obligatorio");
        }
        if (!ALLOWED_STATUSES.contains(nitStatus)) {
            throw new IllegalArgumentException(
                "Estado no válido: '" + nitStatus + "'. Los valores permitidos son: " +
                String.join(", ", ALLOWED_STATUSES)
            );
        }

        int count = 0;
        for (Long id : ids) {
            try {
                updateStatus(id, nitStatus, null, actor);
                count++;
            } catch (Exception e) {
                log.warn("No se pudo actualizar factura {}: {}", id, e.getMessage());
            }
        }
        return count;
    }

    // ── Upload retiro ────────────────────────────────────────────────────

    @Transactional
    public InvoiceFile uploadRetiro(Long invoiceId, MultipartFile file, String actor) throws IOException {
        // Validaciones
        if (!invoiceRepo.existsById(invoiceId)) {
            throw new RuntimeException("Factura no encontrada: " + invoiceId);
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido: " + contentType);
        }

        // Guardar en disco
        Path dir = Paths.get(uploadDir, String.valueOf(invoiceId));
        Files.createDirectories(dir);

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        Path   dest     = dir.resolve(filename);
        file.transferTo(dest);

        // Registro en DB
        InvoiceFile record = InvoiceFile.builder()
            .invoiceId(invoiceId)
            .name(file.getOriginalFilename())
            .size(formatSize(file.getSize()))
            .url(uploadBaseUrl + "/" + invoiceId + "/" + filename)
            .path(dest.toString())
            .type(contentType)
            .build();

        InvoiceFile saved = fileRepo.save(record);

        historyRepo.save(InvoiceHistory.builder()
            .invoiceId(invoiceId)
            .text("Archivo subido: <strong>" + file.getOriginalFilename() + "</strong>")
            .actor(actor != null ? actor : "sistema")
            .build());

        return saved;
    }

    // ── Delete retiro ────────────────────────────────────────────────────

    @Transactional
    public void deleteRetiro(Long invoiceId, UUID fileId, String actor) throws IOException {
        InvoiceFile rec = fileRepo.findById(fileId)
            .orElseThrow(() -> new RuntimeException("Archivo no encontrado: " + fileId));

        if (!rec.getInvoiceId().equals(invoiceId)) {
            throw new IllegalArgumentException("El archivo no pertenece a esta factura");
        }

        // Eliminar del disco
        if (rec.getPath() != null) {
            Path p = Paths.get(rec.getPath());
            Files.deleteIfExists(p);
        }

        fileRepo.delete(rec);

        historyRepo.save(InvoiceHistory.builder()
            .invoiceId(invoiceId)
            .text("Archivo eliminado: <strong>" + rec.getName() + "</strong>")
            .actor(actor != null ? actor : "sistema")
            .build());
    }

    // ── Mapper entity → DTO ──────────────────────────────────────────────

    private InvoiceDetailDto toDetailDto(Invoice inv,
                                         List<InvoiceHistory> history,
                                         List<InvoiceFile> files) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        double monto = inv.getMonto() != null ? inv.getMonto() : 0.0;
        double neto  = Math.round(monto / 1.19 * 100.0) / 100.0;
        double iva   = Math.round((monto - neto) * 100.0) / 100.0;

        List<InvoiceDetailDto.ItemDto> itemDtos = parseItems(inv.getItems());

        List<InvoiceDetailDto.HistoryDto> histDtos = history.stream()
            .map(h -> new InvoiceDetailDto.HistoryDto(
                h.getText(),
                h.getCreatedAt() != null ? h.getCreatedAt().format(fmt) : ""
            ))
            .toList();

        List<InvoiceDetailDto.FileDto> fileDtos = files.stream()
            .map(f -> new InvoiceDetailDto.FileDto(
                f.getId().toString(),
                f.getName(),
                f.getSize(),
                f.getUrl(),
                f.getType()
            ))
            .toList();

        return new InvoiceDetailDto(
            inv.getId(),
            inv.getNumero(),
            inv.getCliente(),
            inv.getEmpresa(),
            inv.getRut(),
            inv.getGiro(),
            inv.getDireccion(),
            inv.getEmail(),
            inv.getFechaCreacion() != null ? inv.getFechaCreacion().format(fmt) : null,
            inv.getFechaVencimiento() != null ? inv.getFechaVencimiento().format(fmt) : null,
            inv.getMes(),
            monto,
            neto,
            iva,
            monto,
            inv.getNitStatus(),
            inv.getEntregado(),
            itemDtos,
            histDtos,
            fileDtos
        );
    }

    @SuppressWarnings("unchecked")
    private List<InvoiceDetailDto.ItemDto> parseItems(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(
                json, new TypeReference<>() {});
            return raw.stream()
                .map(m -> new InvoiceDetailDto.ItemDto(
                    (String) m.getOrDefault("desc", ""),
                    ((Number) m.getOrDefault("qty", 1)).intValue(),
                    ((Number) m.getOrDefault("price", 0)).doubleValue(),
                    ((Number) m.getOrDefault("total", 0)).doubleValue()
                ))
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ── Limpieza Lioren ───────────────────────────────────────────────────

    @Transactional
    public int deleteLiorenData() {
        return deleteBySource("lioren");
    }

    // ── Limpieza genérica por fuente ──────────────────────────────────────

    @Transactional
    public int deleteBySource(String source) {
        List<Long> ids = invoiceRepo.findIdsBySource(source);
        if (ids.isEmpty()) return 0;
        historyRepo.deleteByInvoiceIdIn(ids);
        for (Long id : ids) fileRepo.deleteByInvoiceId(id);
        invoiceRepo.deleteBySource(source);
        log.info("Eliminados {} registros de fuente '{}'", ids.size(), source);
        return ids.size();
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
