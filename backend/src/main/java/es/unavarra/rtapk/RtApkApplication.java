package es.unavarra.rtapk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RtApkApplication {

    public static void main(String[] args) {
        SpringApplication.run(RtApkApplication.class, args);
    }
}
