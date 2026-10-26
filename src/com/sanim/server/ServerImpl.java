package com.sanim.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerImpl extends Server {

    public record endpoints(Map<String, FunctionalHandler> get, Map<String, FunctionalHandler> post) {
    }

    private InetSocketAddress addr;
    private ServerSocket serverSocket;
    private endpoints end = new endpoints(new HashMap<>(), new HashMap<>());

    ServerImpl(InetAddress addr, int port) {
        this.addr = new InetSocketAddress(addr, port);
    }

    public void error(Response res, int code) throws IOException {
        res.sendResponseHeader(code);
        res.sendResponse(Optional.empty());
    }

    private FunctionalHandler grabCallback(String type, String endpoint) {
        Map<String, FunctionalHandler> get = end.get();
        Map<String, FunctionalHandler> post = end.post();
        switch (type) {
            case "POST" -> {
                if (post.containsKey(endpoint))
                    return post.get(endpoint);

                if (get.containsKey(endpoint))
                    throw new IllegalCallerException("405");
            }
            case "GET" -> {
                if (get.containsKey(endpoint))
                    return get.get(endpoint);

                if (post.containsKey(endpoint))
                    throw new IllegalCallerException("405");
            }
        }

        throw new IllegalCallerException("404");
    }

    public void handleClient(Socket client, DataInputStream in, DataOutputStream out) throws IOException {
        Response res = new Response(out);
        try {
            byte[] headers = Request.parseHeader(in);
            Request req = new Request(headers, end, in);

            String potentialEndpoint = req.checkParameters();
            if (potentialEndpoint != null) {
                FunctionalHandler callback = grabCallback(req.getType(), potentialEndpoint);
                callback.handle(req, res);
            } else {
                FunctionalHandler callback = grabCallback(req.getType(), req.getEndpoint());
                callback.handle(req, res);
            }
        } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
            if (!client.isClosed())
                error(res, 400);
        } catch (IllegalCallerException e) {
            int code = Integer.parseInt(e.getMessage());
            if (!client.isClosed())
                error(res, code);
        } catch (EOFException e) {
            System.out.println("User left mid way");
        }
    }

    @Override
    public void start() {
        try {
            this.serverSocket = new ServerSocket();
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
                handleClient(client, in, out);

                client.close();
            }

            serverSocket.close();

        } catch (SocketException e) {
            System.out.println("Socket exception: a thread was blocked at accept()");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            serverSocket.close();
            System.out.println("Stopped server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void get(String endpoint, FunctionalHandler handler) {
        end.get().put(endpoint, handler);
    }

    @Override
    public void post(String endpoint, FunctionalHandler handler) {
        end.post().put(endpoint, handler);
    }
}
