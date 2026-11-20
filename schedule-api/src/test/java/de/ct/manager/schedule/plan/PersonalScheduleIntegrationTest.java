package de.ct.manager.schedule.plan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null")
class PersonalScheduleIntegrationTest extends AbstractIntegrationTest {

    /** Plans with services of test@test.com. Method level, a class level @Sql would replace the users of the base class. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql(scripts = "classpath:scripts/test-personal-schedule.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @interface WithServices {
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private ResponseEntity<byte[]> get(String path, UserRole role) {
        return restTemplate.exchange(createURLWithPort("plan/mine") + path, HttpMethod.GET,
            new HttpEntity<>(null, createHeadersWithToken(role)), byte[].class);
    }

    @Test
    @WithServices
    void testShowsUpcomingServicesOfPublishedPlansWithColleagues() throws Exception {
        ResponseEntity<byte[]> response = get("", UserRole.ROLE_USER);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode services = mapper.readTree(response.getBody());
        // the past service and the unpublished plan are left out, sorted by start
        Assertions.assertThat(services.findValuesAsText("id")).containsExactly("ps-e3", "ps-e2");
        JsonNode first = services.get(0);
        Assertions.assertThat(first.get("serviceName").asText()).isEqualTo("Flugleiter");
        Assertions.assertThat(first.get("planName").asText()).isEqualTo("Saison 2099");
        // only who has the same service at the same time, not the user themself
        Assertions.assertThat(first.get("colleagues")).hasSize(1);
        Assertions.assertThat(first.get("colleagues").get(0).asText()).isEqualTo("Kurt Kollege");
        Assertions.assertThat(services.get(1).get("colleagues")).isEmpty();
    }

    @Test
    @WithServices
    void testShowsPastServicesOnRequest() throws Exception {
        JsonNode services = mapper.readTree(get("?past=true", UserRole.ROLE_USER).getBody());
        Assertions.assertThat(services.findValuesAsText("id")).containsExactly("ps-e1", "ps-e3", "ps-e2");
    }

    @Test
    @WithServices
    void testUsersWithoutParticipantHaveNoServices() throws Exception {
        Assertions.assertThat(mapper.readTree(get("", UserRole.ROLE_ADMIN).getBody())).isEmpty();
    }

    @Test
    @WithServices
    void testExportsTheServicesAsCalendar() {
        ResponseEntity<byte[]> response = get("/ics", UserRole.ROLE_USER);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentType().toString()).startsWith("text/calendar");
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("dienste.ics");

        String ics = new String(response.getBody(), StandardCharsets.UTF_8);
        Assertions.assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        Assertions.assertThat(ics.split("BEGIN:VEVENT", -1)).hasSize(3);
        Assertions.assertThat(ics).contains("UID:ps-e3@serviceschedule", "SUMMARY:Flugleiter", "DESCRIPTION:Saison 2099\\nKurt Kollege");
        // Europe/Berlin is UTC+2 in April
        Assertions.assertThat(ics).contains("DTSTART:20990412T070000Z", "DTEND:20990412T113000Z");
        Assertions.assertThat(ics).doesNotContain("ps-e1", "ps-e6");
    }

    @Test
    void testRequiresSignIn() {
        ResponseEntity<String> response = restTemplate.getForEntity(createURLWithPort("plan/mine"), String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
