package de.ct.manager.schedule.maintenance;

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
import org.springframework.test.context.jdbc.SqlMergeMode;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
// method level scripts add to the cleanup instead of replacing it
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(statements = {
    "DELETE FROM maintenance_task_history", "DELETE FROM maintenance_task_note", "DELETE FROM maintenance_task_checklist_item",
    "DELETE FROM maintenance_task", "DELETE FROM maintenance_defect", "DELETE FROM maintenance_template_checklist_item",
    "DELETE FROM maintenance_template_aircraft_type", "DELETE FROM maintenance_template", "DELETE FROM maintenance_aircraft",
    "DELETE FROM auth_user_permissions"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MaintenanceIntegrationTest extends AbstractIntegrationTest {

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

    private Map<String, Object> task(String title, List<String> checklist) {
        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("level", "BEGINNER");
        task.put("checklist", checklist);
        return task;
    }

    private JsonNode createTask(String aircraftId, String title, List<String> checklist) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "aircraft/" + aircraftId + "/tasks", ADMIN, task(title, checklist));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response);
    }

    @Test
    void testOnlyMaintenanceAdministratorsManageAircraft() throws Exception {
        Assertions.assertThat(call(HttpMethod.POST, "aircraft", USER, Map.of("type", "ASK 21", "registration", "D-1234")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        String id = createAircraft("ASK 21", "D-1234");

        ResponseEntity<String> list = call(HttpMethod.GET, "aircraft", USER, null);
        Assertions.assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(json(list)).hasSize(1);
        Assertions.assertThat(json(list).get(0).get("registration").asText()).isEqualTo("D-1234");

        Assertions.assertThat(call(HttpMethod.DELETE, "aircraft/" + id, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "aircraft/" + id, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(json(call(HttpMethod.GET, "aircraft", USER, null))).isEmpty();
    }

    @Test
    void testAnonymousUsersAreRejected() {
        Assertions.assertThat(call(HttpMethod.GET, "aircraft", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Sql(statements = "INSERT INTO auth_user_permissions (user_id, permission) VALUES ('1234', 'PERMISSION_MAINTENANCE_MANAGE')")
    void testTheMaintenanceRightUnlocksAdministration() {
        Assertions.assertThat(call(HttpMethod.POST, "aircraft", USER, Map.of("type", "LS 8", "registration", "D-5678")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(call(HttpMethod.GET, "templates", USER, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testRegistrationsAreUniqueAndPeriodsValid() throws Exception {
        createAircraft("ASK 21", "D-1234");

        Assertions.assertThat(call(HttpMethod.POST, "aircraft", ADMIN, Map.of("type", "ASK 23", "registration", "d-1234")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.POST, "aircraft", ADMIN,
                Map.of("type", "ASK 23", "registration", "D-9999", "workshopStart", "2026-12-12", "workshopEnd", "2026-12-05")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(call(HttpMethod.POST, "aircraft", ADMIN, Map.of("type", " ", "registration", "D-9999")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testTemplatesCreateTasksOnceAndOnlyForMatchingTypes() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        String ls8 = createAircraft("LS 8", "D-5678");

        Map<String, Object> forAll = new HashMap<>(task("Rumpf reinigen", List.of("Innen", "Außen")));
        forAll.put("durationMinutes", 60);
        Map<String, Object> forAsk = new HashMap<>(task("Haube polieren", List.of()));
        forAsk.put("aircraftTypes", List.of("ASK 21"));
        Assertions.assertThat(call(HttpMethod.POST, "templates", ADMIN, forAll).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(call(HttpMethod.POST, "templates", ADMIN, forAsk).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(call(HttpMethod.GET, "templates", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Assertions.assertThat(json(call(HttpMethod.POST, "aircraft/" + ask21 + "/import-templates", ADMIN, null)).get("created").asInt()).isEqualTo(2);
        Assertions.assertThat(json(call(HttpMethod.POST, "aircraft/" + ask21 + "/import-templates", ADMIN, null)).get("created").asInt()).isZero();
        Assertions.assertThat(json(call(HttpMethod.POST, "aircraft/" + ls8 + "/import-templates", ADMIN, null)).get("created").asInt()).isEqualTo(1);

        JsonNode tasks = json(call(HttpMethod.GET, "aircraft/" + ask21 + "/tasks", USER, null));
        Assertions.assertThat(tasks).hasSize(2);
        JsonNode cleaning = tasks.get(tasks.get(0).get("title").asText().equals("Rumpf reinigen") ? 0 : 1);
        Assertions.assertThat(cleaning.get("checklistTotal").asInt()).isEqualTo(2);
        Assertions.assertThat(cleaning.get("durationMinutes").asInt()).isEqualTo(60);
        Assertions.assertThat(cleaning.get("fromTemplate").asBoolean()).isTrue();
        Assertions.assertThat(cleaning.get("status").asText()).isEqualTo("OPEN");

        JsonNode overview = json(call(HttpMethod.GET, "aircraft/" + ask21, USER, null));
        Assertions.assertThat(overview.get("taskCount").asInt()).isEqualTo(2);
        Assertions.assertThat(overview.get("doneCount").asInt()).isZero();
    }

    @Test
    void testDeletingATemplateKeepsItsTasks() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String template = json(call(HttpMethod.POST, "templates", ADMIN, task("Rumpf reinigen", List.of()))).get("id").asText();
        call(HttpMethod.POST, "aircraft/" + aircraft + "/import-templates", ADMIN, null);

        Assertions.assertThat(call(HttpMethod.DELETE, "templates/" + template, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        JsonNode tasks = json(call(HttpMethod.GET, "aircraft/" + aircraft + "/tasks", USER, null));
        Assertions.assertThat(tasks).hasSize(1);
        Assertions.assertThat(tasks.get(0).get("fromTemplate").asBoolean()).isFalse();
    }

    @Test
    void testOnlyMaintenanceAdministratorsCreateTasks() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        Assertions.assertThat(call(HttpMethod.POST, "aircraft/" + aircraft + "/tasks", USER, task("Reifen", List.of())).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        String id = createTask(aircraft, "Reifen", List.of()).get("id").asText();
        Assertions.assertThat(call(HttpMethod.PUT, "tasks/" + id, USER, task("Reifen wechseln", List.of())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "tasks/" + id, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testTaskLifecycleWithSignOff() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String id = createTask(aircraft, "Reifen prüfen", List.of()).get("id").asText();

        Assertions.assertThat(call(HttpMethod.POST, "tasks/" + id + "/finish", USER, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(json(call(HttpMethod.POST, "tasks/" + id + "/start", USER, null)).get("status").asText()).isEqualTo("IN_PROGRESS");

        JsonNode done = json(call(HttpMethod.POST, "tasks/" + id + "/finish", USER, null));
        Assertions.assertThat(done.get("status").asText()).isEqualTo("DONE");
        Assertions.assertThat(done.get("doneBy").get("name").asText()).isEqualTo("test@test.com");

        Assertions.assertThat(call(HttpMethod.POST, "tasks/" + id + "/sign", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode signed = json(call(HttpMethod.POST, "tasks/" + id + "/sign", ADMIN, null));
        Assertions.assertThat(signed.get("signedBy").get("name").asText()).isEqualTo("admin@test.de");

        // signed off tasks are locked, even for administrators, until the sign-off is withdrawn
        Assertions.assertThat(call(HttpMethod.POST, "tasks/" + id + "/reopen", USER, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.POST, "tasks/" + id + "/notes", USER, Map.of("content", "zu spät")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.PUT, "tasks/" + id, ADMIN, task("Neu", List.of())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.DELETE, "tasks/" + id + "/sign", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Assertions.assertThat(json(call(HttpMethod.DELETE, "tasks/" + id + "/sign", ADMIN, null)).get("signedBy").isNull()).isTrue();
        JsonNode reopened = json(call(HttpMethod.POST, "tasks/" + id + "/reopen", USER, null));
        Assertions.assertThat(reopened.get("status").asText()).isEqualTo("IN_PROGRESS");
        Assertions.assertThat(reopened.get("doneBy").isNull()).isTrue();

        List<String> actions = reopened.get("history").findValuesAsText("action");
        Assertions.assertThat(actions).containsExactly("REOPENED", "UNSIGNED", "SIGNED", "FINISHED", "STARTED", "CREATED");

        JsonNode overview = json(call(HttpMethod.GET, "aircraft/" + aircraft, USER, null));
        Assertions.assertThat(overview.get("doneCount").asInt()).isZero();
    }

    @Test
    void testDraggingATaskLogsEveryStepItSkips() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String id = createTask(aircraft, "Reifen prüfen", List.of()).get("id").asText();

        JsonNode done = json(call(HttpMethod.PUT, "tasks/" + id + "/status", USER, Map.of("status", "DONE")));
        Assertions.assertThat(done.get("status").asText()).isEqualTo("DONE");
        Assertions.assertThat(done.get("doneBy").get("name").asText()).isEqualTo("test@test.com");

        JsonNode open = json(call(HttpMethod.PUT, "tasks/" + id + "/status", USER, Map.of("status", "OPEN")));
        Assertions.assertThat(open.get("status").asText()).isEqualTo("OPEN");
        Assertions.assertThat(open.get("doneBy").isNull()).isTrue();
        Assertions.assertThat(open.get("history").findValuesAsText("action"))
                .containsExactly("RESET", "REOPENED", "FINISHED", "STARTED", "CREATED");

        // dropping a task into its own column changes nothing
        Assertions.assertThat(json(call(HttpMethod.PUT, "tasks/" + id + "/status", USER, Map.of("status", "OPEN"))).get("history")).hasSize(5);
        Assertions.assertThat(call(HttpMethod.PUT, "tasks/" + id + "/status", USER, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        call(HttpMethod.PUT, "tasks/" + id + "/status", USER, Map.of("status", "DONE"));
        call(HttpMethod.POST, "tasks/" + id + "/sign", ADMIN, null);
        Assertions.assertThat(call(HttpMethod.PUT, "tasks/" + id + "/status", ADMIN, Map.of("status", "IN_PROGRESS")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void testTheChecklistDrivesTheStatus() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        JsonNode task = createTask(aircraft, "Ruder schmieren", List.of("Reinigen", "Fetten"));
        String id = task.get("id").asText();
        String first = task.get("checklist").get(0).get("id").asText();
        String second = task.get("checklist").get(1).get("id").asText();

        JsonNode started = json(call(HttpMethod.PUT, "tasks/" + id + "/checklist/" + first, USER, Map.of("done", true)));
        Assertions.assertThat(started.get("status").asText()).isEqualTo("IN_PROGRESS");
        Assertions.assertThat(started.get("checklist").get(0).get("doneBy").get("name").asText()).isEqualTo("test@test.com");

        Assertions.assertThat(json(call(HttpMethod.PUT, "tasks/" + id + "/checklist/" + second, USER, Map.of("done", true))).get("status").asText())
                .isEqualTo("DONE");
        Assertions.assertThat(json(call(HttpMethod.PUT, "tasks/" + id + "/checklist/" + first, USER, Map.of("done", false))).get("status").asText())
                .isEqualTo("IN_PROGRESS");

        JsonNode summary = json(call(HttpMethod.GET, "aircraft/" + aircraft + "/tasks", USER, null)).get(0);
        Assertions.assertThat(summary.get("checklistDone").asInt()).isEqualTo(1);
        Assertions.assertThat(summary.get("checklistTotal").asInt()).isEqualTo(2);
    }

    @Test
    void testEditingTheChecklistKeepsTickedSteps() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        JsonNode task = createTask(aircraft, "Ruder schmieren", List.of("Reinigen", "Fetten", "Sichern"));
        String id = task.get("id").asText();
        call(HttpMethod.PUT, "tasks/" + id + "/checklist/" + task.get("checklist").get(1).get("id").asText(), USER, Map.of("done", true));

        JsonNode updated = json(call(HttpMethod.PUT, "tasks/" + id, ADMIN, task("Ruder schmieren", List.of("Fetten", "Prüfen"))));

        Assertions.assertThat(updated.get("checklist")).hasSize(2);
        Assertions.assertThat(updated.get("checklist").get(0).get("text").asText()).isEqualTo("Fetten");
        Assertions.assertThat(updated.get("checklist").get(0).get("done").asBoolean()).isTrue();
        Assertions.assertThat(updated.get("checklist").get(1).get("done").asBoolean()).isFalse();
        Assertions.assertThat(updated.get("history").get(0).get("action").asText()).isEqualTo("UPDATED");
    }

    @Test
    void testANewStepReopensAFinishedTask() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        JsonNode task = createTask(aircraft, "Ruder schmieren", List.of("Fetten"));
        String id = task.get("id").asText();
        call(HttpMethod.PUT, "tasks/" + id + "/checklist/" + task.get("checklist").get(0).get("id").asText(), USER, Map.of("done", true));

        JsonNode updated = json(call(HttpMethod.PUT, "tasks/" + id, ADMIN, task("Ruder schmieren", List.of("Fetten", "Sichern"))));

        Assertions.assertThat(updated.get("status").asText()).isEqualTo("IN_PROGRESS");
        Assertions.assertThat(updated.get("history").get(0).get("action").asText()).isEqualTo("REOPENED");
    }

    @Test
    void testNotesCanBeDeletedByTheirAuthorAndAdministrators() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String id = createTask(aircraft, "Reifen", List.of()).get("id").asText();

        JsonNode withNote = json(call(HttpMethod.POST, "tasks/" + id + "/notes", ADMIN, Map.of("content", "  Profil knapp  ")));
        JsonNode adminNote = withNote.get("notes").get(0);
        Assertions.assertThat(adminNote.get("content").asText()).isEqualTo("Profil knapp");

        JsonNode asUser = json(call(HttpMethod.GET, "tasks/" + id, USER, null));
        Assertions.assertThat(asUser.get("notes").get(0).get("deletable").asBoolean()).isFalse();
        Assertions.assertThat(call(HttpMethod.DELETE, "tasks/" + id + "/notes/" + adminNote.get("id").asText(), USER, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode userNote = json(call(HttpMethod.POST, "tasks/" + id + "/notes", USER, Map.of("content", "Neuer Reifen bestellt"))).get("notes").get(1);
        Assertions.assertThat(userNote.get("deletable").asBoolean()).isTrue();
        Assertions.assertThat(call(HttpMethod.POST, "tasks/" + id + "/notes", USER, Map.of("content", " ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Assertions.assertThat(json(call(HttpMethod.DELETE, "tasks/" + id + "/notes/" + userNote.get("id").asText(), ADMIN, null)).get("notes")).hasSize(1);

        JsonNode summary = json(call(HttpMethod.GET, "aircraft/" + aircraft + "/tasks", USER, null)).get(0);
        Assertions.assertThat(summary.get("noteCount").asInt()).isEqualTo(1);
    }

    @Test
    void testDefectsAreReportedByEveryoneAndLinkedToOneTask() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");

        ResponseEntity<String> reported = call(HttpMethod.POST, "aircraft/" + aircraft + "/defects", USER,
                Map.of("title", "Riss in der Haube", "priority", "HIGH"));
        Assertions.assertThat(reported.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String defect = json(reported).get("id").asText();
        Assertions.assertThat(json(reported).get("editable").asBoolean()).isTrue();
        Assertions.assertThat(json(call(HttpMethod.GET, "aircraft/" + aircraft, USER, null)).get("openDefectCount").asInt()).isEqualTo(1);

        Map<String, Object> fix = new HashMap<>(task("Haube reparieren", List.of()));
        fix.put("defectId", defect);
        JsonNode task = json(call(HttpMethod.POST, "aircraft/" + aircraft + "/tasks", ADMIN, fix));
        Assertions.assertThat(task.get("defect").get("title").asText()).isEqualTo("Riss in der Haube");
        Assertions.assertThat(call(HttpMethod.POST, "aircraft/" + aircraft + "/tasks", ADMIN, fix).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        JsonNode defects = json(call(HttpMethod.GET, "aircraft/" + aircraft + "/defects", USER, null));
        Assertions.assertThat(defects.get(0).get("task").get("title").asText()).isEqualTo("Haube reparieren");
        Assertions.assertThat(defects.get(0).get("reportedBy").get("name").asText()).isEqualTo("test@test.com");

        JsonNode done = json(call(HttpMethod.PUT, "defects/" + defect + "/status", USER, Map.of("status", "DONE")));
        Assertions.assertThat(done.get("status").asText()).isEqualTo("DONE");
        Assertions.assertThat(json(call(HttpMethod.GET, "aircraft/" + aircraft, USER, null)).get("openDefectCount").asInt()).isZero();

        Assertions.assertThat(call(HttpMethod.DELETE, "defects/" + defect, USER, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode remaining = json(call(HttpMethod.GET, "tasks/" + task.get("id").asText(), USER, null));
        Assertions.assertThat(remaining.get("defect").isNull()).isTrue();
    }

    @Test
    void testOthersCannotEditADefectTheyDidNotReport() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = json(call(HttpMethod.POST, "aircraft/" + aircraft + "/defects", ADMIN, Map.of("title", "Kratzer", "priority", "LOW"))).get("id").asText();

        Assertions.assertThat(json(call(HttpMethod.GET, "aircraft/" + aircraft + "/defects", USER, null)).get(0).get("editable").asBoolean()).isFalse();
        Assertions.assertThat(call(HttpMethod.PUT, "defects/" + defect, USER, Map.of("title", "Delle", "priority", "LOW")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "defects/" + defect, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testATaskCannotFixADefectOfAnotherAircraft() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        String ls8 = createAircraft("LS 8", "D-5678");
        String defect = json(call(HttpMethod.POST, "aircraft/" + ls8 + "/defects", USER, Map.of("title", "Kratzer", "priority", "LOW"))).get("id").asText();

        Map<String, Object> fix = new HashMap<>(task("Kratzer", List.of()));
        fix.put("defectId", defect);
        Assertions.assertThat(call(HttpMethod.POST, "aircraft/" + ask21 + "/tasks", ADMIN, fix).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testDeletingAnAircraftRemovesItsTasksAndDefects() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String task = createTask(aircraft, "Reifen", List.of("Luftdruck")).get("id").asText();
        call(HttpMethod.POST, "tasks/" + task + "/notes", USER, Map.of("content", "ok"));
        call(HttpMethod.POST, "aircraft/" + aircraft + "/defects", USER, Map.of("title", "Kratzer", "priority", "LOW"));

        Assertions.assertThat(call(HttpMethod.DELETE, "aircraft/" + aircraft, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(call(HttpMethod.GET, "tasks/" + task, USER, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(call(HttpMethod.GET, "aircraft/" + aircraft + "/defects", USER, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testAircraftTypesAreOfferedForTemplates() throws Exception {
        createAircraft("LS 8", "D-5678");
        createAircraft("ASK 21", "D-1234");
        createAircraft("ASK 21", "D-4321");

        JsonNode types = json(call(HttpMethod.GET, "aircraft/types", USER, null));
        Assertions.assertThat(types).hasSize(2);
        Assertions.assertThat(types.get(0).asText()).isEqualTo("ASK 21");
    }
}
