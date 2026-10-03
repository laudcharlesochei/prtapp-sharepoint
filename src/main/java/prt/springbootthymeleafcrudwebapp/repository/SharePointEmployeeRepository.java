package prt.springbootthymeleafcrudwebapp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import prt.springbootthymeleafcrudwebapp.config.SharePointProperties;
import prt.springbootthymeleafcrudwebapp.model.Employee;
import prt.springbootthymeleafcrudwebapp.sharepoint.GraphTokenProvider;
import prt.springbootthymeleafcrudwebapp.sharepoint.SharePointException;

/**
 * Replaces the old JPA EmployeeRepository (JawsDB MySQL).
 * Reads and writes the SharePoint Online list through the Microsoft Graph REST API:
 *
 *   GET    /sites/{host}:/sites/{name}                     resolve site id
 *   GET    /sites/{site}/lists                             resolve list id from its name
 *   GET    /sites/{site}/lists/{list}/items?expand=fields  list items
 *   GET    /sites/{site}/lists/{list}/items/{id}?expand=fields
 *   POST   /sites/{site}/lists/{list}/items                {"fields": {...}}
 *   PATCH  /sites/{site}/lists/{list}/items/{id}/fields    {...}
 *   DELETE /sites/{site}/lists/{list}/items/{id}
 */
@Repository
public class SharePointEmployeeRepository {

    public static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private static final Pattern GUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;
    private final GraphTokenProvider tokens;
    private final SharePointProperties props;

    private volatile String siteId;
    private volatile String listId;

    public SharePointEmployeeRepository(RestClient graphRestClient, GraphTokenProvider tokens,
                                        SharePointProperties props) {
        this.http = graphRestClient;
        this.tokens = tokens;
        this.props = props;
    }

    // ------------------------------------------------------------------ CRUD

    public List<Employee> findAll() {
        List<Employee> result = new ArrayList<>();
        String url = itemsUrl() + "?$expand=fields&$top=500";
        while (url != null) {
            JsonNode page = get(url);
            for (JsonNode item : page.path("value")) {
                result.add(toEmployee(item));
            }
            url = page.hasNonNull("@odata.nextLink") ? page.get("@odata.nextLink").asText() : null;
        }
        result.sort(Comparator.comparingLong(Employee::getId));
        return result;
    }

    public Optional<Employee> findById(long id) {
        try {
            return Optional.of(toEmployee(get(itemsUrl() + "/" + id + "?$expand=fields")));
        } catch (SharePointException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Employee save(Employee employee) {
        Map<String, Object> fields = toFields(employee, props.getFields());
        if (employee.getId() > 0) {
            send(http.patch(), itemsUrl() + "/" + employee.getId() + "/fields", fields);
            return employee;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fields", fields);
        JsonNode created = send(http.post(), itemsUrl(), body);
        employee.setId(parseId(created.path("id").asText()));
        return employee;
    }

    public void deleteById(long id) {
        http.delete()
                .uri(URI.create(itemsUrl() + "/" + id))
                .headers(h -> h.setBearerAuth(tokens.getAccessToken()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, SharePointEmployeeRepository::raise)
                .toBodilessEntity();
    }

    // ------------------------------------------------------- diagnostics

    /** Visible columns of the list - use the "name" values in SHAREPOINT_FIELD_* settings. */
    public List<Map<String, Object>> listColumns() {
        List<Map<String, Object>> columns = new ArrayList<>();
        JsonNode page = get(listUrl() + "/columns");
        for (JsonNode c : page.path("value")) {
            if (c.path("hidden").asBoolean(false)) {
                continue;
            }
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("name", c.path("name").asText());
            col.put("displayName", c.path("displayName").asText());
            col.put("readOnly", c.path("readOnly").asBoolean(false));
            columns.add(col);
        }
        return columns;
    }

    public Map<String, Object> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("authMode", props.getAuthMode().name().toLowerCase());
        s.put("siteUrl", props.getSiteUrl());
        s.put("siteId", resolveSiteId());
        s.put("listId", resolveListId());
        s.put("fieldMapping", toFieldsTemplate(props.getFields()));
        return s;
    }

    // ------------------------------------------------------------ mapping

    Employee toEmployee(JsonNode item) {
        JsonNode f = item.path("fields");
        SharePointProperties.Fields map = props.getFields();
        Employee e = new Employee();
        e.setId(parseId(item.path("id").asText()));
        e.setFirstName(f.path(map.getFirstName()).asText(null));
        e.setLastName(f.path(map.getLastName()).asText(null));
        e.setEmail(f.path(map.getEmail()).asText(null));
        return e;
    }

    static Map<String, Object> toFields(Employee e, SharePointProperties.Fields map) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(map.getFirstName(), e.getFirstName());
        fields.put(map.getLastName(), e.getLastName());
        fields.put(map.getEmail(), e.getEmail());
        // SharePoint lists always have a required 'Title' column. Fill it unless already mapped.
        if (!fields.containsKey("Title")) {
            String titleFrom = map.getTitleFrom() == null ? "email" : map.getTitleFrom().trim();
            String title;
            switch (titleFrom.toLowerCase()) {
                case "none":
                    title = null;
                    break;
                case "firstname":
                    title = e.getFirstName();
                    break;
                case "lastname":
                    title = e.getLastName();
                    break;
                case "fullname":
                    title = ((e.getFirstName() == null ? "" : e.getFirstName()) + " "
                            + (e.getLastName() == null ? "" : e.getLastName())).trim();
                    break;
                default:
                    title = e.getEmail();
            }
            if (title != null) {
                fields.put("Title", title);
            }
        }
        return fields;
    }

    private static Map<String, Object> toFieldsTemplate(SharePointProperties.Fields map) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("firstName", map.getFirstName());
        m.put("lastName", map.getLastName());
        m.put("email", map.getEmail());
        m.put("titleFrom", map.getTitleFrom());
        return m;
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new SharePointException(500, "Unexpected SharePoint item id: " + id);
        }
    }

