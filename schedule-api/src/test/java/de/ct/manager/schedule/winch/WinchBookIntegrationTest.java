package de.ct.manager.schedule.winch;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

/** The winch book: settings (ropes, incident types), the page of today, photos, remarks, earlier days and the kiosk mode. */
@SuppressWarnings("null") // null warnings in assertions are false-positives
class WinchBookIntegrationTest extends AbstractIntegrationTest {

    private static final UserRole ADMIN = UserRole.ROLE_ADMIN;
    private static final UserRole USER = UserRole.ROLE_USER;
    private static final String USER_ID = "1234";
    private static final String OTHER_ID = "9999";
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, (byte) 0xFF, (byte) 0xD9 };

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
        for (String table : List.of("winch_photo_content", "winch_photo", "winch_incident", "winch_rope_day", "winch_day_driver", "winch_day",
                "winch_rope", "winch_incident_type", "auth_user_permissions")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        jdbcTemplate.update("DELETE FROM auth_user WHERE id = '9999'");
    }

    // --- helpers

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

    private HttpHeaders kiosk(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtUtils.generateKioskToken(new UserDetailsImpl("x", username, "x", List.of())));
        return headers;
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Berlin"));
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private static void expectStatus(ResponseEntity<String> response, HttpStatus status) {
        Assertions.assertThat(response.getStatusCode()).as(response.getBody()).isEqualTo(status);
    }

    private JsonNode addOption(String kind, String name) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "maintenance/winch/settings/" + kind, ADMIN, Map.of("name", name));
        expectStatus(response, HttpStatus.CREATED);
        return json(response);
    }

    private static String idOf(JsonNode options, String name) {
        for (JsonNode option : options) {
            if (option.get("name").asText().equals(name)) {
                return option.get("id").asText();
            }
        }
        throw new AssertionError(name + " not found in " + options);
    }

    private static List<String> names(JsonNode nodes, String field) {
        List<String> names = new ArrayList<>();
        nodes.forEach(node -> names.add(node.at(field).asText()));
        return names;
    }

    /** Two ropes and two kinds of incidents; answers the settings. */
    private JsonNode setUpWinch() throws Exception {
        addOption("ropes", "Seil 1");
        addOption("ropes", "Seil 2");
        addOption("incident-types", "Seilriss");
        return addOption("incident-types", "Schlaufe");
    }

    private ResponseEntity<String> addIncident(HttpHeaders headers, String ropeId, String typeId, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("ropeId", ropeId);
        body.put("typeId", typeId);
        body.put("comment", comment);
        return call(HttpMethod.POST, "maintenance/winch/incidents", headers, body);
    }

    private ResponseEntity<String> remark(String path, HttpHeaders headers, String text, Integer version) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("version", version);
        return call(HttpMethod.PUT, path, headers, body);
    }

    private ResponseEntity<String> upload(HttpHeaders auth, String ropeId, byte[] content, String filename, MediaType type) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(type);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        }, partHeaders));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length()));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.exchange(createURLWithPort("maintenance/winch/ropes/" + ropeId + "/photos"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }

    private static JsonNode rope(JsonNode detail, String name) {
        for (JsonNode rope : detail.get("ropes")) {
            if (rope.at("/rope/name").asText().equals(name)) {
                return rope;
            }
        }
        throw new AssertionError(name + " not on the page " + detail);
    }

    /** An earlier page written directly into the database: one rope break on Seil 1 by test@test.com. */
    private void earlierDay(String dayId, LocalDate date, String ropeId, String typeId) {
        jdbcTemplate.update("INSERT INTO winch_day (id, version, day_date, remark) VALUES (?, 0, ?, ?)", dayId, date, "Viel Seitenwind");
        jdbcTemplate.update("INSERT INTO winch_day_driver (day_id, user_id) VALUES (?, ?)", dayId, USER_ID);
        jdbcTemplate.update("INSERT INTO winch_incident (id, version, day_id, rope_id, type_id, driver_id, created_at) VALUES (?, 0, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                dayId + "-i", dayId, ropeId, typeId, USER_ID);
    }

    // --- settings

    @Test
    void testMaintenanceAdministratorsDefineRopesAndIncidentTypes() throws Exception {
        Assertions.assertThat(call(HttpMethod.POST, "maintenance/winch/settings/ropes", USER, Map.of("name", "Seil 3")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode settings = setUpWinch();
        Assertions.assertThat(names(settings.get("ropes"), "/name")).containsExactly("Seil 1", "Seil 2");
        Assertions.assertThat(names(settings.get("incidentTypes"), "/name")).containsExactly("Seilriss", "Schlaufe");

        // everybody reads them
        ResponseEntity<String> read = call(HttpMethod.GET, "maintenance/winch/settings", USER, null);
        expectStatus(read, HttpStatus.OK);
        Assertions.assertThat(names(json(read).get("ropes"), "/name")).containsExactly("Seil 1", "Seil 2");

        // names are unique, ignoring case, and required
        expectStatus(call(HttpMethod.POST, "maintenance/winch/settings/ropes", ADMIN, Map.of("name", " seil 1 ")), HttpStatus.CONFLICT);
        expectStatus(call(HttpMethod.POST, "maintenance/winch/settings/ropes", ADMIN, Map.of("name", " ")), HttpStatus.BAD_REQUEST);
        expectStatus(call(HttpMethod.POST, "maintenance/winch/settings/hooks", ADMIN, Map.of("name", "Haken")), HttpStatus.NOT_FOUND);

        // the maintenance right is enough to rename and reorder
        jdbcTemplate.update("INSERT INTO auth_user_permissions (user_id, permission) VALUES (?, ?)", USER_ID, "PERMISSION_MAINTENANCE_MANAGE");
        String seil2 = idOf(settings.get("ropes"), "Seil 2");
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        ResponseEntity<String> renamed = call(HttpMethod.PUT, "maintenance/winch/settings/ropes/" + seil2, USER, Map.of("name", "Rechtes Seil", "active", true));
        expectStatus(renamed, HttpStatus.OK);
        Assertions.assertThat(names(json(renamed).get("ropes"), "/name")).containsExactly("Seil 1", "Rechtes Seil");

        ResponseEntity<String> reordered = call(HttpMethod.PUT, "maintenance/winch/settings/ropes/order", USER, Map.of("ids", List.of(seil2, seil1)));
        expectStatus(reordered, HttpStatus.OK);
        Assertions.assertThat(names(json(reordered).get("ropes"), "/name")).containsExactly("Rechtes Seil", "Seil 1");
        expectStatus(call(HttpMethod.PUT, "maintenance/winch/settings/ropes/order", USER, Map.of("ids", List.of(seil2))), HttpStatus.BAD_REQUEST);
    }

    @Test
    void testRemovingDeletesUnusedOptionsAndOnlyDeactivatesUsedOnes() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String seil2 = idOf(settings.get("ropes"), "Seil 2");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");
        String schlaufe = idOf(settings.get("incidentTypes"), "Schlaufe");
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, null), HttpStatus.CREATED);

        Assertions.assertThat(call(HttpMethod.DELETE, "maintenance/winch/settings/ropes/" + seil1, USER, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode afterRopes = json(call(HttpMethod.DELETE, "maintenance/winch/settings/ropes/" + seil2, ADMIN, null));
        Assertions.assertThat(names(afterRopes.get("ropes"), "/name")).containsExactly("Seil 1");
        afterRopes = json(call(HttpMethod.DELETE, "maintenance/winch/settings/ropes/" + seil1, ADMIN, null));
        Assertions.assertThat(afterRopes.at("/ropes/0/active").asBoolean()).isFalse();
        Assertions.assertThat(afterRopes.at("/ropes/0/used").asBoolean()).isTrue();

        JsonNode afterTypes = json(call(HttpMethod.DELETE, "maintenance/winch/settings/incident-types/" + riss, ADMIN, null));
        afterTypes = json(call(HttpMethod.DELETE, "maintenance/winch/settings/incident-types/" + schlaufe, ADMIN, null));
        Assertions.assertThat(names(afterTypes.get("incidentTypes"), "/name")).containsExactly("Seilriss");
        Assertions.assertThat(afterTypes.at("/incidentTypes/0/active").asBoolean()).isFalse();

        // the entry keeps them, but they can no longer be chosen
        JsonNode today = json(call(HttpMethod.GET, "maintenance/winch/days/today", USER, null));
        Assertions.assertThat(names(today.get("ropes"), "/rope/name")).containsExactly("Seil 1");
        Assertions.assertThat(rope(today, "Seil 1").at("/rope/active").asBoolean()).isFalse();
        Assertions.assertThat(rope(today, "Seil 1").at("/incidents/0/type/name").asText()).isEqualTo("Seilriss");
        Assertions.assertThat(today.get("incidentTypes")).isEmpty();
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, null), HttpStatus.BAD_REQUEST);

        // reactivated, it is offered again
        expectStatus(call(HttpMethod.PUT, "maintenance/winch/settings/ropes/" + seil1, ADMIN, Map.of("name", "Seil 1", "active", true)), HttpStatus.OK);
        expectStatus(call(HttpMethod.PUT, "maintenance/winch/settings/incident-types/" + riss, ADMIN, Map.of("name", "Seilriss", "active", true)), HttpStatus.OK);
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, null), HttpStatus.CREATED);
    }

    // --- the page of today

    @Test
    void testEveryMemberRecordsIncidentsOfTodayAsWinchDriver() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String seil2 = idOf(settings.get("ropes"), "Seil 2");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");
        String schlaufe = idOf(settings.get("incidentTypes"), "Schlaufe");

        JsonNode empty = json(call(HttpMethod.GET, "maintenance/winch/days/today", USER, null));
        Assertions.assertThat(empty.get("date").asText()).isEqualTo(today().toString());
        Assertions.assertThat(empty.get("exists").asBoolean()).isFalse();
        Assertions.assertThat(empty.get("editable").asBoolean()).isTrue();
        Assertions.assertThat(names(empty.get("ropes"), "/rope/name")).containsExactly("Seil 1", "Seil 2");
        Assertions.assertThat(names(empty.get("incidentTypes"), "/name")).containsExactly("Seilriss", "Schlaufe");

        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, " am Startplatz "), HttpStatus.CREATED);
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, null), HttpStatus.CREATED);
        ResponseEntity<String> third = addIncident(other(), seil2, schlaufe, "");
        expectStatus(third, HttpStatus.CREATED);

        JsonNode page = json(third);
        Assertions.assertThat(page.get("exists").asBoolean()).isTrue();
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("other@test.com", "test@test.com");
        JsonNode first = rope(page, "Seil 1");
        Assertions.assertThat(first.get("incidents")).hasSize(2);
        Assertions.assertThat(first.at("/incidents/0/type/name").asText()).isEqualTo("Seilriss");
        Assertions.assertThat(first.at("/incidents/0/driver/name").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(first.at("/incidents/0/comment").asText()).isEqualTo("am Startplatz");
        // the other member deletes only own entries
        Assertions.assertThat(first.at("/incidents/0/deletable").asBoolean()).isFalse();
        Assertions.assertThat(rope(page, "Seil 2").at("/incidents/0/deletable").asBoolean()).isTrue();
        Assertions.assertThat(rope(page, "Seil 2").at("/incidents/0/comment").isNull()).isTrue();

        expectStatus(addIncident(createHeadersWithToken(USER), "missing", riss, null), HttpStatus.NOT_FOUND);
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, "missing", null), HttpStatus.NOT_FOUND);
    }

    @Test
    void testMembersDeleteTheirOwnIncidentsAndAnEmptyPageDisappears() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");
        JsonNode page = json(addIncident(createHeadersWithToken(USER), seil1, riss, null));
        String incident = rope(page, "Seil 1").at("/incidents/0/id").asText();

        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/incidents/" + incident, other(), null), HttpStatus.FORBIDDEN);
        ResponseEntity<String> deleted = call(HttpMethod.DELETE, "maintenance/winch/incidents/" + incident, USER, null);
        expectStatus(deleted, HttpStatus.OK);
        Assertions.assertThat(json(deleted).get("exists").asBoolean()).isFalse();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM winch_day", Long.class)).isZero();

        // maintenance administrators delete everybody's
        page = json(addIncident(other(), seil1, riss, null));
        incident = rope(page, "Seil 1").at("/incidents/0/id").asText();
        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/incidents/" + incident, ADMIN, null), HttpStatus.OK);
        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/incidents/" + incident, ADMIN, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void testRemarksOfTheDayAndPerRope() throws Exception {
        JsonNode settings = setUpWinch();
        String seil2 = idOf(settings.get("ropes"), "Seil 2");
        String dayPath = "maintenance/winch/days/" + today() + "/remark";
        String ropePath = "maintenance/winch/days/" + today() + "/ropes/" + seil2 + "/remark";

        // a remark alone starts the page, the writer becomes a winch driver
        ResponseEntity<String> saved = remark(dayPath, other(), " Winde läuft rund ", null);
        expectStatus(saved, HttpStatus.OK);
        JsonNode page = json(saved);
        Assertions.assertThat(page.at("/remark/text").asText()).isEqualTo("Winde läuft rund");
        Assertions.assertThat(page.at("/remark/updatedBy/name").asText()).isEqualTo("other@test.com");
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("other@test.com");
        int version = page.at("/remark/version").asInt();

        // everybody changes it, but not over a change of somebody else
        expectStatus(remark(dayPath, createHeadersWithToken(USER), "Überschrieben", null), HttpStatus.CONFLICT);
        expectStatus(remark(dayPath, createHeadersWithToken(USER), "Überschrieben", version + 1), HttpStatus.CONFLICT);
        page = json(remark(dayPath, createHeadersWithToken(USER), "Winde läuft rund, Öl nachgefüllt", version));
        Assertions.assertThat(page.at("/remark/text").asText()).isEqualTo("Winde läuft rund, Öl nachgefüllt");
        Assertions.assertThat(page.at("/remark/updatedBy/name").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("other@test.com", "test@test.com");

        // the remark of a rope; recording an incident afterwards does not count as a change of the remark
        page = json(remark(ropePath, createHeadersWithToken(USER), "Seil an der Kausch aufgeraut", null));
        JsonNode rope = rope(page, "Seil 2");
        Assertions.assertThat(rope.at("/remark/text").asText()).isEqualTo("Seil an der Kausch aufgeraut");
        int ropeVersion = rope.at("/remark/version").asInt();
        int dayVersion = page.at("/remark/version").asInt();
        page = json(addIncident(createHeadersWithToken(ADMIN), seil2, idOf(settings.get("incidentTypes"), "Schlaufe"), null));
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("admin@test.de", "other@test.com", "test@test.com");
        Assertions.assertThat(page.at("/remark/version").asInt()).isEqualTo(dayVersion);
        Assertions.assertThat(rope(page, "Seil 2").at("/remark/version").asInt()).isEqualTo(ropeVersion);

        // blank removes it
        page = json(remark(ropePath, createHeadersWithToken(USER), "  ", ropeVersion));
        Assertions.assertThat(rope(page, "Seil 2").at("/remark/text").isNull()).isTrue();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM winch_rope_day", Long.class)).isZero();

        expectStatus(remark("maintenance/winch/days/" + today().plusDays(1) + "/remark", createHeadersWithToken(USER), "Morgen", null), HttpStatus.BAD_REQUEST);
        expectStatus(remark("maintenance/winch/days/" + today() + "/ropes/missing/remark", createHeadersWithToken(USER), "x", null), HttpStatus.NOT_FOUND);
    }

    // --- photos

    @Test
    void testPhotosPerRope() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");

        ResponseEntity<String> uploaded = upload(createHeadersWithToken(USER), seil1, JPEG, "C:\\Fotos\\seil.jpg", MediaType.IMAGE_JPEG);
        expectStatus(uploaded, HttpStatus.CREATED);
        JsonNode photo = rope(json(uploaded), "Seil 1").at("/photos/0");
        Assertions.assertThat(photo.get("filename").asText()).isEqualTo("seil.jpg");
        Assertions.assertThat(photo.get("contentType").asText()).isEqualTo("image/jpeg");
        Assertions.assertThat(photo.get("size").asLong()).isEqualTo(JPEG.length);
        Assertions.assertThat(photo.at("/uploadedBy/name").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(names(json(uploaded).get("drivers"), "/name")).containsExactly("test@test.com");
        String photoId = photo.get("id").asText();

        // everybody sees it
        ResponseEntity<byte[]> image = restTemplate.exchange(createURLWithPort("maintenance/winch/photos/" + photoId), HttpMethod.GET,
                new HttpEntity<>(other()), byte[].class);
        Assertions.assertThat(image.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(image.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        Assertions.assertThat(image.getBody()).isEqualTo(JPEG);

        // only images, and not too large
        expectStatus(upload(createHeadersWithToken(USER), seil1, "kein Bild".getBytes(), "notiz.txt", MediaType.TEXT_PLAIN), HttpStatus.BAD_REQUEST);
        expectStatus(upload(createHeadersWithToken(USER), seil1, new byte[0], "leer.jpg", MediaType.IMAGE_JPEG), HttpStatus.BAD_REQUEST);
        expectStatus(upload(createHeadersWithToken(USER), seil1, new byte[WinchBookService.MAX_PHOTO_BYTES + 1], "riesig.jpg", MediaType.IMAGE_JPEG),
                HttpStatus.PAYLOAD_TOO_LARGE);
        expectStatus(upload(createHeadersWithToken(USER), "missing", JPEG, "seil.jpg", MediaType.IMAGE_JPEG), HttpStatus.NOT_FOUND);

        // uploaders and maintenance administrators delete
        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/photos/" + photoId, other(), null), HttpStatus.FORBIDDEN);
        ResponseEntity<String> deleted = call(HttpMethod.DELETE, "maintenance/winch/photos/" + photoId, USER, null);
        expectStatus(deleted, HttpStatus.OK);
        Assertions.assertThat(rope(json(deleted), "Seil 1").get("photos")).isEmpty();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM winch_photo_content", Long.class)).isZero();
        expectStatus(call(HttpMethod.GET, "maintenance/winch/photos/" + photoId, USER, null), HttpStatus.NOT_FOUND);
    }

    // --- earlier days

    @Test
    void testEarlierDaysAreReadByEverybodyAndCorrectedOnlyByMaintenanceAdministrators() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");
        LocalDate yesterday = today().minusDays(1);
        earlierDay("d-yesterday", yesterday, seil1, riss);

        ResponseEntity<String> read = call(HttpMethod.GET, "maintenance/winch/days/" + yesterday, USER, null);
        expectStatus(read, HttpStatus.OK);
        JsonNode page = json(read);
        Assertions.assertThat(page.get("today").asBoolean()).isFalse();
        Assertions.assertThat(page.get("editable").asBoolean()).isFalse();
        Assertions.assertThat(page.get("remarksEditable").asBoolean()).isFalse();
        Assertions.assertThat(page.get("incidentTypes")).isEmpty();
        // only the ropes with entries of that day
        Assertions.assertThat(names(page.get("ropes"), "/rope/name")).containsExactly("Seil 1");
        Assertions.assertThat(rope(page, "Seil 1").at("/incidents/0/deletable").asBoolean()).isFalse();
        Assertions.assertThat(page.at("/remark/text").asText()).isEqualTo("Viel Seitenwind");
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("test@test.com");

        expectStatus(call(HttpMethod.GET, "maintenance/winch/days/" + yesterday.minusDays(1), USER, null), HttpStatus.NOT_FOUND);
        expectStatus(call(HttpMethod.GET, "maintenance/winch/days/kein-datum", USER, null), HttpStatus.BAD_REQUEST);

        // members neither change nor delete anything of earlier days, not even their own entries
        int version = page.at("/remark/version").asInt();
        String dayRemark = "maintenance/winch/days/" + yesterday + "/remark";
        expectStatus(remark(dayRemark, createHeadersWithToken(USER), "Korrektur", version), HttpStatus.FORBIDDEN);
        expectStatus(remark("maintenance/winch/days/" + yesterday + "/ropes/" + seil1 + "/remark", createHeadersWithToken(USER), "Korrektur", null),
                HttpStatus.FORBIDDEN);
        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/incidents/d-yesterday-i", USER, null), HttpStatus.FORBIDDEN);

        // maintenance administrators correct remarks and delete entries, which does not make them winch drivers
        JsonNode adminView = json(call(HttpMethod.GET, "maintenance/winch/days/" + yesterday, ADMIN, null));
        Assertions.assertThat(adminView.get("remarksEditable").asBoolean()).isTrue();
        Assertions.assertThat(adminView.get("editable").asBoolean()).isFalse();
        Assertions.assertThat(rope(adminView, "Seil 1").at("/incidents/0/deletable").asBoolean()).isTrue();
        page = json(remark(dayRemark, createHeadersWithToken(ADMIN), "Viel Seitenwind aus West", version));
        Assertions.assertThat(page.at("/remark/text").asText()).isEqualTo("Viel Seitenwind aus West");
        Assertions.assertThat(names(page.get("drivers"), "/name")).containsExactly("test@test.com");
        expectStatus(remark("maintenance/winch/days/" + yesterday.minusDays(1) + "/remark", createHeadersWithToken(ADMIN), "x", null), HttpStatus.NOT_FOUND);

        page = json(call(HttpMethod.DELETE, "maintenance/winch/incidents/d-yesterday-i", ADMIN, null));
        Assertions.assertThat(page.get("exists").asBoolean()).isTrue();
        page = json(remark(dayRemark, createHeadersWithToken(ADMIN), "", page.at("/remark/version").asInt()));
        // nothing left: the page is gone
        Assertions.assertThat(page.get("exists").asBoolean()).isFalse();
        expectStatus(call(HttpMethod.GET, "maintenance/winch/days/" + yesterday, USER, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void testListsTheDaysOfAYearNewestFirst() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String seil2 = idOf(settings.get("ropes"), "Seil 2");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");
        String schlaufe = idOf(settings.get("incidentTypes"), "Schlaufe");
        int year = today().getYear();
        earlierDay("d-old", LocalDate.of(year - 2, 5, 1), seil1, riss);
        // on the 1st of January that day is today
        boolean firstOfJanuary = today().getDayOfYear() == 1;
        if (!firstOfJanuary) {
            earlierDay("d-jan", LocalDate.of(year, 1, 1), seil1, riss);
        }
        expectStatus(addIncident(createHeadersWithToken(USER), seil1, riss, null), HttpStatus.CREATED);
        expectStatus(addIncident(other(), seil2, riss, null), HttpStatus.CREATED);
        expectStatus(addIncident(other(), seil2, schlaufe, null), HttpStatus.CREATED);
        expectStatus(upload(createHeadersWithToken(USER), seil1, JPEG, "seil.jpg", MediaType.IMAGE_JPEG), HttpStatus.CREATED);

        // the current year by default
        ResponseEntity<String> list = call(HttpMethod.GET, "maintenance/winch/days", USER, null);
        expectStatus(list, HttpStatus.OK);
        JsonNode content = json(list).get("content");
        Assertions.assertThat(json(list).get("totalElements").asInt()).isEqualTo(firstOfJanuary ? 1 : 2);
        JsonNode latest = content.get(0);
        Assertions.assertThat(latest.get("date").asText()).isEqualTo(today().toString());
        Assertions.assertThat(names(latest.get("drivers"), "/name")).containsExactly("other@test.com", "test@test.com");
        Assertions.assertThat(names(latest.get("incidents"), "/name")).containsExactly("Seilriss", "Schlaufe");
        Assertions.assertThat(latest.at("/incidents/0/count").asLong()).isEqualTo(2);
        Assertions.assertThat(latest.get("incidentCount").asLong()).isEqualTo(3);
        Assertions.assertThat(latest.get("photoCount").asLong()).isEqualTo(1);
        Assertions.assertThat(latest.get("hasRemarks").asBoolean()).isFalse();
        if (!firstOfJanuary) {
            Assertions.assertThat(content.get(1).get("date").asText()).isEqualTo(year + "-01-01");
            Assertions.assertThat(content.get(1).get("hasRemarks").asBoolean()).isTrue();
        }

        // other years and pages
        JsonNode old = json(call(HttpMethod.GET, "maintenance/winch/days?year=" + (year - 2), USER, null));
        Assertions.assertThat(names(old.get("content"), "/date")).containsExactly((year - 2) + "-05-01");
        JsonNode paged = json(call(HttpMethod.GET, "maintenance/winch/days?year=" + year + "&size=1&page=0", USER, null));
        Assertions.assertThat(paged.get("content")).hasSize(1);
        Assertions.assertThat(json(call(HttpMethod.GET, "maintenance/winch/days?year=" + (year - 1), USER, null)).get("content")).isEmpty();

        JsonNode years = json(call(HttpMethod.GET, "maintenance/winch/years", USER, null));
        Assertions.assertThat(years.toString()).isEqualTo("[" + year + "," + (year - 2) + "]");
    }

    // --- kiosk

    @Test
    void testTheWinchBookWorksInTheKioskModeWithoutAdministration() throws Exception {
        JsonNode settings = setUpWinch();
        String seil1 = idOf(settings.get("ropes"), "Seil 1");
        String riss = idOf(settings.get("incidentTypes"), "Seilriss");

        HttpHeaders kioskAdmin = kiosk("admin@test.de");
        expectStatus(call(HttpMethod.GET, "maintenance/winch/days/today", kioskAdmin, null), HttpStatus.OK);
        expectStatus(call(HttpMethod.GET, "maintenance/winch/days", kioskAdmin, null), HttpStatus.OK);
        expectStatus(call(HttpMethod.GET, "maintenance/winch/settings", kioskAdmin, null), HttpStatus.OK);
        ResponseEntity<String> recorded = addIncident(kioskAdmin, seil1, riss, null);
        expectStatus(recorded, HttpStatus.CREATED);
        Assertions.assertThat(rope(json(recorded), "Seil 1").at("/incidents/0/driver/name").asText()).isEqualTo("admin@test.de");
        expectStatus(upload(kioskAdmin, seil1, JPEG, "seil.jpg", MediaType.IMAGE_JPEG), HttpStatus.CREATED);
        expectStatus(remark("maintenance/winch/days/" + today() + "/remark", kioskAdmin, "Vom Tablet", null), HttpStatus.OK);

        // the rights of the administrator do not count on the tablet
        expectStatus(call(HttpMethod.POST, "maintenance/winch/settings/ropes", kioskAdmin, Map.of("name", "Seil 3")), HttpStatus.FORBIDDEN);
        String foreign = rope(json(addIncident(createHeadersWithToken(USER), seil1, riss, null)), "Seil 1").at("/incidents/1/id").asText();
        expectStatus(call(HttpMethod.DELETE, "maintenance/winch/incidents/" + foreign, kioskAdmin, null), HttpStatus.FORBIDDEN);
    }
}
