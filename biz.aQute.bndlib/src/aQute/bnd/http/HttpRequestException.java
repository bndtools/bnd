package aQute.bnd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;

public class HttpRequestException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;
	public final int			responseCode;

	public HttpRequestException(HttpURLConnection conn) throws IOException {
		super(getMessage(conn));
		this.responseCode = conn.getResponseCode();
	}

	public HttpRequestException(TaggedData tag) {
		this(tag, null);
	}

	public HttpRequestException(TaggedData tag, Throwable cause) {
		super(getMessage(tag), cause);
		this.responseCode = tag.getResponseCode();
	}

	private static String getMessage(TaggedData tag) {
		return tag.getUrl() + ":" + tag.getResponseCode() + ":" + tag.getTag();
	}

	private static String getMessage(HttpURLConnection conn) throws IOException {
		StringBuilder message = new StringBuilder().append(conn.getURL())
			.append(':')
			.append(conn.getResponseCode());
		String responseMessage = conn.getResponseMessage();
		if (responseMessage != null) {
			message.append(':')
				.append(responseMessage);
		} else {
			try (InputStream in = conn.getErrorStream()) {
				if (in != null) {
					String error = IO.collect(in);
					message.append(':')
						.append(error);
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return message.toString();
	}

}
