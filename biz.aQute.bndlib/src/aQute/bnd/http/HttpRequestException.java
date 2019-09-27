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
		super(conn.getURL() + ":" + conn.getResponseCode() + ":" + conn.getResponseMessage() == null ? getMessage(conn)
			: conn.getResponseMessage());
		this.responseCode = conn.getResponseCode();
	}

	public HttpRequestException(TaggedData tag) {
		this(tag, null);
	}

	public HttpRequestException(TaggedData tag, Throwable cause) {
		super(tag.getUrl() + ":" + tag.getResponseCode() + ":" + tag.getTag(), cause);
		this.responseCode = tag.getResponseCode();
	}

	private static String getMessage(HttpURLConnection conn) {
		try (InputStream in = conn.getErrorStream()) {
			if (in != null)
				return IO.collect(in);
		} catch (Exception e) {
			// Ignore
		}
		return "";
	}

}
