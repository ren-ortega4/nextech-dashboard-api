package cl.nextech.dashboard.dto;

public record InvoiceStatsDto(
    long   total,
    long   pendiente,
    long   pagada,
    long   vencida,
    long   revision,
    long   anulada,
    double montoPendiente
) {}
