package com.datavvy.vms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.domain.Vehicle;
import com.datavvy.vms.vehicle.domain.VehicleDetails;
import com.datavvy.vms.vehicle.domain.VehicleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vehicle CRUD end to end: HTTP → security → controller → service → JPA →
 * Flyway schema (H2 in MySQL mode, seeded with 8 demo vehicles). Each test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VehicleApiIntegrationTest {

    private static final String API = "/api/v1/vehicles";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    VehicleRepository repository;

    String manager;

    @BeforeEach
    void signIn() throws Exception {
        manager = TestAuth.login(mvc, json, "manager@datavvy.com", "Manager@12345");
    }

    // ------------------------------------------------------------------ CRUD

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        send(post(API), body("dha metro gha 22 1234"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(API + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.vehicleNumber").value("DHA-METRO-GHA 22-1234"))
                .andExpect(jsonPath("$.company").value("Datavvy Logistics"))
                .andExpect(jsonPath("$.type").value("Microbus"))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Hiace"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.vehicleNumberKey").doesNotExist()); // entity internals stay hidden
    }

    @Test
    void listReturnsAPageAndFilters() throws Exception {
        mvc.perform(get(API).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.content[0].vehicleNumber").exists());
        mvc.perform(get(API).param("status", "MAINTENANCE").header("Authorization", manager))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(API).param("q", "dha metro ga 11").header("Authorization", manager))
                .andExpect(jsonPath("$.content[0].vehicleNumber").value("DHA-METRO-GA 11-4582"));
        mvc.perform(get(API).param("brand", "toyota").header("Authorization", manager))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get(API + "/facets").header("Authorization", manager))
                .andExpect(jsonPath("$.statusCounts.all").value(8))
                .andExpect(jsonPath("$.companies", hasItem("Datavvy Logistics")));
    }

    @Test
    void getUpdateDeleteLifecycle() throws Exception {
        JsonNode created = read(send(post(API), body("RAJ-KA 11-1111")).andExpect(status().isCreated()));
        long id = created.get("id").asLong();

        mvc.perform(get(API + "/" + id).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleNumber").value("RAJ-KA 11-1111"));

        Map<String, Object> change = body("RAJ-KA 11-1111");
        change.put("status", "MAINTENANCE");
        change.put("model", "Hiace GL");
        change.put("version", created.get("version").asLong());
        send(put(API + "/" + id), change)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"))
                .andExpect(jsonPath("$.model").value("Hiace GL"));

        mvc.perform(delete(API + "/" + id).header("Authorization", manager)).andExpect(status().isNoContent());
        mvc.perform(get(API + "/" + id).header("Authorization", manager))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // ------------------------------------------------- duplicate Vehicle Number

    @Test
    void createWithAnExistingVehicleNumberIsRejectedWith409() throws Exception {
        // "DHA-METRO-GA 11-4582" is seeded; same number, different spelling.
        send(post(API), body("dha metro ga 11 4582"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VEHICLE_NUMBER"))
                .andExpect(jsonPath("$.detail").value("A vehicle with number DHA-METRO-GA 11-4582 already exists."))
                .andExpect(jsonPath("$.details.field").value("vehicleNumber"))
                .andExpect(jsonPath("$.details.existingId").isNumber())
                .andExpect(jsonPath("$.detail", not(containsString("SQL"))));
        assertThat(repository.count()).isEqualTo(8);
    }

    @Test
    void updateToAnotherVehiclesNumberIsRejectedWith409() throws Exception {
        JsonNode created = read(send(post(API), body("KHU-THA 33-3333")).andExpect(status().isCreated()));
        Map<String, Object> change = body("DHA-METRO-GA 11-4582");
        change.put("version", created.get("version").asLong());
        send(put(API + "/" + created.get("id").asLong()), change)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VEHICLE_NUMBER"));
    }

    @Test
    void databaseUniqueConstraintBacksTheRule() {
        VehicleDetails same = new VehicleDetails("DHA-METRO-GA 11-4582", "X", "Bus", "Hino", "AK1J",
                VehicleStatus.ACTIVE);
        assertThatThrownBy(() -> repository.saveAndFlush(new Vehicle(same)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------- validation & auth

    @Test
    void missingFieldsReturn400WithFieldErrors() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("vehicleNumber", "");
        bad.put("company", "");
        send(post(API), bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("vehicleNumber")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("company")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("status")));
    }

    @Test
    void badNumberAndUnknownTypeReturn400() throws Exception {
        Map<String, Object> bad = body("DHA-GA 1");
        bad.put("type", "Spaceship");
        send(post(API), bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("vehicleNumber")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("type")));
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        Map<String, Object> bad = body("RNG-HA 44-4444");
        bad.put("status", "BROKEN");
        send(post(API), bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Use one of: ACTIVE, INACTIVE, MAINTENANCE."));
    }

    @Test
    void staleVersionReturns409() throws Exception {
        JsonNode created = read(send(post(API), body("BAR-LA 55-5555")).andExpect(status().isCreated()));
        Map<String, Object> change = body("BAR-LA 55-5555");
        change.put("version", created.get("version").asLong() + 1);
        send(put(API + "/" + created.get("id").asLong()), change)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        String viewer = TestAuth.login(mvc, json, "viewer@datavvy.com", "Viewer@12345");
        mvc.perform(get(API).header("Authorization", viewer)).andExpect(status().isOk());
        mvc.perform(post(API).header("Authorization", viewer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("MYM-MA 66-6666"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mvc.perform(get(API + "/999999").header("Authorization", manager))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
            Map<String, Object> body) throws Exception {
        return mvc.perform(req.header("Authorization", manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static Map<String, Object> body(String vehicleNumber) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("vehicleNumber", vehicleNumber);
        b.put("company", "Datavvy Logistics");
        b.put("type", "Microbus");
        b.put("brand", "Toyota");
        b.put("model", "Hiace");
        b.put("status", "ACTIVE");
        return b;
    }
}
