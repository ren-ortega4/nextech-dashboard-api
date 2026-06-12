package cl.nextech.dashboard.wc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Deserialización del endpoint GET /wp-json/wc/v3/orders */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WcOrder(

    Long id,
    String status,

    @JsonProperty("date_created")
    String dateCreated,

    @JsonProperty("date_modified")
    String dateModified,

    String total,
    String currency,

    @JsonProperty("billing")
    WcBilling billing,

    @JsonProperty("line_items")
    List<WcLineItem> lineItems,

    @JsonProperty("meta_data")
    List<WcMeta> metaData

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WcBilling(
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name")  String lastName,
        String company,
        @JsonProperty("address_1")  String address1,
        String city,
        String email,
        String phone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WcLineItem(
        Long id,
        String name,
        Integer quantity,
        String subtotal,
        String total,
        @JsonProperty("product_id") Long productId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WcMeta(
        Long id,
        String key,
        Object value
    ) {}

    /** Extrae el valor de un meta_data por clave */
    public String getMeta(String key) {
        if (metaData == null) return null;
        return metaData.stream()
            .filter(m -> key.equals(m.key()) && m.value() != null)
            .map(m -> m.value().toString())
            .findFirst()
            .orElse(null);
    }
}
