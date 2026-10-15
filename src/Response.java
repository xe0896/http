import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Response {
    private ResponseBuilder builder = ResponseBuilder.newResponseBuilder();
    private DataOutputStream out;

    public Response(DataOutputStream out) {
        this.out = out;
    }

    public void sendResponseHeader(int code) {
        this.builder = builder.code(code);
    }

    public void sendResponse(Optional<byte[]> responseBytes) throws IOException {
        int len = responseBytes.isEmpty() ? 0 : responseBytes.get().length;

        String head = builder.content("text/plain").length(len).connectionAfter("close").build();

        out.write(head.getBytes(StandardCharsets.UTF_8));
        if (len != 0)
            out.write(responseBytes.get());
        out.flush();
    }
}
