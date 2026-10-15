import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

public class ServerTest {
    private static Example server;

    @BeforeAll
    static void setup() throws IOException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 8080;
        server = new Example(new InetSocketAddress(address, port));
        server.start();
    }
}