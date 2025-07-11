package com.example.chartvisualizerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:secret.properties")
public class ChartVisualizerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChartVisualizerBackendApplication.class, args);
	}

}
