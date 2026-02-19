package com.mykulle.booking.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic(
		sharedModules = {"com.mykulle.booking.system.useraccount"},
		useFullyQualifiedModuleNames = true
)
@EnableScheduling
@SpringBootApplication
public class RoomBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomBookingSystemApplication.class, args);
	}

}
