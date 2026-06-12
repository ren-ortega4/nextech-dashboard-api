package cl.nextech.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NextechDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(NextechDashboardApplication.class, args);
    }
}
