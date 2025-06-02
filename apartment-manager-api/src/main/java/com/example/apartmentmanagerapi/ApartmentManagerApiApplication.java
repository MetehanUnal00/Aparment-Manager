package com.example.apartmentmanagerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableTransactionManagement
public class ApartmentManagerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApartmentManagerApiApplication.class, args);
	}

}
