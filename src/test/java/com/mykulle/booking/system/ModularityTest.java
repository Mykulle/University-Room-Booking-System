package com.mykulle.booking.system;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules.of(RoomBookingSystemApplication.class).verify();
    }
}
