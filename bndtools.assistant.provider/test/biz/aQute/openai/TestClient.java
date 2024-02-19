package biz.aQute.openai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.Map;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;
import biz.aQute.openai.provider.OpenAIProvider.Client;

public class TestClient implements Client {
	final static JSONCodec codec = new JSONCodec();
	static {
		codec.setIgnorenull(true);
	}
	final HttpClient client = HttpClient.newHttpClient();

	@Override
	public <M, R> R webrequest(String url, String method, Map<String, String> headers,
			M msg, Class<R> replyType) {
		try {
			Builder requestBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url));

			if (headers != null) {
				headers.forEach((k, v) -> requestBuilder.header(k, v));
			}

			String payload = "";
			if (msg != null) {
				payload = codec.enc().writeDefaults().put(msg).toString();
				System.out.println(payload);
			}

			switch (method) {
			case "DELETE" -> requestBuilder.DELETE();
			case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload));
			case "GET" -> requestBuilder.GET();
			}

			HttpRequest request = requestBuilder.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String body = response.body();
			if (response.statusCode() >= 300) {
				throw new RuntimeException("Error response: " + body);
			} else {
				return codec.dec().from(body).get(replyType);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