    // ------------------------------------------------- site / list lookup

    private String itemsUrl() {
        return listUrl() + "/items";
    }

    private String listUrl() {
        return GRAPH + "/sites/" + resolveSiteId() + "/lists/" + resolveListId();
    }

    private String resolveSiteId() {
        String id = siteId;
        if (id == null) {
            synchronized (this) {
                if (siteId == null) {
                    if (StringUtils.hasText(props.getSiteId())) {
                        siteId = props.getSiteId().trim();
                    } else {
                        siteId = get(GRAPH + "/sites/" + sitePathFromUrl(props.getSiteUrl()) + "?$select=id")
                                .path("id").asText();
                    }
                }
                id = siteId;
            }
        }
        return id;
    }

    /** "https://dmail.sharepoint.com/sites/MentorMeetingSystem/Lists/employee/AllItems.aspx"
     *  becomes "dmail.sharepoint.com:/sites/MentorMeetingSystem". */
    static String sitePathFromUrl(String siteUrl) {
        URI uri = URI.create(siteUrl.trim());
        String[] parts = uri.getRawPath().split("/");
        StringBuilder path = new StringBuilder();
        // keep "/sites/<name>" or "/teams/<name>" only
        for (int i = 1; i < parts.length && i <= 2; i++) {
            if (!parts[i].isEmpty()) {
                path.append('/').append(parts[i]);
            }
        }
        if (path.length() == 0) {
            return uri.getHost();   // root site
        }
        return uri.getHost() + ":" + path;
    }

    private String resolveListId() {
        String id = listId;
        if (id == null) {
            synchronized (this) {
                if (listId == null) {
                    String wanted = props.getList().trim();
                    if (GUID.matcher(wanted).matches()) {
                        listId = wanted;
                    } else {
                        listId = findListIdByName(wanted);
                    }
                }
                id = listId;
            }
        }
        return id;
    }

    private String findListIdByName(String wanted) {
        List<String> seen = new ArrayList<>();
        String url = GRAPH + "/sites/" + resolveSiteId() + "/lists?$select=id,name,displayName";
        while (url != null) {
            JsonNode page = get(url);
            for (JsonNode l : page.path("value")) {
                String name = l.path("name").asText();
                String display = l.path("displayName").asText();
                if (wanted.equalsIgnoreCase(name) || wanted.equalsIgnoreCase(display)) {
                    return l.path("id").asText();
                }
                seen.add(display);
            }
            url = page.hasNonNull("@odata.nextLink") ? page.get("@odata.nextLink").asText() : null;
        }
        throw new SharePointException(404, "SharePoint list '" + wanted + "' was not found on "
                + props.getSiteUrl() + ". Lists available: " + seen);
    }

    // --------------------------------------------------------- HTTP helpers

    private JsonNode get(String url) {
        return http.get()
                .uri(URI.create(url))
                .headers(h -> h.setBearerAuth(tokens.getAccessToken()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, SharePointEmployeeRepository::raise)
                .body(JsonNode.class);
    }

    private JsonNode send(RestClient.RequestBodyUriSpec spec, String url, Object body) {
        return spec
                .uri(URI.create(url))
                .headers(h -> h.setBearerAuth(tokens.getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, SharePointEmployeeRepository::raise)
                .body(JsonNode.class);
    }

    private static void raise(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String message = raw;
        try {
            JsonNode err = JSON.readTree(raw).path("error");
            if (err.hasNonNull("message")) {
                message = err.path("code").asText("") + ": " + err.path("message").asText();
            }
        } catch (IOException ignored) {
            // body was not JSON - keep raw text
        }
        throw new SharePointException(status, "Microsoft Graph returned HTTP " + status
                + " for " + request.getMethod() + " " + request.getURI().getPath() + " - " + message);
    }
}
