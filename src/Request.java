import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final Map<String, List<String>> header = new HashMap<>();
    private final Map<String, String> queries = new HashMap<>();

    private DataInputStream in;
    private byte[] messageBytes = new byte[0];

    public Request(byte[] headerBytes, DataInputStream in) throws IOException, IllegalArgumentException {
        this.in = in;
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
        header.put("type", List.of(type)); // Every request has a 'type'

        if (idxQuery != -1) {
            String handler = endpoint.substring(0, idxQuery);
            String queries = endpoint.substring(idxQuery + 1, endpoint.length());

            header.put("endpoint", List.of(handler));
            header.put("queries", List.of(queries));
        } else {
            header.put("endpoint", List.of(endpoint));
        }

        if (header.containsKey("queries")) {
            System.out.println(header.get("queries").getFirst());
            String[] q = header.get("queries").getFirst().split("&");
            for (String query : q) {
                String[] pairs = query.split("=", 2);
                if (pairs.length != 2 || pairs[1].length() == 0)
                    throw new IllegalArgumentException();

                queries.put(decodePercent(pairs[0]), decodePercent(pairs[1]));
            }
        }

        System.out.println(new String(headerBytes, StandardCharsets.UTF_8));

        // A header with no content-length has no body so there is no message to read
        if (header.containsKey("content-length"))
            messageBytes = readRequest();
    }

    private String decodePercent(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '%' && str.charAt(i + 1) == '2' && str.charAt(i + 2) == '0') {
                i += 2;
                sb.append(' ');
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    public byte[] getRequest() {
        return messageBytes;
    }

    private byte[] readRequest() throws IOException, EOFException {

        // A byte array bounded by 'len' allowed us to readFully to fully populate it and we parse to UTF-8
        // uppercase then back to bytes and pass it to the header text which is a typical HTTP response
        // and we must define the byte length like they had done above with 'content-length'

        int len = Integer.parseInt(get("content-length"));
        if (len < 0)
            throw new NumberFormatException("Negative 'Content-Length' is not allowed");
        byte[] messageBytes = new byte[len];
        in.readFully(messageBytes);
        return messageBytes;
    }

    public String get(String key) {
        if (!header.containsKey(key))
            throw new IllegalArgumentException(String.format("Key %s does not exist in the handler map", key));

        return header.get(key).getFirst();
    }

    public String query(String key) {
        return queries.get(key);
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
}