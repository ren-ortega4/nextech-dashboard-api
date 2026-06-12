package cl.nextech.dashboard.dto;

/** Fila del listado — datos mínimos para la tabla React */
public record InvoiceListItemDto(
    Long    id,
    String  numero,
    String  cliente,
    String  empresa,
    String  rut,
    String  fecha,
    String  mes,
    Double  monto,
    String  status,
    Boolean entregado
) {}
