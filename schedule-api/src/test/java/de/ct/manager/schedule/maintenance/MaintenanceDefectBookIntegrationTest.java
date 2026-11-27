package de.ct.manager.schedule.maintenance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

/** The defect book: workflow, sign-off, assignment, comments and the notifications of assignees. */
@SuppressWarnings("null") // null warnings in assertions are false-positives
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(statements = {
    "DELETE FROM user_notification", "DELETE FROM maintenance_defect_event", "DELETE FROM maintenance_defect_assignee",
    "DELETE FROM maintenance_task", "DELETE FROM maintenance_defect", "DELETE FROM maintenance_aircraft",
    "DELETE FROM auth_user_permissions"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MaintenanceDefectBookIntegrationTest extends AbstractIntegrationTest {

    private static final UserRole ADMIN = UserRole.ROLE_ADMIN;
    private static final UserRole USER = UserRole.ROLE_USER;
    private static final String USER_ID = "1234";
    private static final String ADMIN_ID = "5678";

    private static final String OTHER_USER = "INSERT INTO auth_user (id, version, username, password) VALUES ('9999', 0, 'other@test.com', 'x')";
    private static final String REMOVE_OTHER_USER = "DELETE FROM auth_user WHERE id = '9999'";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResponseEntity<String> call(HttpMethod method, String path, UserRole role, Object body) {
        return call(method, path, createHeadersWithToken(role), body);
    }

    private ResponseEntity<String> call(HttpMethod method, String path, HttpHeaders headers, Object body) {
        return restTemplate.exchange(createURLWithPort(path), method, new HttpEntity<>(body, headers), String.class);
    }

    /** A plain member without any role who neither reported nor was assigned anything. */
    private HttpHeaders otherUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtUtils.generateJwtToken(new TestingAuthenticationToken(new UserDetailsImpl("9999", "other@test.com", "x", List.of()), null)));
        return headers;
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String createAircraft(String type, String registration) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "maintenance/aircraft", ADMIN, Map.of("type", type, "registration", registration));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).get("id").asText();
    }

    private Map<String, Object> defect(String title, Set<String> assigneeIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("priority", "NORMAL");
        body.put("assigneeIds", assigneeIds);
        return body;
    }

    private String report(String aircraftId, UserRole role, String title) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "maintenance/aircraft/" + aircraftId + "/defects", role, defect(title, null));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).at("/defect/id").asText();
    }

    private ResponseEntity<String> setStatus(String defectId, UserRole role, String status, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("comment", comment);
        return call(HttpMethod.PUT, "maintenance/defects/" + defectId + "/status", role, body);
    }

    private List<String> actions(JsonNode detail) {
        List<String> actions = new ArrayList<>();
        detail.get("events").forEach(event -> actions.add(event.get("action").asText()));
        return actions;
    }

    private long openDefects(String aircraftId) throws Exception {
        return json(call(HttpMethod.GET, "maintenance/aircraft/" + aircraftId, USER, null)).get("openDefectCount").asLong();
    }

    @Test
    void testTheDefectBookListsAllAircraftAndEquipmentAndFiltersByOne() throws Exception {
        String ask21 = createAircraft("ASK 21", "D-1234");
        String winch = createAircraft("Winde", "Winde 1");
        report(ask21, USER, "Riss in der Haube");
        report(winch, USER, "Seil ausgefranst");

        JsonNode all = json(call(HttpMethod.GET, "maintenance/defects", USER, null));
        Assertions.assertThat(all).hasSize(2);
        Assertions.assertThat(all.findValuesAsText("aircraftLabel")).containsExactlyInAnyOrder("D-1234 · ASK 21", "Winde 1 · Winde");

        JsonNode ofWinch = json(call(HttpMethod.GET, "maintenance/defects?aircraftId=" + winch, USER, null));
        Assertions.assertThat(ofWinch).hasSize(1);
        Assertions.assertThat(ofWinch.get(0).get("title").asText()).isEqualTo("Seil ausgefranst");
        Assertions.assertThat(ofWinch.get(0).get("status").asText()).isEqualTo("OPEN");

        Assertions.assertThat(call(HttpMethod.GET, "maintenance/defects", (UserRole) null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testEverybodyWorksOnADefectButOnlyMaintenanceAdministratorsSignItOff() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = report(aircraft, ADMIN, "Kratzer");

        // a member who neither reported nor was assigned the defect works on it
        JsonNode started = json(setStatus(defect, USER, "IN_PROGRESS", null));
        Assertions.assertThat(started.at("/defect/status").asText()).isEqualTo("IN_PROGRESS");
        Assertions.assertThat(started.at("/defect/editable").asBoolean()).isFalse();
        Assertions.assertThat(openDefects(aircraft)).isEqualTo(1);

        // open defects cannot be signed off
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/sign", ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        JsonNode done = json(setStatus(defect, USER, "DONE", "Auspoliert"));
        Assertions.assertThat(done.at("/defect/doneBy/name").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(done.at("/defect/doneAt").isNull()).isFalse();
        Assertions.assertThat(openDefects(aircraft)).isZero();

        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/sign", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode signed = json(call(HttpMethod.POST, "maintenance/defects/" + defect + "/sign", ADMIN, null));
        Assertions.assertThat(signed.at("/defect/signedBy/name").asText()).isEqualTo("admin@test.de");
        Assertions.assertThat(actions(signed)).containsExactly("REPORTED", "STARTED", "DONE", "SIGNED");
        Assertions.assertThat(signed.at("/events/2/content").asText()).isEqualTo("Auspoliert");

        // signed off defects are locked
        Assertions.assertThat(setStatus(defect, USER, "OPEN", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", USER, Map.of("content", "?")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, ADMIN, defect("Delle", null)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect + "/sign", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode withdrawn = json(call(HttpMethod.DELETE, "maintenance/defects/" + defect + "/sign", ADMIN, null));
        Assertions.assertThat(withdrawn.at("/defect/signedAt").isNull()).isTrue();
        JsonNode reopened = json(setStatus(defect, USER, "OPEN", null));
        Assertions.assertThat(reopened.at("/defect/doneBy").isNull()).isTrue();
        Assertions.assertThat(actions(reopened)).endsWith("UNSIGNED", "REOPENED");
    }

    @Test
    void testOnlyMaintenanceAdministratorsRejectADefectWithAReason() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = report(aircraft, USER, "Reifen platt");

        // members work on the defect, but rejecting it is up to the maintenance administrators
        Assertions.assertThat(setStatus(defect, USER, "REJECTED", "Luftdruck war nur niedrig").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(setStatus(defect, ADMIN, "REJECTED", null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(setStatus(defect, ADMIN, "REJECTED", "  ").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode rejected = json(setStatus(defect, ADMIN, "REJECTED", "Luftdruck war nur niedrig"));
        Assertions.assertThat(rejected.at("/defect/status").asText()).isEqualTo("REJECTED");
        Assertions.assertThat(rejected.at("/events/1/action").asText()).isEqualTo("REJECTED");
        Assertions.assertThat(rejected.at("/events/1/content").asText()).isEqualTo("Luftdruck war nur niedrig");
        Assertions.assertThat(openDefects(aircraft)).isZero();

        // only administrators take a rejection back, members still comment
        Assertions.assertThat(setStatus(defect, USER, "OPEN", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(setStatus(defect, USER, "DONE", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", USER,
                Map.of("content", "Bitte trotzdem tauschen")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(json(setStatus(defect, ADMIN, "OPEN", null)).at("/defect/status").asText()).isEqualTo("OPEN");
        json(setStatus(defect, ADMIN, "REJECTED", "Luftdruck war nur niedrig"));

        // rejected defects are signed off like done ones
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/sign", ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(statements = OTHER_USER)
    @Sql(statements = REMOVE_OTHER_USER, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAssigneesAreNotifiedAndMayEditTheDefect() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        ResponseEntity<String> created = call(HttpMethod.POST, "maintenance/aircraft/" + aircraft + "/defects", ADMIN,
                defect("Bremse quietscht", Set.of(USER_ID, ADMIN_ID)));
        Assertions.assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String defect = json(created).at("/defect/id").asText();
        Assertions.assertThat(json(created).at("/defect/assignees").findValuesAsText("name")).containsExactly("admin@test.de", "test@test.com");
        Assertions.assertThat(actions(json(created))).containsExactly("REPORTED", "ASSIGNED", "ASSIGNED");

        // the assignee sees the notification, administrators are not notified about their own assignment
        JsonNode notifications = json(call(HttpMethod.GET, "notifications", USER, null));
        Assertions.assertThat(notifications.get("unreadCount").asInt()).isEqualTo(1);
        JsonNode notification = notifications.get("items").get(0);
        Assertions.assertThat(notification.get("type").asText()).isEqualTo("DEFECT_ASSIGNED");
        Assertions.assertThat(notification.get("read").asBoolean()).isFalse();
        Assertions.assertThat(notification.at("/defect/id").asText()).isEqualTo(defect);
        Assertions.assertThat(notification.at("/defect/aircraftId").asText()).isEqualTo(aircraft);
        Assertions.assertThat(notification.at("/defect/aircraftLabel").asText()).isEqualTo("D-1234");
        Assertions.assertThat(notification.at("/actor/name").asText()).isEqualTo("admin@test.de");
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", ADMIN, null)).get("unreadCount").asInt()).isZero();

        // the assignee edits the defect of somebody else, but does not change the assignees
        JsonNode mine = json(call(HttpMethod.GET, "maintenance/defects/" + defect, USER, null));
        Assertions.assertThat(mine.at("/defect/assignedToMe").asBoolean()).isTrue();
        Assertions.assertThat(mine.at("/defect/editable").asBoolean()).isTrue();
        Assertions.assertThat(mine.at("/defect/deletable").asBoolean()).isFalse();
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, USER, defect("Bremse quietscht laut", null)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, USER, defect("Bremse", Set.of(USER_ID))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // somebody who is not assigned only works on it
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, otherUser(), defect("Bremse", null)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", otherUser(), Map.of("content", "Schaue ich mir an")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // reading
        String notificationId = notification.get("id").asText();
        Assertions.assertThat(call(HttpMethod.PUT, "notifications/" + notificationId + "/read", otherUser(), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(call(HttpMethod.PUT, "notifications/" + notificationId + "/read", USER, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode read = json(call(HttpMethod.GET, "notifications", USER, null));
        Assertions.assertThat(read.get("unreadCount").asInt()).isZero();
        Assertions.assertThat(read.at("/items/0/read").asBoolean()).isTrue();
    }

    @Test
    @Sql(statements = OTHER_USER)
    @Sql(statements = REMOVE_OTHER_USER, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testOnlyMaintenanceAdministratorsAssignDefects() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = report(aircraft, USER, "Kratzer");

        Assertions.assertThat(call(HttpMethod.GET, "maintenance/defects/assignable-users", USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode users = json(call(HttpMethod.GET, "maintenance/defects/assignable-users", ADMIN, null));
        Assertions.assertThat(users.findValuesAsText("name")).containsExactly("admin@test.de", "other@test.com", "test@test.com");

        // the reporter edits, but does not assign
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, USER, defect("Kratzer", Set.of("9999"))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/aircraft/" + aircraft + "/defects", USER, defect("Delle", Set.of("9999"))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, ADMIN, defect("Kratzer", Set.of("unknown"))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode assigned = json(call(HttpMethod.PUT, "maintenance/defects/" + defect, ADMIN, defect("Kratzer", Set.of("9999"))));
        Assertions.assertThat(assigned.at("/defect/assignees/0/name").asText()).isEqualTo("other@test.com");
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", otherUser(), null)).get("unreadCount").asInt()).isEqualTo(1);

        // taking the assignment back before it was read removes the notification and is logged
        JsonNode unassigned = json(call(HttpMethod.PUT, "maintenance/defects/" + defect, ADMIN, defect("Kratzer", Set.of(USER_ID))));
        Assertions.assertThat(unassigned.at("/defect/assignees").findValuesAsText("id")).containsExactly(USER_ID);
        Assertions.assertThat(actions(unassigned)).containsExactly("REPORTED", "ASSIGNED", "UNASSIGNED", "ASSIGNED");
        Assertions.assertThat(unassigned.at("/events/2/subject/name").asText()).isEqualTo("other@test.com");
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", otherUser(), null)).get("items")).isEmpty();

        // the right unlocks assigning as well
        jdbcTemplate.update("INSERT INTO auth_user_permissions (user_id, permission) VALUES ('1234', 'PERMISSION_MAINTENANCE_MANAGE')");
        Assertions.assertThat(call(HttpMethod.PUT, "maintenance/defects/" + defect, USER, defect("Kratzer", Set.of())).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testAllNotificationsAreMarkedReadAtOnceAndVanishWithTheDefect() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        call(HttpMethod.POST, "maintenance/aircraft/" + aircraft + "/defects", ADMIN, defect("Kratzer", Set.of(USER_ID)));
        String second = json(call(HttpMethod.POST, "maintenance/aircraft/" + aircraft + "/defects", ADMIN, defect("Delle", Set.of(USER_ID))))
                .at("/defect/id").asText();

        JsonNode notifications = json(call(HttpMethod.GET, "notifications", USER, null));
        Assertions.assertThat(notifications.get("unreadCount").asInt()).isEqualTo(2);
        // latest first
        Assertions.assertThat(notifications.at("/items/0/defect/title").asText()).isEqualTo("Delle");

        Assertions.assertThat(call(HttpMethod.PUT, "notifications/read-all", USER, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", USER, null)).get("unreadCount").asInt()).isZero();

        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + second, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(json(call(HttpMethod.GET, "notifications", USER, null)).get("items")).hasSize(1);
    }

    @Test
    void testCommentsAreDeletedByTheirAuthorAndAdministrators() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = report(aircraft, USER, "Kratzer");

        JsonNode byUser = json(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", USER, Map.of("content", "Foto folgt")));
        JsonNode byAdmin = json(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", ADMIN, Map.of("content", "Bitte bis Samstag")));
        Assertions.assertThat(byAdmin.at("/defect/commentCount").asInt()).isEqualTo(2);
        String userComment = byUser.at("/events/1/id").asText();
        String adminComment = byAdmin.at("/events/2/id").asText();
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/defects/" + defect + "/comments", USER, Map.of("content", " ")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode seenByUser = json(call(HttpMethod.GET, "maintenance/defects/" + defect, USER, null));
        Assertions.assertThat(seenByUser.at("/events/1/deletable").asBoolean()).isTrue();
        Assertions.assertThat(seenByUser.at("/events/2/deletable").asBoolean()).isFalse();
        // log entries other than comments are never deletable
        Assertions.assertThat(seenByUser.at("/events/0/deletable").asBoolean()).isFalse();
        String reported = seenByUser.at("/events/0/id").asText();
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect + "/comments/" + reported, ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect + "/comments/" + adminComment, USER, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode afterDelete = json(call(HttpMethod.DELETE, "maintenance/defects/" + defect + "/comments/" + userComment, ADMIN, null));
        Assertions.assertThat(afterDelete.at("/defect/commentCount").asInt()).isEqualTo(1);
        Assertions.assertThat(json(call(HttpMethod.GET, "maintenance/defects", USER, null)).at("/0/commentCount").asInt()).isEqualTo(1);
    }

    @Test
    void testReportersDeleteTheirDefectUntilItIsSignedOff() throws Exception {
        String aircraft = createAircraft("ASK 21", "D-1234");
        String defect = report(aircraft, USER, "Kratzer");
        Assertions.assertThat(json(call(HttpMethod.GET, "maintenance/defects/" + defect, USER, null)).at("/defect/deletable").asBoolean()).isTrue();

        setStatus(defect, USER, "DONE", null);
        call(HttpMethod.POST, "maintenance/defects/" + defect + "/sign", ADMIN, null);
        Assertions.assertThat(json(call(HttpMethod.GET, "maintenance/defects/" + defect, USER, null)).at("/defect/deletable").asBoolean()).isFalse();
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/defects/" + defect, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(call(HttpMethod.GET, "maintenance/defects/" + defect, USER, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
