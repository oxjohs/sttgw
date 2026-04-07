package com.oxjohs.sttgw;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SttgwApplication {

	public static void main(String[] args) {
		SpringApplication.run(SttgwApplication.class, args);
	}

}
