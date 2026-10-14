public class ResponseBuilder {
    private final StringBuilder sb = new StringBuilder();

    private ResponseBuilder() {
    }

    public ResponseBuilder code(int code, String reason) {
        sb.append(String.format("HTTP/1.1 %d %s\r\n", code, reason));
        return this;
    }

    public ResponseBuilder content(String type) {
        sb.append(String.format("Content-Type: %s\r\n", type));
        return this;
    }

    public ResponseBuilder length(int length) {
        sb.append(String.format("Content-Length: %d\r\n", length));
        return this;
    }

    public ResponseBuilder connectionAfter(String status) {
        sb.append(String.format("Connection: %s\r\n", status));
        return this;
    }

    public String build() {
        sb.append("\r\n");
        return sb.toString();
    }

    public static ResponseBuilder newResponseBuilder() {
        return new ResponseBuilder();
    }

}
