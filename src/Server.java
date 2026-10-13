import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;

    public Server(InetSocketAddress addr) throws IOException {
        serverSocket = new ServerSocket();
        // Binding to a specific address makes it not go to the wildcard address
        serverSocket.bind(addr);
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

                while (true) { // Process indefinitely
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

                    Map<String, String> headerMap = new HashMap<>();
                    // A HTTP request is seperated by "\r\n" for all the fields, except the first one which would
                    // be the request type as well as the endpoint, we skip that with i = 1 and break it into two
                    // via ':' to populate our map
                    String[] lines = new String(headers.toByteArray(), StandardCharsets.UTF_8).split("\r\n");

                    int idx = lines[0].indexOf("/");
                    String type = lines[0].substring(0, idx).trim();

                    System.out.println(type);

                    for (int i = 1; i < lines.length; i++) {
                        String[] parts = lines[i].split(":", 2);
                        if (parts.length != 2)
                            throw new IllegalArgumentException("Invalid header for parts: " + Arrays.toString(parts));
                        headerMap.put(parts[0].toLowerCase(), parts[1].trim());
                    }

                    int len = Integer.parseInt(headerMap.get("content-length"));

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
                    break;
                }

                client.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
