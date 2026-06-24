package ca.bc.gov.nrs.userlookup.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

/**
 * SOAP message sender that adds an HTTP Basic {@code Authorization} header to
 * every outbound request, mirroring the proxy's base64 credential header.
 */
public class BasicAuthMessageSender extends HttpUrlConnectionMessageSender {

  private final String authHeader;

  public BasicAuthMessageSender(String username, String password) {
    String raw = (username == null ? "" : username) + ":" + (password == null ? "" : password);
    this.authHeader = "Basic " + Base64.getEncoder()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void prepareConnection(HttpURLConnection connection) throws IOException {
    connection.setRequestProperty("Authorization", authHeader);
    super.prepareConnection(connection);
  }
}
