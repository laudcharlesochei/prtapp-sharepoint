package prt.springbootthymeleafcrudwebapp.sharepoint;

/** A failed call to SharePoint / Microsoft Graph, carrying the HTTP status Graph returned. */
public class SharePointException extends RuntimeException {

    private final int status;

    public SharePointException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    /** A plain-English hint for the most common failures. */
    public String getHint() {
        switch (status) {
            case 401:
                return "Authentication failed. Check the Azure app registration values in the Heroku Config Vars "
                        + "(AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET) and that the client secret has not expired.";
            case 403:
                return "Access denied. In delegated mode your account needs Edit rights on the SharePoint list and the "
                        + "app needs the Sites.ReadWrite.All (delegated) permission. In app mode an administrator must grant "
                        + "admin consent (Sites.ReadWrite.All or Sites.Selected with this site granted).";
            case 404:
                return "Not found. Check SHAREPOINT_SITE_URL and SHAREPOINT_LIST, or the record may have been deleted.";
            case 400:
                return "SharePoint rejected the data. Usually a column name is wrong - open /api/v1/sharepoint/columns "
                        + "and set SHAREPOINT_FIELD_FIRST_NAME / _LAST_NAME / _EMAIL to the internal 'name' values.";
            case 429:
            case 503:
                return "SharePoint is throttling requests. Wait a moment and try again.";
            default:
                return "";
        }
    }
}
