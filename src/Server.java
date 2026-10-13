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

                System.out.println(new String(headers.toByteArray(), StandardCharsets.UTF_8));

            }
        } catch (IOException e) {
            System.err.println("Error occured");
            return;
        }
    }
}
