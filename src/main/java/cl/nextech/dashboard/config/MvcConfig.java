package cl.nextech.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/retiro}")
    private String uploadDir;

    /**
     * Sirve los archivos de retiro en /files/**
     * Ejemplo: GET /files/12345/uuid.pdf
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:" + uploadPath.getParent() + "/");
    }
}
