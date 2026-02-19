package com.mykulle.booking.system.useraccount.security;

import com.mykulle.booking.system.catalog.application.CatalogManagement;
import com.mykulle.booking.system.catalog.application.RoomDTO;
import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.security.keycloak.client-id=room-booking-backend",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8083/realms/room-booking-backend",
        "spring.datasource.url=jdbc:h2:mem:securitytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityHttpBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogManagement catalogManagement;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void postRooms_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/rooms")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Focus Room",
                                  "roomLocation":"LIB-03-12",
                                  "type":"STUDY_ROOM"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(catalogManagement);
    }

    @Test
    void postRooms_returns403_whenAuthenticatedWithoutStaffRole() throws Exception {
        mockMvc.perform(post("/rooms")
                        .with(jwt().authorities(() -> "ROLE_STUDENT"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Focus Room",
                                  "roomLocation":"LIB-03-12",
                                  "type":"STUDY_ROOM"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalogManagement);
    }

    @Test
    void postRooms_returns201_whenAuthenticatedWithStaffRole() throws Exception {
        given(catalogManagement.addRoom(eq("Focus Room"), eq("LIB-03-12"), eq(CatalogRoom.RoomType.STUDY_ROOM)))
                .willReturn(new RoomDTO(11L, "Focus Room", "LIB-03-12", "STUDY_ROOM", "ENABLED"));

        mockMvc.perform(post("/rooms")
                        .with(jwt().authorities(() -> "ROLE_STAFF"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Focus Room",
                                  "roomLocation":"LIB-03-12",
                                  "type":"STUDY_ROOM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L));
    }
}
