import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;

    public Server(InetSocketAddress addr) throws IOException {
        serverSocket = new ServerSocket();
        // Binding to a specific address makes it not go to the wildcard address
        serverSocket.bind(addr);
    }

    public static Map<String, List<String>> generateHeaderMap(String[] lines) {
        Map<String, List<String>> headerMap = new HashMap<>();

        // i = 1 to skip the first part
        for (int i = 1; i < lines.length; i++) {
            // Split into two parts Content-Length : 11 
            String[] parts = lines[i].split(":", 2);
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid header for parts: " + Arrays.toString(parts));

            String lowerCasedKey = parts[0].toLowerCase();

            if (!headerMap.containsKey(parts[0])) {
                headerMap.put(lowerCasedKey, new ArrayList<String>());
            }
            List<String> list = headerMap.get(lowerCasedKey);
            list.add(parts[1].trim());
        }

        return headerMap;
    }

    public static void handlePost(String[] lines, DataOutputStream out, DataInputStream in) throws IOException {
        Map<String, List<String>> headerMap = generateHeaderMap(lines);
        System.out.println(headerMap);
        int len = Integer.parseInt(headerMap.get("content-length").getFirst());

        // A byte array bounded by 'len' allowed us to readFully to fully populate it and we parse to UTF-8
        // uppercase then back to bytes and pass it to the header text which is a typical HTTP response
        // and we must define the byte length like they had done above with 'content-length'
        byte[] messageBytes = new byte[len];
        in.readFully(messageBytes);
        String message = new String(messageBytes, StandardCharsets.UTF_8).toUpperCase();
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);

        String head = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + responseBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(responseBytes);
        out.flush();
    }

    public static void handleGet(String[] lines, DataOutputStream out, DataInputStream in) {
        Map<String, List<String>> headerMap = generateHeaderMap(lines);
    }

    public static void handleHead(String[] lines, DataOutputStream out, DataInputStream in) {
        Map<String, List<String>> headerMap = generateHeaderMap(lines);
        throw new UnsupportedOperationException("Have not implemented HEAD yet");
    }

    public void handleClient(DataInputStream in, DataOutputStream out) throws IOException {
        int b;

        // Save our header as we go through the HTTP request
        ByteArrayOutputStream headers = new ByteArrayOutputStream();
        byte[] arr = { '\r', '\n', '\r', '\n' };
        byte[] window = new byte[4]; // Sliding window

        // in.read() would be -1 once the user disconnects, else we get a singular byte to work with
        while ((b = in.read()) != -1) {
            // Record the header so we do not lose it as we go through this
            headers.write(b);

            // Slide our window across in order as read
            window[0] = window[1];
            window[1] = window[2];
            window[2] = window[3];
            window[3] = (byte) b;

            // If they are equal to 'arr' then the window is now on "\r\n\r\n" so the body is now next
            if (Arrays.equals(arr, window))
                break;
        }

        String header = new String(headers.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(header);

        // A HTTP request is seperated by "\r\n" for all the fields, except the first one which would
        // be the request type as well as the endpoint, we skip that with i = 1 and break it into two
        // via ':' to populate our map
        String[] lines = header.split("\r\n");

        // POST / HTTP1.1
        int idx = lines[0].indexOf("/");
        String type = lines[0].substring(0, idx).trim();
        int idxWhitespace = lines[0].lastIndexOf(' ');
        String endpoint = lines[0].substring(idx, idxWhitespace);
        int idxQuery = endpoint.indexOf('?');

        if (idxQuery != -1) {
            String handler = endpoint.substring(0, idxQuery);
            String queries = endpoint.substring(idxQuery + 1, endpoint.length());

            System.out.println(endpoint);

            System.out.println(handler);
            System.out.println(queries);
        }

        // Dispatches to the relevant type
        try {
            switch (type) {
                case "POST" -> handlePost(lines, out, in);
                case "HEAD" -> handleHead(lines, out, in);
                case "GET" -> handleGet(lines, out, in);
                default -> System.out.println("Unknown or not implemented request type: " + type);
            }
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            System.out
                    .println(String.format("Listening at %s on port %d", serverSocket.getInetAddress().getHostAddress(),
                            serverSocket.getLocalPort()));
            while (!serverSocket.isClosed()) {
                // Waits for a client to come
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}