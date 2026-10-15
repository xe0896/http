import java.io.IOException;

@FunctionalInterface
public interface FunctionalHandler {
    void handle(Handler handler) throws IOException;
}