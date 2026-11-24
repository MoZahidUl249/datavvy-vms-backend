package com.datavvy.vms;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents, maintenance, drivers, assignment and the dashboard end to end
 * (H2 in MySQL mode with the demo seed: 23 documents — 3 expired, 4 expiring
 * soon —, 17 maintenance records, 8 drivers, 5 open assignments). Each test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FleetManagementApiIntegrationTest {

    static final byte[] PDF = "%PDF-1.4\n1 0 obj << >> endobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Dhaka"));

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String manager;
    String viewer;

    @BeforeEach
    void signIn() throws Exception {
        manager = TestAuth.login(mvc, json, "manager@datavvy.com", "Manager@12345");
        viewer = TestAuth.login(mvc, json, "viewer@datavvy.com", "Viewer@12345");
    }

    // ------------------------------------------------------------- documents

    @Nested
    class Documents {

        @Test
        void uploadStoresTheFileAndWorksOutExpiry() throws Exception {
            long v = vehicleId("DHA-METRO-GHA 19-7710");
            JsonNode doc = read(mvc.perform(upload(v, "insurance 2027.pdf", PDF)
                            .param("type", "insurance")
                            .param("documentNumber", "GDIC/MV/2026/1")
                            .param("issueDate", TODAY.minusDays(1).toString())
                            .param("expiryDate", TODAY.plusDays(10).toString())
                            .header("Authorization", manager))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.type").value("INSURANCE"))
                    .andExpect(jsonPath("$.vehicleNumber").value("DHA-METRO-GHA 19-7710"))
                    .andExpect(jsonPath("$.expiryStatus").value("EXPIRING_SOON"))
                    .andExpect(jsonPath("$.daysUntilExpiry").value(10))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileName").value("insurance 2027.pdf"))
                    .andExpect(jsonPath("$.sizeBytes").value(PDF.length)));

            mvc.perform(get("/api/v1/documents/" + doc.get("id").asLong() + "/file").header("Authorization", viewer))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(content().bytes(PDF));
        }

        @Test
        void fileTypeIsCheckedFromTheBytesNotTheName() throws Exception {
            long v = vehicleId("DHA-METRO-GHA 19-7710");
            mvc.perform(upload(v, "virus.pdf", "MZ this is not a pdf".getBytes(StandardCharsets.US_ASCII))
                            .param("type", "REGISTRATION").header("Authorization", manager))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("file"));
            mvc.perform(upload(v, "photo.pdf", PNG).param("type", "REGISTRATION").header("Authorization", manager))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contentType").value("image/png"))
                    .andExpect(jsonPath("$.fileName").value("photo.png"))
                    .andExpect(jsonPath("$.expiryStatus").value("NO_EXPIRY"));
        }

        @Test
        void periodicPapersNeedAnExpiryDateAndOtherNeedsATitle() throws Exception {
            long v = vehicleId("DHA-METRO-GHA 19-7710");
            mvc.perform(upload(v, "tax.pdf", PDF).param("type", "TAX_TOKEN").header("Authorization", manager))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("expiryDate")));
            mvc.perform(upload(v, "x.pdf", PDF).param("type", "OTHER").header("Authorization", manager))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
            mvc.perform(upload(v, "x.pdf", PDF).param("type", "INSURANCE").param("expiryDate", "31/12/2027")
                            .header("Authorization", manager))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("expiryDate"));
        }

        @Test
        void vehicleListPutsExpiredPapersFirst() throws Exception {
            long v = vehicleId("CTG-METRO-TA 12-0931");
            mvc.perform(get("/api/v1/vehicles/" + v + "/documents").header("Authorization", viewer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].type").value("TAX_TOKEN"))
                    .andExpect(jsonPath("$[0].expiryStatus").value("EXPIRED"))
                    .andExpect(jsonPath("$[1].expiryStatus").value("EXPIRING_SOON"))
                    .andExpect(jsonPath("$[3].expiryStatus").value("NO_EXPIRY"));
        }

        @Test
        void fleetListFiltersByExpiryStateAndVehicleNumber() throws Exception {
            mvc.perform(get("/api/v1/documents").param("expiry", "EXPIRED").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.content[*].expiryStatus", everyItem(is("EXPIRED"))));
            mvc.perform(get("/api/v1/documents").param("expiry", "EXPIRED", "EXPIRING_SOON")
                            .header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(7));
            mvc.perform(get("/api/v1/documents").param("q", "ba 11 2087").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.content[0].vehicleNumber").value("DHA-METRO-BA 11-2087"));
            mvc.perform(get("/api/v1/documents").param("type", "INSURANCE").param("size", "2")
                            .header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
            mvc.perform(get("/api/v1/documents/summary").header("Authorization", viewer))
                    .andExpect(jsonPath("$.total").value(23))
                    .andExpect(jsonPath("$.expired").value(3))
                    .andExpect(jsonPath("$.expiringSoon").value(4))
                    .andExpect(jsonPath("$.noExpiry").value(7))
                    .andExpect(jsonPath("$.valid").value(9));
        }

        @Test
        void renewalUpdatesTheExpiryWithOptimisticLocking() throws Exception {
            JsonNode expired = read(mvc.perform(get("/api/v1/documents").param("expiry", "EXPIRED")
                    .param("q", "ta 12 0931").header("Authorization", manager))).get("content").get(0);
            long id = expired.get("id").asLong();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", "TAX_TOKEN");
            body.put("documentNumber", "TT-26-0120931");
            body.put("issueDate", TODAY.toString());
            body.put("expiryDate", TODAY.plusYears(1).toString());
            body.put("version", expired.get("version").asLong());
            send(put("/api/v1/documents/" + id), body)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expiryStatus").value("VALID"));
            send(put("/api/v1/documents/" + id), body) // same (now stale) version again
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("STALE_VERSION"));
            mvc.perform(multipart("/api/v1/documents/" + id + "/file")
                            .file(new MockMultipartFile("file", "renewed.png", "image/png", PNG))
                            .header("Authorization", manager))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileName").value("renewed.png"));
        }

        @Test
        void viewerCanReadButNotUploadOrDelete() throws Exception {
            long v = vehicleId("DHA-METRO-GHA 19-7710");
            mvc.perform(upload(v, "a.pdf", PDF).param("type", "REGISTRATION").header("Authorization", viewer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            long id = read(mvc.perform(get("/api/v1/vehicles/" + v + "/documents").header("Authorization", viewer)))
                    .get(0).get("id").asLong();
            mvc.perform(delete("/api/v1/documents/" + id).header("Authorization", viewer))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/documents/" + id).header("Authorization", manager))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/documents/" + id).header("Authorization", viewer))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
        }

        @Test
        void unknownVehicleIs404() throws Exception {
            mvc.perform(upload(999_999, "a.pdf", PDF).param("type", "REGISTRATION").header("Authorization", manager))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        }
    }

    // ----------------------------------------------------------- maintenance

    @Nested
    class Maintenance {

        @Test
        void recordAndReadHistoryNewestFirst() throws Exception {
            long v = vehicleId("DHA-METRO-BA 11-2087");
            send(post("/api/v1/vehicles/" + v + "/maintenance"),
                    maintenance(TODAY.toString(), "INSPECTION", "1500", "BRTA Mirpur", 316500))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vehicleNumber").value("DHA-METRO-BA 11-2087"))
                    .andExpect(jsonPath("$.cost").value(1500.00))
                    .andExpect(jsonPath("$.createdBy").value("manager@datavvy.com"));
            mvc.perform(get("/api/v1/vehicles/" + v + "/maintenance").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content[0].type").value("INSPECTION"))
                    .andExpect(jsonPath("$.content[1].type").value("BODY_WORK"));
            mvc.perform(get("/api/v1/vehicles/" + v + "/maintenance/summary").header("Authorization", viewer))
                    .andExpect(jsonPath("$.recordCount").value(5))
                    .andExpect(jsonPath("$.totalCost").value(129000.00))
                    .andExpect(jsonPath("$.lastServiceType").value("INSPECTION"))
                    .andExpect(jsonPath("$.lastOdometerKm").value(316500));
        }

        @Test
        void serviceDateCantBeInTheFutureAndOdometerCantGoBackwards() throws Exception {
            long v = vehicleId("DHA-METRO-BA 11-2087");
            send(post("/api/v1/vehicles/" + v + "/maintenance"),
                    maintenance(TODAY.plusDays(2).toString(), "REPAIR", "100", "X", null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("serviceDate"));
            send(post("/api/v1/vehicles/" + v + "/maintenance"),
                    maintenance(TODAY.toString(), "REPAIR", "100", "X", 1000))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("odometerKm"));
            send(post("/api/v1/vehicles/" + v + "/maintenance"),
                    maintenance(TODAY.toString(), "REPAIR", "-5", "", null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("cost")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("vendor")));
        }

        @Test
        void fleetLogSearchesByVehicleNumberVendorAndType() throws Exception {
            mvc.perform(get("/api/v1/maintenance").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(17));
            mvc.perform(get("/api/v1/maintenance").param("q", "hino service").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(3));
            mvc.perform(get("/api/v1/maintenance").param("q", "SYL GA").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(2));
            mvc.perform(get("/api/v1/maintenance").param("type", "ENGINE").param("sort", "cost,desc")
                            .header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].cost").value(85000.00));
        }

        @Test
        void viewerCannotRecordMaintenance() throws Exception {
            long v = vehicleId("DHA-METRO-BA 11-2087");
            mvc.perform(post("/api/v1/vehicles/" + v + "/maintenance").header("Authorization", viewer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(maintenance(TODAY.toString(), "REPAIR", "1", "X", null))))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------ drivers & assignment

    @Nested
    class Drivers {

        @Test
        void createNormalisesPhoneAndRejectsDuplicateLicence() throws Exception {
            send(post("/api/v1/drivers"), driver("Kamal Hossain", "+880 1711-223344", "dk 555 666 c 777"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.phone").value("01711223344"))
                    .andExpect(jsonPath("$.licenseNumber").value("DK 555 666 C 777"))
                    .andExpect(jsonPath("$.licenseStatus").value("VALID"))
                    .andExpect(jsonPath("$.assignedVehicle").value(nullValue()));
            send(post("/api/v1/drivers"), driver("Someone Else", "01811223344", "DK555666C777"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_LICENSE_NUMBER"))
                    .andExpect(jsonPath("$.details.existingName").value("Kamal Hossain"));
            send(post("/api/v1/drivers"), driver("Bad Phone", "12345", "DK000111C222"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
        }

        @Test
        void listFiltersByAssignmentStatusAndSearch() throws Exception {
            mvc.perform(get("/api/v1/drivers").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(8))
                    .andExpect(jsonPath("$.content[0].fullName").value("Abdul Karim"));
            mvc.perform(get("/api/v1/drivers").param("assigned", "true").header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content[*].assignedVehicle.vehicleNumber", hasItem("CTG-METRO-TA 12-0931")));
            mvc.perform(get("/api/v1/drivers").param("assigned", "false").param("status", "ACTIVE")
                            .header("Authorization", viewer))
                    .andExpect(jsonPath("$.totalElements").value(2));
            mvc.perform(get("/api/v1/drivers").param("q", "0171600").header("Authorization", viewer))
                    .andExpect(jsonPath("$.content[0].fullName").value("Rubel Mia"))
                    .andExpect(jsonPath("$.content[0].licenseStatus").value("EXPIRED"));
            mvc.perform(get("/api/v1/drivers/stats").header("Authorization", viewer))
                    .andExpect(jsonPath("$.total").value(8))
                    .andExpect(jsonPath("$.assigned").value(5))
                    .andExpect(jsonPath("$.available").value(2))
                    .andExpect(jsonPath("$.licenseExpired").value(1))
                    .andExpect(jsonPath("$.licenseExpiringSoon").value(1));
        }

        @Test
        void assignChangeAndUnassignKeepHistory() throws Exception {
            long l200 = vehicleId("DHA-METRO-GHA 19-7710");
            long sumon = driverId("Sumon Ahmed");
            long rahim = driverId("Rahim");

            assign(l200, sumon, false).andExpect(status().isOk())
                    .andExpect(jsonPath("$.driver.fullName").value("Sumon Ahmed"))
                    .andExpect(jsonPath("$.assignedBy").value("manager@datavvy.com"));

            // Rahim drives the Hiace: moving him needs reassign=true
            assign(l200, rahim, false).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DRIVER_ASSIGNED_ELSEWHERE"))
                    .andExpect(jsonPath("$.details.vehicleNumber").value("DHA-METRO-GA 11-4582"));
            assign(l200, rahim, true).andExpect(status().isOk())
                    .andExpect(jsonPath("$.driver.fullName").value("Md. Rahim Uddin"));

            long hiace = vehicleId("DHA-METRO-GA 11-4582");
            mvc.perform(get("/api/v1/vehicles/" + hiace + "/assignment").header("Authorization", viewer))
                    .andExpect(jsonPath("$.driver").value(nullValue()));
            mvc.perform(get("/api/v1/vehicles/" + l200 + "/assignment/history").header("Authorization", viewer))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].driverName").value("Md. Rahim Uddin"))
                    .andExpect(jsonPath("$[0].releasedAt").value(nullValue()))
                    .andExpect(jsonPath("$[1].driverName").value("Sumon Ahmed"))
                    .andExpect(jsonPath("$[1].releasedAt").exists());

            mvc.perform(delete("/api/v1/drivers/" + rahim).header("Authorization", manager))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DRIVER_ASSIGNED"));
            mvc.perform(delete("/api/v1/vehicles/" + l200 + "/assignment").header("Authorization", manager))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/drivers/" + rahim).header("Authorization", viewer))
                    .andExpect(jsonPath("$.assignedVehicle").value(nullValue()));
        }

        @Test
        void onlyActiveDriversWithValidLicencesOnUsableVehicles() throws Exception {
            long l200 = vehicleId("DHA-METRO-GHA 19-7710");
            assign(l200, driverId("Rubel Mia"), false).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("DRIVER_LICENSE_EXPIRED"));
            assign(l200, driverId("Habibur"), false).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("DRIVER_NOT_ACTIVE"));
            assign(vehicleId("DHA-METRO-CHA 15-3307"), driverId("Sumon Ahmed"), false)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("VEHICLE_INACTIVE"));
            mvc.perform(put("/api/v1/vehicles/" + l200 + "/assignment").header("Authorization", viewer)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"driverId\":1}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------- vehicle delete + dashboard

    @Test
    void deletingAVehicleRemovesItsRecordsAndFreesItsDriver() throws Exception {
        long bus = vehicleId("DHA-METRO-BA 11-2087");
        mvc.perform(delete("/api/v1/vehicles/" + bus).header("Authorization", manager))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/documents").param("vehicleId", String.valueOf(bus)).header("Authorization", viewer))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/maintenance").param("vehicleId", String.valueOf(bus)).header("Authorization", viewer))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/drivers").param("q", "Nazmul").header("Authorization", viewer))
                .andExpect(jsonPath("$.content[0].assignedVehicle").value(nullValue()));
    }

    @Test
    void dashboardSummarisesDocumentsMaintenanceAndDrivers() throws Exception {
        mvc.perform(get("/api/v1/dashboard/summary").header("Authorization", viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.total").value(8))
                .andExpect(jsonPath("$.kpis.active").value(6))
                .andExpect(jsonPath("$.kpis.maintenance").value(1))
                .andExpect(jsonPath("$.documents.expired").value(3))
                .andExpect(jsonPath("$.documents.expiringSoon").value(4))
                .andExpect(jsonPath("$.documentAlerts.length()").value(5))
                .andExpect(jsonPath("$.documentAlerts[0].expiryStatus").value("EXPIRED"))
                .andExpect(jsonPath("$.maintenance.totalRecords").value(17))
                .andExpect(jsonPath("$.maintenance.byMonth.length()").value(6))
                .andExpect(jsonPath("$.recentMaintenance[0].vehicleNumber").value("DHA-METRO-BA 11-2087"))
                .andExpect(jsonPath("$.drivers.assigned").value(5));
    }

    // ---------------------------------------------------------------- helpers

    private MockHttpServletRequestBuilder upload(long vehicleId, String name, byte[] bytes) {
        return multipart("/api/v1/vehicles/" + vehicleId + "/documents")
                .file(new MockMultipartFile("file", name, "application/octet-stream", bytes));
    }

    private ResultActions assign(long vehicleId, long driverId, boolean reassign) throws Exception {
        return send(put("/api/v1/vehicles/" + vehicleId + "/assignment"),
                Map.of("driverId", driverId, "reassign", reassign));
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return mvc.perform(req.header("Authorization", manager).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private static Map<String, Object> maintenance(String date, String type, String cost, String vendor, Integer odo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serviceDate", date);
        m.put("type", type);
        m.put("cost", new java.math.BigDecimal(cost));
        m.put("vendor", vendor);
        m.put("odometerKm", odo);
        return m;
    }

    private static Map<String, Object> driver(String name, String phone, String licence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fullName", name);
        m.put("phone", phone);
        m.put("licenseNumber", licence);
        m.put("licenseExpiry", TODAY.plusYears(3).toString());
        m.put("status", "ACTIVE");
        return m;
    }

    private long vehicleId(String number) throws Exception {
        return read(mvc.perform(get("/api/v1/vehicles").param("q", number).header("Authorization", manager)))
                .get("content").get(0).get("id").asLong();
    }

    private long driverId(String name) throws Exception {
        return read(mvc.perform(get("/api/v1/drivers").param("q", name).header("Authorization", manager)))
                .get("content").get(0).get("id").asLong();
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
