package com.mykulle.booking.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RoomBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomBookingSystemApplication.class, args);
	}

}
