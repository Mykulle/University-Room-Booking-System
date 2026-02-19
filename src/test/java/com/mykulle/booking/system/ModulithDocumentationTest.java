package com.mykulle.booking.system;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.docs.Documenter;

class ModulithDocumentationTest {

    @Test
    void generatesModuleDocumentation() {
        new Documenter(RoomBookingSystemApplication.class).writeDocumentation();
    }
}
