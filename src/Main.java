import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class Main {
    public static void main(String[] args) throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 8080;
        Server server = Server.create(address, port);

        server.get("/hello", handler -> {
            handler.sendResponseHeader(200);
            handler.sendResponse(Optional.of("hello endpoint\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.get("/", handler -> {
            handler.sendResponseHeader(200);
            handler.sendResponse(Optional.of("root endpoint\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.post("/hello", handler -> {
            handler.sendResponseHeader(200);
            handler.sendResponse(Optional.of("post endpoint\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.start();
    }
}