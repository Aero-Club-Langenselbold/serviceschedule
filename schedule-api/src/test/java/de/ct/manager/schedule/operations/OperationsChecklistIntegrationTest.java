package de.ct.manager.schedule.operations;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

/** Start/end of operations checklists: templates, the checklist of the day, defects, submit, approval and the archive. */
@SuppressWarnings("null") // null warnings in assertions are false-positives
class OperationsChecklistIntegrationTest extends AbstractIntegrationTest {

    private static final UserRole ADMIN = UserRole.ROLE_ADMIN;
    private static final UserRole USER = UserRole.ROLE_USER;
    private static final String USER_ID = "1234";
    private static final String OTHER_ID = "9999";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Done here rather than with @Sql on the class, which would hide the users seeded by the base class.
    @BeforeEach
    void addOtherUser() {
        jdbcTemplate.update("INSERT INTO auth_user (id, version, username, password) VALUES ('9999', 0, 'other@test.com', 'x')");
    }

    @AfterEach
    void cleanUp() {
        for (String table : List.of("user_notification", "operations_checklist_item", "operations_checklist", "operations_checklist_template_item",
                "operations_checklist_template", "maintenance_defect_event", "maintenance_defect", "maintenance_aircraft", "auth_user_permissions")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        jdbcTemplate.update("DELETE FROM auth_user WHERE id = '9999'");
    }

    private ResponseEntity<String> call(HttpMethod method, String path, UserRole role, Object body) {
        return call(method, path, createHeadersWithToken(role), body);
    }

    private ResponseEntity<String> call(HttpMethod method, String path, HttpHeaders headers, Object body) {
        return restTemplate.exchange(createURLWithPort(path), method, new HttpEntity<>(body, headers), String.class);
    }

    /** A plain member besides test@test.com. */
    private HttpHeaders other() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtUtils.generateJwtToken(new TestingAuthenticationToken(new UserDetailsImpl(OTHER_ID, "other@test.com", "x", List.of()), null)));
        return headers;
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Berlin"));
    }

    private void grant(String userId, String permission) {
        jdbcTemplate.update("INSERT INTO auth_user_permissions (user_id, permission) VALUES (?, ?)", userId, permission);
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String createAircraft(String type, String registration) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "maintenance/aircraft", ADMIN, Map.of("type", type, "registration", registration));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).get("id").asText();
    }

    private ResponseEntity<String> saveTemplate(String aircraftId, String kind, UserRole role, List<String> items) {
        return call(HttpMethod.PUT, "operations/aircraft/" + aircraftId + "/templates/" + kind, role, Map.of("items", items));
    }

    private String startToday(String aircraftId, String kind, HttpHeaders headers) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "operations/aircraft/" + aircraftId + "/checklists/" + kind + "/today", headers, null);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).at("/checklist/id").asText();
    }

    private List<String> itemIds(JsonNode detail) {
        List<String> ids = new ArrayList<>();
        detail.get("items").forEach(item -> ids.add(item.get("id").asText()));
        return ids;
    }

    private ResponseEntity<String> check(String checklistId, String itemId, HttpHeaders headers, String result, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("result", result);
        body.put("comment", comment);
        return call(HttpMethod.PUT, "operations/checklists/" + checklistId + "/items/" + itemId, headers, body);
    }

    private String aircraftWithStartChecklist() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        Assertions.assertThat(saveTemplate(aircraft, "START", ADMIN, List.of("Haube reinigen", " Reifendruck prüfen ", "")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return aircraft;
    }

    // --- templates

    @Test
    void testMaintenanceAndOperationsAdministratorsManageTheTemplatesOthersOnlyReadThem() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");

        Assertions.assertThat(saveTemplate(aircraft, "START", USER, List.of("Haube")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/templates/START", USER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        grant(USER_ID, "PERMISSION_OPERATIONS_MANAGE");
        ResponseEntity<String> saved = saveTemplate(aircraft, "START", USER, List.of(" Haube reinigen ", "", "Reifendruck"));
        Assertions.assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(json(saved).get("items").toString()).isEqualTo("[\"Haube reinigen\",\"Reifendruck\"]");

        jdbcTemplate.update("DELETE FROM auth_user_permissions");
        grant(USER_ID, "PERMISSION_MAINTENANCE_MANAGE");
        Assertions.assertThat(saveTemplate(aircraft, "END", USER, List.of("Abdeckung drauf")).getStatusCode()).isEqualTo(HttpStatus.OK);
        // replacing keeps one template per aircraft and kind
        Assertions.assertThat(saveTemplate(aircraft, "END", USER, List.of("Abdeckung drauf", "Batterie laden")).getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode read = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/templates/END", other(), null));
        Assertions.assertThat(read.get("items")).hasSize(2);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist_template", Long.class)).isEqualTo(2);

        Assertions.assertThat(call(HttpMethod.DELETE, "operations/aircraft/" + aircraft + "/templates/END", other(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "operations/aircraft/" + aircraft + "/templates/END", USER, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/templates/END", USER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testATemplateNeedsItems() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");

        Assertions.assertThat(saveTemplate(aircraft, "START", ADMIN, List.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(saveTemplate(aircraft, "START", ADMIN, List.of(" ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- the checklist of the day

    @Test
    void testTheFixedLinkLeadsToTheChecklistOfToday() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String today = today().toString();

        JsonNode before = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", USER, null));
        Assertions.assertThat(before.get("date").asText()).isEqualTo(today);
        Assertions.assertThat(before.get("registration").asText()).isEqualTo("D-1234");
        Assertions.assertThat(before.get("templateItems")).hasSize(2);
        Assertions.assertThat(before.get("checklist").isNull()).isTrue();

        // no template for the end of operations
        JsonNode end = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/END/today", USER, null));
        Assertions.assertThat(end.get("templateItems").isNull()).isTrue();
        Assertions.assertThat(call(HttpMethod.POST, "operations/aircraft/" + aircraft + "/checklists/END/today", USER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        String id = startToday(aircraft, "START", createHeadersWithToken(USER));
        JsonNode after = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", other(), null));
        Assertions.assertThat(after.at("/checklist/checklist/id").asText()).isEqualTo(id);
        Assertions.assertThat(after.at("/checklist/checklist/createdBy/id").asText()).isEqualTo(USER_ID);
        Assertions.assertThat(after.at("/checklist/items/0/text").asText()).isEqualTo("Haube reinigen");
        Assertions.assertThat(after.at("/checklist/editable").asBoolean()).isTrue();
    }

    @Test
    void testOnlyOneChecklistPerAircraftKindAndDay() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        Assertions.assertThat(saveTemplate(aircraft, "END", ADMIN, List.of("Abdeckung")).getStatusCode()).isEqualTo(HttpStatus.OK);

        startToday(aircraft, "START", createHeadersWithToken(USER));
        Assertions.assertThat(call(HttpMethod.POST, "operations/aircraft/" + aircraft + "/checklists/START/today", other(), null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        // the end of operations is a checklist of its own
        startToday(aircraft, "END", other());

        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist", Long.class)).isEqualTo(2);
    }

    @Test
    void testEverybodyChecksItemsButTheInitialRecorderStays() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));
        List<String> items = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null)));

        ResponseEntity<String> checked = check(id, items.get(0), other(), "OK", " sauber ");
        Assertions.assertThat(checked.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode detail = json(checked);
        Assertions.assertThat(detail.at("/items/0/result").asText()).isEqualTo("OK");
        Assertions.assertThat(detail.at("/items/0/comment").asText()).isEqualTo("sauber");
        Assertions.assertThat(detail.at("/items/0/checkedBy/id").asText()).isEqualTo(OTHER_ID);
        Assertions.assertThat(detail.at("/checklist/createdBy/id").asText()).isEqualTo(USER_ID);
        Assertions.assertThat(detail.at("/checklist/checkedCount").asLong()).isEqualTo(1);
        Assertions.assertThat(detail.get("submittable").asBoolean()).isFalse();

        // unchecking forgets who checked it
        JsonNode unchecked = json(check(id, items.get(0), createHeadersWithToken(USER), null, null));
        Assertions.assertThat(unchecked.at("/items/0/result").isNull()).isTrue();
        Assertions.assertThat(unchecked.at("/items/0/checkedBy").isNull()).isTrue();

        // an item of another checklist
        Assertions.assertThat(check(id, "unknown", other(), "OK", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDefectsAreReportedToTheDefectBookFromAnItem() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));
        List<String> items = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null)));

        Map<String, Object> defect = new HashMap<>();
        defect.put("title", "Riss in der Haube");
        defect.put("priority", "HIGH");
        ResponseEntity<String> reported = call(HttpMethod.POST, "operations/checklists/" + id + "/items/" + items.get(0) + "/defect", other(), defect);
        Assertions.assertThat(reported.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode detail = json(reported);
        Assertions.assertThat(detail.at("/items/0/result").asText()).isEqualTo("NOT_OK");
        Assertions.assertThat(detail.at("/items/0/defect/title").asText()).isEqualTo("Riss in der Haube");
        Assertions.assertThat(detail.at("/checklist/notOkCount").asLong()).isEqualTo(1);

        JsonNode book = json(call(HttpMethod.GET, "maintenance/defects?aircraftId=" + aircraft, USER, null));
        Assertions.assertThat(book).hasSize(1);
        Assertions.assertThat(book.get(0).at("/reportedBy/id").asText()).isEqualTo(OTHER_ID);
        Assertions.assertThat(book.get(0).get("priority").asText()).isEqualTo("HIGH");

        // one defect per item, and the item stays not OK
        Assertions.assertThat(call(HttpMethod.POST, "operations/checklists/" + id + "/items/" + items.get(0) + "/defect", other(), defect).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(check(id, items.get(0), other(), "OK", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(check(id, items.get(0), other(), "NOT_OK", "Riss 3 cm").getStatusCode()).isEqualTo(HttpStatus.OK);

        // deleting the defect keeps the item
        String defectId = book.get(0).get("id").asText();
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defectId, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode afterDelete = json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null));
        Assertions.assertThat(afterDelete.at("/items/0/defect").isNull()).isTrue();
        Assertions.assertThat(afterDelete.at("/items/0/result").asText()).isEqualTo("NOT_OK");
    }

    @Test
    void testSubmittingNotifiesTheOperationsAdministratorsWhoApprove() throws Exception {
        grant(USER_ID, "PERMISSION_OPERATIONS_MANAGE");
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", other());
        List<String> items = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, other(), null)));

        check(id, items.get(0), other(), "OK", null);
        Assertions.assertThat(call(HttpMethod.POST, "operations/checklists/" + id + "/submit", other(), null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode complete = json(check(id, items.get(1), other(), "NOT_OK", "zu wenig Luft"));
        Assertions.assertThat(complete.get("submittable").asBoolean()).isTrue();
        Assertions.assertThat(complete.get("approvable").asBoolean()).isFalse();

        ResponseEntity<String> submitted = call(HttpMethod.POST, "operations/checklists/" + id + "/submit", other(), null);
        Assertions.assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(json(submitted).at("/checklist/status").asText()).isEqualTo("SUBMITTED");
        Assertions.assertThat(json(submitted).at("/checklist/submittedBy/id").asText()).isEqualTo(OTHER_ID);
        Assertions.assertThat(json(submitted).get("editable").asBoolean()).isFalse();
        // locked once submitted
        Assertions.assertThat(check(id, items.get(0), other(), null, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // only the holder of the right is notified, not the administrator nor the submitter
        JsonNode notifications = json(call(HttpMethod.GET, "notifications", USER, null));
        Assertions.assertThat(notifications.get("unreadCount").asLong()).isEqualTo(1);
        Assertions.assertThat(notifications.at("/items/0/type").asText()).isEqualTo("CHECKLIST_SUBMITTED");
        Assertions.assertThat(notifications.at("/items/0/checklist/id").asText()).isEqualTo(id);
        Assertions.assertThat(notifications.at("/items/0/checklist/registration").asText()).isEqualTo("D-1234");
        Assertions.assertThat(notifications.at("/items/0/actor/id").asText()).isEqualTo(OTHER_ID);
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", ADMIN, null)).get("unreadCount").asLong()).isZero();
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", other(), null)).get("unreadCount").asLong()).isZero();

        // members do not approve, operations administrators (and administrators) do
        Assertions.assertThat(call(HttpMethod.POST, "operations/checklists/" + id + "/approve", other(), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> approved = call(HttpMethod.POST, "operations/checklists/" + id + "/approve", ADMIN, null);
        Assertions.assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(json(approved).at("/checklist/status").asText()).isEqualTo("APPROVED");
        Assertions.assertThat(json(approved).at("/checklist/approvedBy/id").asText()).isEqualTo("5678");
        Assertions.assertThat(call(HttpMethod.POST, "operations/checklists/" + id + "/approve", USER, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        // approved by someone else, so the notification is not news anymore
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", USER, null)).get("unreadCount").asLong()).isZero();
    }

    @Test
    void testOperationsAdministratorsSendAChecklistBack() throws Exception {
        grant(USER_ID, "PERMISSION_OPERATIONS_MANAGE");
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", other());
        for (String item : itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, other(), null)))) {
            check(id, item, other(), "OK", null);
        }
        call(HttpMethod.POST, "operations/checklists/" + id + "/submit", other(), null);

        Assertions.assertThat(call(HttpMethod.POST, "operations/checklists/" + id + "/reopen", other(), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode reopened = json(call(HttpMethod.POST, "operations/checklists/" + id + "/reopen", USER, null));
        Assertions.assertThat(reopened.at("/checklist/status").asText()).isEqualTo("IN_PROGRESS");
        Assertions.assertThat(reopened.at("/checklist/submittedBy").isNull()).isTrue();
        Assertions.assertThat(reopened.get("editable").asBoolean()).isTrue();
        // the checks stay, and the unread notification is gone
        Assertions.assertThat(reopened.at("/checklist/checkedCount").asLong()).isEqualTo(2);
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", USER, null)).get("unreadCount").asLong()).isZero();
    }

    @Test
    void testChecklistsOfPastDaysAreReadOnlyAndOnlyForOperationsAdministrators() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));
        String item = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null))).get(0);
        jdbcTemplate.update("UPDATE operations_checklist SET checklist_date = ? WHERE id = ?", today().minusDays(3), id);

        Assertions.assertThat(call(HttpMethod.GET, "operations/checklists/" + id, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode past = json(call(HttpMethod.GET, "operations/checklists/" + id, ADMIN, null));
        Assertions.assertThat(past.get("editable").asBoolean()).isFalse();
        Assertions.assertThat(check(id, item, createHeadersWithToken(USER), "OK", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // today is a new day: a new checklist can be started
        JsonNode today = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", USER, null));
        Assertions.assertThat(today.get("checklist").isNull()).isTrue();
        startToday(aircraft, "START", other());
    }

    @Test
    void testChangingTheTemplateKeepsStartedChecklists() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));

        saveTemplate(aircraft, "START", ADMIN, List.of("Ganz anders"));
        Assertions.assertThat(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null)).get("items")).hasSize(2);
        call(HttpMethod.DELETE, "operations/aircraft/" + aircraft + "/templates/START", ADMIN, null);
        Assertions.assertThat(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null)).get("items")).hasSize(2);
    }

    private void recreateStartTemplate(String aircraft, List<String> items) {
        Assertions.assertThat(call(HttpMethod.DELETE, "operations/aircraft/" + aircraft + "/templates/START", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(saveTemplate(aircraft, "START", ADMIN, items).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testOpeningTheChecklistOfTodayAfterTheTemplateWasCreatedAnewDiscardsIt() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));
        String item = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, USER, null))).get(0);
        check(id, item, other(), "OK", null);

        recreateStartTemplate(aircraft, List.of("Neue Haube prüfen"));

        JsonNode today = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", other(), null));
        Assertions.assertThat(today.get("checklist").isNull()).isTrue();
        Assertions.assertThat(today.get("templateItems").toString()).isEqualTo("[\"Neue Haube prüfen\"]");
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist", Long.class)).isZero();

        // a new one with the items of the new template, recorded by whoever starts it
        String fresh = startToday(aircraft, "START", other());
        JsonNode detail = json(call(HttpMethod.GET, "operations/checklists/" + fresh, other(), null));
        Assertions.assertThat(detail.get("items")).hasSize(1);
        Assertions.assertThat(detail.at("/checklist/createdBy/id").asText()).isEqualTo(OTHER_ID);
        // opening it again keeps it
        Assertions.assertThat(json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", USER, null))
                .at("/checklist/checklist/id").asText()).isEqualTo(fresh);
    }

    @Test
    void testAnOutdatedChecklistNobodyOpensStaysInTheArchive() throws Exception {
        grant(USER_ID, "PERMISSION_OPERATIONS_MANAGE");
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", other());

        recreateStartTemplate(aircraft, List.of("Neue Haube prüfen"));

        // neither the archive nor the overview discard it
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists", USER, null)))).containsExactly(id);
        JsonNode overview = json(call(HttpMethod.GET, "operations/overview", USER, null));
        Assertions.assertThat(overview.at("/aircraft/0/todayStart").isNull()).isTrue();
        Assertions.assertThat(call(HttpMethod.GET, "operations/checklists/" + id, USER, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist", Long.class)).isEqualTo(1);
    }

    @Test
    void testStartingAfterTheTemplateWasCreatedAnewReplacesTheOutdatedChecklist() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));

        recreateStartTemplate(aircraft, List.of("Neue Haube prüfen"));

        String fresh = startToday(aircraft, "START", other());
        Assertions.assertThat(fresh).isNotEqualTo(id);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist", Long.class)).isEqualTo(1);
    }

    @Test
    void testSubmittedChecklistsAreNeverDiscarded() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", other());
        for (String item : itemIds(json(call(HttpMethod.GET, "operations/checklists/" + id, other(), null)))) {
            check(id, item, other(), "OK", null);
        }
        call(HttpMethod.POST, "operations/checklists/" + id + "/submit", other(), null);

        recreateStartTemplate(aircraft, List.of("Neue Haube prüfen"));

        JsonNode today = json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", other(), null));
        Assertions.assertThat(today.at("/checklist/checklist/id").asText()).isEqualTo(id);
        Assertions.assertThat(today.at("/checklist/checklist/status").asText()).isEqualTo("SUBMITTED");
    }

    @Test
    void testChangingOrOnlyDeletingTheTemplateKeepsTheChecklistOfToday() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));

        saveTemplate(aircraft, "START", ADMIN, List.of("Geändert"));
        Assertions.assertThat(json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", USER, null))
                .at("/checklist/checklist/id").asText()).isEqualTo(id);

        call(HttpMethod.DELETE, "operations/aircraft/" + aircraft + "/templates/START", ADMIN, null);
        Assertions.assertThat(json(call(HttpMethod.GET, "operations/aircraft/" + aircraft + "/checklists/START/today", USER, null))
                .at("/checklist/checklist/id").asText()).isEqualTo(id);
    }

    // --- overview and archive

    @Test
    void testTheOverviewListsAircraftWithTemplatesAndTheChecklistsOfToday() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        createAircraft("Winde", "Winde 1");
        String id = startToday(aircraft, "START", createHeadersWithToken(USER));

        JsonNode overview = json(call(HttpMethod.GET, "operations/overview", other(), null));
        Assertions.assertThat(overview.get("aircraft")).hasSize(2);
        JsonNode ask21 = overview.get("aircraft").get(0);
        Assertions.assertThat(ask21.get("registration").asText()).isEqualTo("D-1234");
        Assertions.assertThat(ask21.at("/startTemplate/items")).hasSize(2);
        Assertions.assertThat(ask21.get("endTemplate").isNull()).isTrue();
        Assertions.assertThat(ask21.at("/todayStart/id").asText()).isEqualTo(id);
        Assertions.assertThat(ask21.at("/todayStart/itemCount").asLong()).isEqualTo(2);
        Assertions.assertThat(overview.get("aircraft").get(1).get("startTemplate").isNull()).isTrue();
    }

    @Test
    void testOperationsAdministratorsSearchTheArchive() throws Exception {
        String ask21 = aircraftWithStartChecklist();
        String winch = createAircraft("Winde", "Winde 1");
        saveTemplate(winch, "END", ADMIN, List.of("Seil einziehen"));
        String first = startToday(ask21, "START", createHeadersWithToken(USER));
        jdbcTemplate.update("UPDATE operations_checklist SET checklist_date = ? WHERE id = ?", today().minusDays(1), first);
        String second = startToday(ask21, "START", other());
        String third = startToday(winch, "END", other());
        String item = itemIds(json(call(HttpMethod.GET, "operations/checklists/" + third, other(), null))).get(0);
        check(third, item, other(), "NOT_OK", "Seil verdreht");

        Assertions.assertThat(call(HttpMethod.GET, "operations/checklists", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        grant(USER_ID, "PERMISSION_OPERATIONS_MANAGE");

        JsonNode all = json(call(HttpMethod.GET, "operations/checklists", USER, null));
        Assertions.assertThat(all.at("/totalElements").asLong()).isEqualTo(3);
        // latest day first
        Assertions.assertThat(all.at("/content/2/id").asText()).isEqualTo(first);

        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?aircraftId=" + ask21, USER, null)))).containsExactlyInAnyOrder(first, second);
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?kind=END", USER, null)))).containsExactly(third);
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?to=" + today().minusDays(1), USER, null)))).containsExactly(first);
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?search=verdreht", USER, null)))).containsExactly(third);
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?search=TEST@test", USER, null)))).containsExactly(first);
        Assertions.assertThat(ids(json(call(HttpMethod.GET, "operations/checklists?status=APPROVED", USER, null)))).isEmpty();

        // and read every checklist of the archive
        Assertions.assertThat(call(HttpMethod.GET, "operations/checklists/" + first, USER, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeletingTheAircraftRemovesItsChecklists() throws Exception {
        String aircraft = aircraftWithStartChecklist();
        startToday(aircraft, "START", createHeadersWithToken(USER));

        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/aircraft/" + aircraft, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist", Long.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operations_checklist_template", Long.class)).isZero();
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.get("content").forEach(summary -> ids.add(summary.get("id").asText()));
        return ids;
    }
}
