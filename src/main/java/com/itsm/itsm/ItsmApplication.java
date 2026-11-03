package com.itsm.itsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling switches on @Scheduled methods (the SLA checker in SlaMonitor)
@SpringBootApplication
@EnableScheduling
public class ItsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItsmApplication.class, args);
	}

}
