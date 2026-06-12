package cl.nextech.dashboard.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class WebClientConfig {

    @Value("${app.woocommerce.base-url}")
    private String wcBaseUrl;

    @Value("${app.woocommerce.consumer-key}")
    private String consumerKey;

    @Value("${app.woocommerce.consumer-secret}")
    private String consumerSecret;

    @Bean
    public WebClient wcClient() throws SSLException {
        // Acepta certificados auto-firmados (solo para desarrollo local)
        SslContext sslContext = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();

        HttpClient httpClient = HttpClient.create()
            .secure(t -> t.sslContext(sslContext));

        String credentials = consumerKey + ":" + consumerSecret;
        String encoded = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return WebClient.builder()
            .baseUrl(wcBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader("Authorization", "Basic " + encoded)
            .defaultHeader("Content-Type", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
