package de.ct.manager.schedule.maintenance;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
@Sql(statements = {
    "DELETE FROM maintenance_inventory_history", "DELETE FROM maintenance_inventory_item_aircraft_type", "DELETE FROM maintenance_inventory_item",
    "DELETE FROM maintenance_task_history", "DELETE FROM maintenance_task_checklist_item", "DELETE FROM maintenance_task",
    "DELETE FROM maintenance_workshop_slot", "DELETE FROM maintenance_aircraft"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MaintenanceWorkshopInventoryIntegrationTest extends AbstractIntegrationTest {

    private static final UserRole ADMIN = UserRole.ROLE_ADMIN;
    private static final UserRole USER = UserRole.ROLE_USER;

    private ResponseEntity<String> call(HttpMethod method, String path, UserRole role, Object body) {
        return restTemplate.exchange(createURLWithPort("maintenance/" + path), method, new HttpEntity<>(body, createHeadersWithToken(role)), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String createAircraft(String type, String registration) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "aircraft", ADMIN, Map.of("type", type, "registration", registration));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).get("id").asText();
    }

    private static Map<String, Object> slot(String aircraftId, LocalDate start, LocalDate end) {
        return Map.of("aircraftId", aircraftId, "startDate", start.toString(), "endDate", end.toString());
    }

    private JsonNode aircraft(String id) throws Exception {
        return json(call(HttpMethod.GET, "aircraft/" + id, USER, null));
    }

    // --- workshop schedule

    @Test
    void testAdministratorsPlanAircraftSeveralTimes() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        String ls8 = createAircraft("LS 8", "D-5678");
        LocalDate today = LocalDate.now();

        Assertions.assertThat(call(HttpMethod.POST, "workshop", USER, slot(ask21, today, today.plusDays(7))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> current = call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, today.minusDays(3), today.plusDays(4)));
        Assertions.assertThat(current.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, today.plusDays(30), today.plusDays(40))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, today.minusDays(60), today.minusDays(50))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        // periods of different aircraft may overlap
        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot(ls8, today, today.plusDays(35))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        JsonNode slots = json(call(HttpMethod.GET, "workshop", USER, null));
        Assertions.assertThat(slots).hasSize(4);
        Assertions.assertThat(slots.get(0).get("startDate").asText()).isEqualTo(today.minusDays(60).toString());

        // the aircraft shows the period running today, not the past or later ones
        JsonNode workshop = aircraft(ask21).get("workshop");
        Assertions.assertThat(workshop.get("id").asText()).isEqualTo(json(current).get("id").asText());
        Assertions.assertThat(workshop.get("endDate").asText()).isEqualTo(today.plusDays(4).toString());

        Assertions.assertThat(call(HttpMethod.DELETE, "workshop/" + workshop.get("id").asText(), ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(aircraft(ask21).get("workshop").get("startDate").asText()).isEqualTo(today.plusDays(30).toString());

        // deleting the aircraft removes its periods
        call(HttpMethod.DELETE, "aircraft/" + ask21, ADMIN, null);
        Assertions.assertThat(json(call(HttpMethod.GET, "workshop", USER, null))).hasSize(1);
    }

    @Test
    void testPeriodsOfOneAircraftMustNotOverlapAndBeValid() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        LocalDate start = LocalDate.of(2026, 11, 7);
        String id = json(call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, start, start.plusDays(14)))).get("id").asText();

        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, start.plusDays(14), start.plusDays(20))).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot(ask21, start.plusDays(10), start.plusDays(5))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(call(HttpMethod.POST, "workshop", ADMIN, slot("unknown", start, start)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // moving a period does not collide with itself
        Map<String, Object> moved = new HashMap<>(slot(ask21, start.plusDays(7), start.plusDays(21)));
        moved.put("note", "  Lackierung  ");
        JsonNode updated = json(call(HttpMethod.PUT, "workshop/" + id, ADMIN, moved));
        Assertions.assertThat(updated.get("startDate").asText()).isEqualTo(start.plusDays(7).toString());
        Assertions.assertThat(updated.get("note").asText()).isEqualTo("Lackierung");
        Assertions.assertThat(call(HttpMethod.PUT, "workshop/" + id, USER, moved).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testProgressCountsChecklistStepsAndTasksWithoutChecklist() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        Map<String, Object> withChecklist = Map.of("title", "Ruder schmieren", "level", "BEGINNER", "checklist", List.of("Reinigen", "Fetten", "Prüfen"));
        Map<String, Object> withoutChecklist = Map.of("title", "Haube polieren", "level", "BEGINNER", "checklist", List.of());
        JsonNode task = json(call(HttpMethod.POST, "aircraft/" + ask21 + "/tasks", ADMIN, withChecklist));
        String plain = json(call(HttpMethod.POST, "aircraft/" + ask21 + "/tasks", ADMIN, withoutChecklist)).get("id").asText();

        Assertions.assertThat(aircraft(ask21).get("stepCount").asInt()).isEqualTo(4);
        Assertions.assertThat(aircraft(ask21).get("stepDoneCount").asInt()).isZero();

        call(HttpMethod.PUT, "tasks/" + task.get("id").asText() + "/checklist/" + task.get("checklist").get(0).get("id").asText(), USER, Map.of("done", true));
        call(HttpMethod.PUT, "tasks/" + plain + "/status", USER, Map.of("status", "DONE"));

        JsonNode progress = aircraft(ask21);
        Assertions.assertThat(progress.get("stepCount").asInt()).isEqualTo(4);
        Assertions.assertThat(progress.get("stepDoneCount").asInt()).isEqualTo(2);
    }

    // --- inventory

    private static Map<String, Object> item(String name, String kind, String counting, Integer quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("kind", kind);
        item.put("counting", counting);
        item.put("quantity", quantity);
        return item;
    }

    @Test
    void testAdministratorsManageItemsAndEveryoneCounts() throws Exception {
        Map<String, Object> request = item("Politur", "CONSUMABLE", "COUNT", 3);
        request.put("unit", "Dosen");
        request.put("minQuantity", 2);
        request.put("location", " Regal A ");
        request.put("aircraftTypes", List.of("ASK 21"));

        Assertions.assertThat(call(HttpMethod.POST, "inventory", USER, request).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> created = call(HttpMethod.POST, "inventory", ADMIN, request);
        Assertions.assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode politur = json(created);
        String id = politur.get("id").asText();
        Assertions.assertThat(politur.get("location").asText()).isEqualTo("Regal A");
        // consumables do not fit aircraft types
        Assertions.assertThat(politur.get("aircraftTypes")).isEmpty();
        Assertions.assertThat(politur.get("status").asText()).isEqualTo("OK");

        JsonNode counted = json(call(HttpMethod.PUT, "inventory/" + id + "/quantity", USER, Map.of("quantity", 2)));
        Assertions.assertThat(counted.get("quantity").asInt()).isEqualTo(2);
        Assertions.assertThat(counted.get("status").asText()).isEqualTo("LOW");
        // a second count of the same user right after replaces the first one in the history
        Assertions.assertThat(json(call(HttpMethod.PUT, "inventory/" + id + "/quantity", USER, Map.of("quantity", 0))).get("status").asText())
                .isEqualTo("EMPTY");
        Assertions.assertThat(call(HttpMethod.PUT, "inventory/" + id + "/quantity", USER, Map.of("quantity", -1)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode history = json(call(HttpMethod.GET, "inventory/" + id + "/history", USER, null));
        Assertions.assertThat(history).hasSize(2);
        Assertions.assertThat(history.get(0).get("action").asText()).isEqualTo("COUNTED");
        Assertions.assertThat(history.get(0).get("quantity").asInt()).isZero();
        Assertions.assertThat(history.get(0).get("actor").get("name").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(history.get(1).get("action").asText()).isEqualTo("CREATED");

        Assertions.assertThat(call(HttpMethod.PUT, "inventory/" + id, USER, request).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "inventory/" + id, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "inventory/" + id, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(json(call(HttpMethod.GET, "inventory", USER, null))).isEmpty();
    }

    @Test
    void testSparePartsFitTypesAndLevelsStayInRange() throws Exception {
        Map<String, Object> request = item("Reifenschlauch 4.00-4", "SPARE_PART", "COUNT", 2);
        request.put("aircraftTypes", List.of("ASK 21", "LS 8"));
        JsonNode part = json(call(HttpMethod.POST, "inventory", ADMIN, request));
        Assertions.assertThat(part.get("aircraftTypes")).hasSize(2);
        Assertions.assertThat(part.get("aircraftTypes").get(0).asText()).isEqualTo("ASK 21");

        request.put("aircraftTypes", List.of("ASK 23"));
        JsonNode updated = json(call(HttpMethod.PUT, "inventory/" + part.get("id").asText(), ADMIN, request));
        Assertions.assertThat(updated.get("aircraftTypes")).hasSize(1);
        Assertions.assertThat(updated.get("aircraftTypes").get(0).asText()).isEqualTo("ASK 23");

        // a fill level defaults to "enough" and ends at 2, unit and minimum are dropped
        Map<String, Object> grease = item("Mehrzweckfett", "CONSUMABLE", "LEVEL", null);
        grease.put("unit", "Dosen");
        grease.put("minQuantity", 1);
        JsonNode level = json(call(HttpMethod.POST, "inventory", ADMIN, grease));
        Assertions.assertThat(level.get("quantity").asInt()).isEqualTo(2);
        Assertions.assertThat(level.get("unit").isNull()).isTrue();
        Assertions.assertThat(level.get("minQuantity").isNull()).isTrue();

        String id = level.get("id").asText();
        Assertions.assertThat(call(HttpMethod.PUT, "inventory/" + id + "/quantity", USER, Map.of("quantity", 3)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(json(call(HttpMethod.PUT, "inventory/" + id + "/quantity", USER, Map.of("quantity", 1))).get("status").asText())
                .isEqualTo("LOW");
        Assertions.assertThat(call(HttpMethod.POST, "inventory", ADMIN, item("Lack", "CONSUMABLE", "LEVEL", 5)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(call(HttpMethod.POST, "inventory", ADMIN, item(" ", "CONSUMABLE", "COUNT", 1)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
