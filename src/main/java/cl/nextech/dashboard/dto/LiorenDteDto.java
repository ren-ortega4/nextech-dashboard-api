package cl.nextech.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Respuesta de GET https://cl.lioren.enterprises/api/dtes?tipodoc=33&folio={folio}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LiorenDteDto(

    Long        id,
    String      tipodoc,
    Integer     folio,
    String      fecha,
    String      rut,
    String      rs,

    @JsonProperty("montoneto")   Double montoNeto,
    @JsonProperty("montoiva")    Double montoIva,
    @JsonProperty("montototal")  Double montoTotal,

    String      estado,
    String      trackid,
    String      pdf,
    String      xml,

    List<LiorenDetalleDto> detalles,
    List<LiorenReferenciaDto> referencias

) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiorenDetalleDto(
        String  nombre,
        Integer cantidad,
        Double  precio,
        Double  descuento
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiorenReferenciaDto(
        @JsonProperty("tipodoc_ref") String tipodocRef,
        @JsonProperty("folio_ref")   String folioRef,
        String razon
    ) {}
}
