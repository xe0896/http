import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ServerTest {
	static InetAddress address = InetAddress.getLoopbackAddress();
	static int port = 8080;
	static HttpClient client = HttpClient.newHttpClient();
	static String URL = "http://localhost:8080";
	private static final Server server = Server.create(address, port);

	@BeforeAll
	static void setup() throws IOException {
		server.get("/ping", (req, res) -> {
			res.sendResponseHeader(200);
			res.sendResponse(Optional.of("pong\n".getBytes(StandardCharsets.UTF_8)));
		});

		server.post("/capitalise", (req, res) -> {
			res.sendResponseHeader(200);
			byte[] messageBytes = req.getRequest();
			byte[] response = new String(messageBytes, StandardCharsets.UTF_8).toUpperCase()
					.getBytes(StandardCharsets.UTF_8);
			res.sendResponse(Optional.of(response));
		});

		server.get("/alotofspaces", (req, res) -> {
			String firstName = req.query("first").toUpperCase();
			String lastName = req.query("last").toUpperCase();
			String date = req.query("date").toUpperCase();

			String concat = firstName.concat(lastName).concat(date);

			byte[] response = concat.getBytes(StandardCharsets.UTF_8);
			res.sendResponseHeader(200);
			res.sendResponse(Optional.of(response));
		});

		new Thread(() -> {
			server.start();
		}).start();
	}

	@Test
	public void hello() throws Exception {
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/ping"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("pong\n", response.body());
	}

	@Test
	public void capitalise() throws Exception {
		String request = "this is my request";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/capitalise"))
				.POST(BodyPublishers.ofString(request))
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals(request.toUpperCase(), response.body());
	}

	@Test
	public void missing() throws Exception {
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/missing"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(404, response.statusCode());
	}

	@Test
	public void postExistsButNotGet() throws Exception {
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/capitalise"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(405, response.statusCode());
	}

	@Test
	public void naughtyClient() throws Exception {
		Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String header = """
				POST /capitalise HTTP/1.1\r
				Host: localhost:8080\r
				Content-Type: text/plain\r
				Content-Length: 100\r
				Connection: close\r
				\r
				hello""";

		out.write(header.getBytes(StandardCharsets.UTF_8));

		socket.close();

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/ping"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("pong\n", response.body());
	}

	@Test
	public void badContentLength() throws Exception {
		Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String header = """
				POST /capitalise HTTP/1.1\r
				Host: localhost:8080\r
				Content-Type: text/plain\r
				Content-Length: banana\r
				Connection: close\r
				\r
				hello""";

		out.write(header.getBytes(StandardCharsets.UTF_8));

		socket.close();

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/ping"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("pong\n", response.body());
	}

	@Test
	public void negativeContentLength() throws Exception {
		Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String header = """
				POST /capitalise HTTP/1.1\r
				Host: localhost:8080\r
				Content-Type: text/plain\r
				Content-Length: -1\r
				Connection: close\r
				\r
				hello""";

		out.write(header.getBytes(StandardCharsets.UTF_8));

		socket.close();

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + "/ping"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("pong\n", response.body());
	}

	@Test
	public void spaceQueries() throws Exception {
		String endpoint = "/alotofspaces?first=Sanim%20ahmed&last=zain%20ahmed&date=f%20%20%20e%20%20burary";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + endpoint))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("SANIM AHMEDZAIN AHMEDF   E  BURARY", response.body());
	}

	@Test
	public void emptyValue() throws Exception {
		String endpoint = "/alotofspaces?first=Sanim%20ahmed&last=zain%20ahmed&date=";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + endpoint))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(400, response.statusCode());
	}

	@Test
	public void noEquals() throws Exception {
		String endpoint = "/alotofspaces?first=Sanim%20ahmed&last=zain%20ahmed&date";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + endpoint))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(400, response.statusCode());
	}

	@Test
	public void multipleEquals() throws Exception {
		String endpoint = "/alotofspaces?first=Sanim%20ahmed&last=zain%20ahmed&date=hello=world&yes=2";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(URL + endpoint))
				.GET()
				.build();

		HttpResponse<String> response = client.send(
				req,
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
	}

	@AfterAll
	static void stop() {
		server.stop();
	}
}