package com.datavvy.vms;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Auth, catalog and dashboard through HTTP. Vehicle CRUD is in {@link VehicleApiIntegrationTest}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VmsApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@datavvy.com\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void apiRequiresToken() throws Exception {
        mvc.perform(get("/api/v1/vehicles")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogListsVehicleTypesWithUsage() throws Exception {
        String token = TestAuth.login(mvc, json, "admin@datavvy.com", "Admin@12345");
        mvc.perform(get("/api/v1/catalog/vehicle-types").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Microbus")));
    }

    @Test
    void dashboardSummarisesFleet() throws Exception {
        String token = TestAuth.login(mvc, json, "viewer@datavvy.com", "Viewer@12345");
        mvc.perform(get("/api/v1/dashboard/summary").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.total").value(8))
                .andExpect(jsonPath("$.kpis.inactive").value(1))
                .andExpect(jsonPath("$.kpis.maintenance").value(1))
                .andExpect(jsonPath("$.inMaintenance[0].vehicleNumber").value("DHA-METRO-BA 11-2087"));
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        String token = TestAuth.login(mvc, json, "manager@datavvy.com", "Manager@12345");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FLEET_MANAGER"));
    }

}
