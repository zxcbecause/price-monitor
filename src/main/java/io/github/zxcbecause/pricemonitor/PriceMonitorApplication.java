package io.github.zxcbecause.pricemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PriceMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceMonitorApplication.class, args);
    }
}
