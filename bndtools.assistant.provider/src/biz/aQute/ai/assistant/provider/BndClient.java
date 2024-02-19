package biz.aQute.ai.assistant.provider;

import java.net.URI;
import java.util.Map;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import biz.aQute.openai.provider.OpenAIProvider.Client;

public class BndClient implements Client {
	final static JSONCodec codec = new JSONCodec();
	final HttpClient		client;
	static {
		codec.setIgnorenull(true);
	}

	public BndClient(HttpClient client) {
		this.client = client;
	}

	@Override
	public <M, R> R webrequest(String url, String method, Map<String, String> headers,
			M msg, Class<R> replyType) {
		try {
			if (client.isOffline())
				throw new IllegalStateException("the workspace is offline");

			HttpRequest<Object> httpRequest = client.build();
			httpRequest.retries(0);
			if (headers != null) {
				headers.forEach((k, v) -> httpRequest.headers(k, v));
			}

			String payload = "";
			if (msg != null) {
				payload = codec.enc()
					.indent("  ")
					.writeDefaults()
					.put(msg)
					.toString();
				httpRequest.upload(payload);
			}

			switch (method) {
				case "DELETE" -> httpRequest.delete();
				case "POST" -> httpRequest.post();
				case "GET" -> httpRequest.get();
			}
			TaggedData response = httpRequest.asTag()
				.go(new URI(url));

			R body = null;
			if (response.getResponseCode() >= 300) {
				System.err.println("getting " + url + " " + response);
				throw new RuntimeException("Error response: " + body + "\n" + response);
			} else {
				String b = IO.collect(response.getInputStream());
				R result = codec.dec()
					.from(b)
					.get(replyType);
				return result;
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
