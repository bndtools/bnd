package bnd.mr.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class HttpClient {

	public byte[] fetchBytes(URL url) throws IOException {
		// For From Java 11 we can even you a true client with HTTP/2 support!
		java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()//
				.followRedirects(Redirect.NORMAL)//
				.connectTimeout(Duration.ofSeconds(20)) //
				.build();
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();
			HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
			if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new FileNotFoundException(url.toString());
			}
			return response.body();
		} catch (URISyntaxException e) {
			throw new IOException("invalid: " + url, e);
		} catch (InterruptedException e) {
			throw new InterruptedIOException();
		}
	}
}
