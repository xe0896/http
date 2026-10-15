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

        server.createContext("/hello", handler -> {
            handler.sendResponseHeader(200);
            handler.sendResponse(Optional.of("back\n".getBytes(StandardCharsets.UTF_8)));
        });

        server.createContext("/", handler -> {
        });

        server.start();
    }
}