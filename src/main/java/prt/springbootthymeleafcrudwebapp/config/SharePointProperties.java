package prt.springbootthymeleafcrudwebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings bound from {@code sharepoint.*} in application.properties / Heroku Config Vars.
 */
@ConfigurationProperties(prefix = "sharepoint")
public class SharePointProperties {

    public enum AuthMode {
        /** Users sign in with their University account; Graph calls run as that user. */
        DELEGATED,
        /** App-only access with the client secret (needs admin consent). */
        APP
    }

    private AuthMode authMode = AuthMode.DELEGATED;
    private String siteUrl = "https://dmail.sharepoint.com/sites/MentorMeetingSystem";
    private String siteId;
    private String list = "employee";
    private Fields fields = new Fields();

    public static class Fields {
        private String firstName = "FirstName";
        private String lastName = "LastName";
        private String email = "Email";
        private String titleFrom = "email";

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTitleFrom() { return titleFrom; }
        public void setTitleFrom(String titleFrom) { this.titleFrom = titleFrom; }
    }

    public AuthMode getAuthMode() { return authMode; }
    public void setAuthMode(AuthMode authMode) { this.authMode = authMode; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getList() { return list; }
    public void setList(String list) { this.list = list; }
    public Fields getFields() { return fields; }
    public void setFields(Fields fields) { this.fields = fields; }

    public boolean isDelegated() {
        return authMode == AuthMode.DELEGATED;
    }
}
