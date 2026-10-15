import java.net.InetAddress;

public abstract class Server {
    abstract public void start();

    abstract public void stop();

    abstract public void createContext(String endpoint, FunctionalHandler handler);

    // Visible due to package private
    public static Server create(InetAddress addr, int port) {
        return new ServerImpl(addr, port);
    }
}