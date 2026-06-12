package cl.nextech.dashboard.dto;

import java.util.List;

/** Vista de detalle completa */
public record InvoiceDetailDto(
    Long    id,
    String  numero,
    String  cliente,
    String  empresa,
    String  rut,
    String  giro,
    String  direccion,
    String  email,
    String  fecha,
    String  vencimiento,
    String  mes,
    Double  monto,
    Double  neto,
    Double  iva,
    Double  total,
    String  status,
    Boolean entregado,
    List<ItemDto>    items,
    List<HistoryDto> history,
    List<FileDto>    retiroFiles
) {
    public record ItemDto(String desc, Integer qty, Double price, Double total) {}
    public record HistoryDto(String text, String time) {}
    public record FileDto(String id, String name, String size, String url, String type) {}
}
