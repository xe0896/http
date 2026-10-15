import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ServerTest {
    static InetAddress address = InetAddress.getLoopbackAddress();
    static int port = 8080;
    static HttpClient client = HttpClient.newHttpClient();
    static String URL = "http://localhost:8080";
    private static final Server server = Server.create(address, port);

    @BeforeAll
    static void setup() throws IOException {
        server.get("/ping", (req, res) -> {
            res.sendResponseHeader(200);
            res.sendResponse(Optional.of("pong\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.post("/capitalise", (req, res) -> {
            res.sendResponseHeader(200);
            byte[] messageBytes = req.readRequest();
            byte[] response = new String(messageBytes, StandardCharsets.UTF_8).toUpperCase()
                    .getBytes(StandardCharsets.UTF_8);
            res.sendResponse(Optional.of(response));
        });

        new Thread(() -> {
            server.start();
        }).start();
    }

    @Test
    public void hello() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/ping"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                req,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("pong\n", response.body());
    }

    @Test
    public void capitalise() throws Exception {
        String request = "this is my request";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/capitalise"))
                .GET()
                .POST(BodyPublishers.ofString(request))
                .build();

        HttpResponse<String> response = client.send(
                req,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(request.toUpperCase(), response.body());
    }

    @AfterAll
    static void stop() {
        server.stop();
    }
}