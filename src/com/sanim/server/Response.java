package com.sanim.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Response {
   private ResponseBuilder builder = ResponseBuilder.newResponseBuilder();
   private final List<Runnable> closeHandlers = new CopyOnWriteArrayList<>();
   private Thread watcher;
   private DataOutputStream out;
   private DataInputStream in;

   public Response(DataInputStream in, DataOutputStream out) {
      this.out = out;
      this.in = in;
   }

   public void sendResponseHeader(int code) {
      this.builder = builder.code(code);
   }

   public synchronized void onClose(Runnable handler) {
      closeHandlers.add(handler);
      if (watcher != null)
         return;
      watcher = Thread.startVirtualThread(() -> {
         try {
            while (in.read() != -1) {
            } // nothing should arrive; wait for the close
         } catch (IOException ignored) {
         } // reset or socket closed also counts
         closeHandlers.forEach(Runnable::run);
      });
   }

   public void sendResponse(Optional<byte[]> responseBytes) throws IOException {
      int len = responseBytes.isEmpty() ? 0 : responseBytes.get().length;

      String head = builder.content("text/plain").length(len).connectionAfter("close").build();

      System.out.println(head);

      out.write(head.getBytes(StandardCharsets.UTF_8));
      if (len != 0) {
         System.out.println(new String(responseBytes.get(), StandardCharsets.UTF_8) + "\n");
         out.write(responseBytes.get());
      }

      System.out.println("-------------------------------------------------------");
      out.flush();
   }

   public void startStream() throws IOException {
      out.write(("HTTP/1.1 200 OK\r\n"
            + "Content-Type: text/event-stream\r\n"
            + "Cache-Control: no-cache\r\n"
            + "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
      out.flush();
   }

   public void sendByte(byte[] data) throws IOException {
      out.write(data);
      out.flush();
   }

   public void sendEvent(String event, String data) throws IOException {
      out.write(("event: " + event + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
      out.flush();
   }
}
