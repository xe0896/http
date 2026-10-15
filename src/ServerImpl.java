import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerImpl extends Server {
    private final Map<String, FunctionalHandler> handlers = new HashMap<>();

    private InetSocketAddress addr;

    ServerImpl(InetAddress addr, int port) {
        this.addr = new InetSocketAddress(addr, port);
    }

    public void send404(Handler handler) throws IOException {
        handler.sendResponseHeader(404);
        handler.sendResponse(Optional.empty());
    }

    public void handleClient(DataInputStream in, DataOutputStream out) throws IOException {
        byte[] headers = Handler.parseHeader(in);
        Handler handler = new Handler(headers, out);

        FunctionalHandler function = handlers.get(handler.getEndpoint());

        if (function == null) {
            send404(handler);
            return;
        }

        function.handle(handler);
    }

    @Override
    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(addr);

            System.out.println(String.format("Listening at %s on port %d", addr.getHostString(), addr.getPort()));

            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                // Setup streams for communication
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                System.out.println("Connection established with " + client.getRemoteSocketAddress());
                // A byte as we read up until we find "\r\n\r\n" which is the signal that
                // the bytes proceeding would be the body

                // Handles a singular client
                handleClient(in, out);

                client.close();
            }

            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("Stopped server");
    }

    @Override
    public void createContext(String endpoint, FunctionalHandler handler) {
        handlers.put(endpoint, handler);
    }
}
