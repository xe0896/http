import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class Main {
    public static void main(String[] args) throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 8080;
        Server server = Server.create(address, port);

        server.get("/hello", (req, res) -> {
            res.sendResponseHeader(200);
            res.sendResponse(Optional.of("hello endpoint\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.get("/", (req, res) -> {
            res.sendResponseHeader(200);
            res.sendResponse(Optional.of("root endpoint\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.post("/hello", (req, res) -> {
            res.sendResponseHeader(200);
            byte[] messageBytes = req.readRequest();
            byte[] response = new String(messageBytes, StandardCharsets.UTF_8).toUpperCase()
                    .getBytes(StandardCharsets.UTF_8);
            res.sendResponse(Optional.of(response));
        });

        server.start();
    }
}