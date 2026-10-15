import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.management.Query;

public class Handler {
    private final Map<String, List<String>> header = new HashMap<>();
    private ResponseBuilder builder = ResponseBuilder.newResponseBuilder();

    private DataOutputStream out;

    public Handler() {
    }

    public Handler(byte[] headerBytes, DataOutputStream out) {
        this.out = out;
        String[] lines = new String(headerBytes, StandardCharsets.UTF_8).split("\r\n");

        // i = 1 to skip the first part
        for (int i = 1; i < lines.length; i++) {
            // Split into two parts Content-Length : 11 
            String[] parts = lines[i].split(":", 2);
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid header for parts: " + Arrays.toString(parts));

            String lowerCasedKey = parts[0].toLowerCase();

            if (!header.containsKey(lowerCasedKey)) {
                header.put(lowerCasedKey, new ArrayList<String>());
            }
            List<String> list = header.get(lowerCasedKey);
            list.add(parts[1].trim());
        }

        String line = lines[0];

        int idx = line.indexOf("/");
        String type = line.substring(0, idx).trim();
        int idxWhitespace = line.lastIndexOf(' ');
        String endpoint = line.substring(idx, idxWhitespace);
        int idxQuery = endpoint.indexOf('?');

        if (idxQuery != -1) {
            String handler = endpoint.substring(0, idxQuery);
            String queries = endpoint.substring(idxQuery + 1, endpoint.length());

            header.put("endpoint", List.of(handler));
            header.put("queries", List.of(queries));
            header.put("type", List.of(type));
        } else {
            header.put("endpoint", List.of(endpoint));
            header.put("type", List.of(type));
        }

        System.out.println(new String(headerBytes, StandardCharsets.UTF_8));
    }

    public void sendResponseHeader(int code) {
        this.builder = builder.code(code);
    }

    public static Handler responder(int code) {
        Handler handler = new Handler();
        handler.sendResponseHeader(code);

        return handler;
    }

    public void sendResponse(Optional<byte[]> responseBytes) throws IOException {
        int len = responseBytes.isEmpty() ? 0 : responseBytes.get().length;

        String head = builder.content("text/plain").length(len).connectionAfter("close").build();

        out.write(head.getBytes(StandardCharsets.UTF_8));
        if (len != 0)
            out.write(responseBytes.get());
        out.flush();
    }

    public byte[] readRequest(DataInputStream in) throws IOException {
        // A byte array bounded by 'len' allowed us to readFully to fully populate it and we parse to UTF-8
        // uppercase then back to bytes and pass it to the header text which is a typical HTTP response
        // and we must define the byte length like they had done above with 'content-length'
        int len = Integer.parseInt(get("content-length"));
        byte[] messageBytes = new byte[len];
        in.readFully(messageBytes);

        return messageBytes;
    }

    public static byte[] parseHeader(DataInputStream in) throws IOException {
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

        return headers.toByteArray();
    }

    public String get(String key) {
        if (!header.containsKey(key))
            throw new IllegalArgumentException(String.format("Key %s does not exist in the handelr map"));

        return header.get(key).getFirst();
    }

    public String getQueries() {
        return get("queries");
    }

    public String getType() {
        return get("type");
    }

    public String getEndpoint() {
        return get("endpoint");
    }
}
