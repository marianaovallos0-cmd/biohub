package co.edu.unbosque.biohub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "co.edu.unbosque.biohub.comun")
public class BiohubApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiohubApplication.class, args);
    }
}