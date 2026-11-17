import java.net.HttpURLConnection;
import java.net.URI;

public class Healthcheck {
    public static void main(String[] args) throws Exception {
        String port = System.getenv("HEALTH_PORT");
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/").toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        int status = connection.getResponseCode();
        connection.disconnect();
        // Endpoints protegidos respondem 401/403 quando o servidor está pronto.
        if (status < 200 || status >= 500) System.exit(1);
    }
}
