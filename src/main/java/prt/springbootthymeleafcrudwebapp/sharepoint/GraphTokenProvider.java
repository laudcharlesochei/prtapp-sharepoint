package prt.springbootthymeleafcrudwebapp.sharepoint;

/** Supplies a bearer token for https://graph.microsoft.com. */
@FunctionalInterface
public interface GraphTokenProvider {
    String getAccessToken();
}
