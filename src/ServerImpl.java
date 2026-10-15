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
import java.util.logging.Handler;

public class ServerImpl extends Server {
    private final Map<String, FunctionalHandler> get = new HashMap<>();
    private final Map<String, FunctionalHandler> post = new HashMap<>();

    private InetSocketAddress addr;

    ServerImpl(InetAddress addr, int port) {
        this.addr = new InetSocketAddress(addr, port);
    }

    public void error(Response res, int code) throws IOException {
        res.sendResponseHeader(code);
        res.sendResponse(Optional.empty());
    }

    private FunctionalHandler grabCallback(String type, String endpoint) {
        switch (type) {
            case "POST" -> {
                if (post.containsKey(endpoint))
                    return post.get(endpoint);

                if (get.containsKey(endpoint))
                    throw new IllegalArgumentException("405");
            }
            case "GET" -> {
                if (get.containsKey(endpoint))
                    return get.get(endpoint);

                if (post.containsKey(endpoint))
                    throw new IllegalArgumentException("405");
            }
        }

        throw new IllegalArgumentException("404");
    }

    public void handleClient(DataInputStream in, DataOutputStream out) throws IOException {
        byte[] headers = Request.parseHeader(in);
        Request req = new Request(headers, out, in);
        Response res = new Response(out);
        try {
            FunctionalHandler callback = grabCallback(req.getType(), req.getEndpoint());
            callback.handle(req, res);
        } catch (IllegalArgumentException e) {
            int code = Integer.parseInt(e.getMessage());
            error(res, code);
        }
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
    public void get(String endpoint, FunctionalHandler handler) {
        get.put(endpoint, handler);
    }

    @Override
    public void post(String endpoint, FunctionalHandler handler) {
        post.put(endpoint, handler);
    }
}
