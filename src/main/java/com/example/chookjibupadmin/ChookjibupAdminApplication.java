package com.example.chookjibupadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChookjibupAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChookjibupAdminApplication.class, args);
	}

}
