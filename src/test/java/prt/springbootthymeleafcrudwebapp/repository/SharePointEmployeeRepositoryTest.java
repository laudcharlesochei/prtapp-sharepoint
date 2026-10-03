package prt.springbootthymeleafcrudwebapp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import prt.springbootthymeleafcrudwebapp.config.SharePointProperties;
import prt.springbootthymeleafcrudwebapp.model.Employee;
import prt.springbootthymeleafcrudwebapp.sharepoint.SharePointException;

class SharePointEmployeeRepositoryTest {

    private static final String SITE = "dmail.sharepoint.com,1111,2222";
    private static final String LIST = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String G = SharePointEmployeeRepository.GRAPH;

    private MockRestServiceServer server;
    private SharePointEmployeeRepository repo;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SharePointProperties props = new SharePointProperties();
        repo = new SharePointEmployeeRepository(builder.build(), () -> "test-token", props);
    }

    private void expectSiteAndListLookup() {
        server.expect(requestTo(G + "/sites/dmail.sharepoint.com:/sites/MentorMeetingSystem?$select=id"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"id\":\"" + SITE + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(G + "/sites/" + SITE + "/lists?$select=id,name,displayName"))
                .andRespond(withSuccess("{\"value\":[{\"id\":\"x\",\"name\":\"Documents\",\"displayName\":\"Documents\"},"
                        + "{\"id\":\"" + LIST + "\",\"name\":\"employee\",\"displayName\":\"employee\"}]}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void findAllMapsListItemsToEmployees() {
        expectSiteAndListLookup();
        server.expect(requestTo(G + "/sites/" + SITE + "/lists/" + LIST + "/items?$expand=fields&$top=500"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"value\":["
                        + "{\"id\":\"7\",\"fields\":{\"Title\":\"b@x\",\"FirstName\":\"Bea\",\"LastName\":\"B\",\"Email\":\"b@x\"}},"
                        + "{\"id\":\"3\",\"fields\":{\"Title\":\"a@x\",\"FirstName\":\"Ann\",\"LastName\":\"A\",\"Email\":\"a@x\"}}"
                        + "]}", MediaType.APPLICATION_JSON));

        List<Employee> all = repo.findAll();

        assertEquals(2, all.size());
        assertEquals(3L, all.get(0).getId());
        assertEquals("Ann", all.get(0).getFirstName());
        assertEquals("b@x", all.get(1).getEmail());
        server.verify();
    }

    @Test
    void saveNewEmployeePostsFieldsAndStoresNewId() {
        expectSiteAndListLookup();
        server.expect(requestTo(G + "/sites/" + SITE + "/lists/" + LIST + "/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"fields\":{\"FirstName\":\"Ann\",\"LastName\":\"A\",\"Email\":\"a@x\",\"Title\":\"a@x\"}}"))
                .andRespond(withSuccess("{\"id\":\"12\",\"fields\":{}}", MediaType.APPLICATION_JSON));

        Employee e = repo.save(new Employee(0, "Ann", "A", "a@x"));

        assertEquals(12L, e.getId());
        server.verify();
    }

    @Test
    void saveExistingEmployeePatchesFields() {
        expectSiteAndListLookup();
        server.expect(requestTo(G + "/sites/" + SITE + "/lists/" + LIST + "/items/12/fields"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        repo.save(new Employee(12, "Ann", "A", "a@x"));
        server.verify();
    }

    @Test
    void graphErrorsBecomeSharePointException() {
        server.expect(requestTo(G + "/sites/dmail.sharepoint.com:/sites/MentorMeetingSystem?$select=id"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"accessDenied\",\"message\":\"Access denied\"}}"));

        SharePointException ex = assertThrows(SharePointException.class, () -> repo.findAll());
        assertEquals(403, ex.getStatus());
    }

    @Test
    void sitePathIsExtractedFromAnySharePointUrl() {
        assertEquals("dmail.sharepoint.com:/sites/MentorMeetingSystem", SharePointEmployeeRepository.sitePathFromUrl(
                "https://dmail.sharepoint.com/sites/MentorMeetingSystem/Lists/employee/AllItems.aspx?env=WebViewList"));
        assertEquals("dmail.sharepoint.com:/sites/MentorMeetingSystem",
                SharePointEmployeeRepository.sitePathFromUrl("https://dmail.sharepoint.com/sites/MentorMeetingSystem/"));
    }

    @Test
    void titleColumnIsFilledFromConfiguredSource() {
        SharePointProperties.Fields map = new SharePointProperties.Fields();
        map.setTitleFrom("fullName");
        Map<String, Object> f = SharePointEmployeeRepository.toFields(new Employee(0, "Ann", "Smith", "a@x"), map);
        assertEquals("Ann Smith", f.get("Title"));
    }
}
