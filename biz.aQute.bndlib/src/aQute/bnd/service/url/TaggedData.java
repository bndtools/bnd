package aQute.bnd.service.url;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.http.HttpRequestException;
import aQute.lib.io.IO;

/**
 * Represents a data stream that has a tag associated with it; the primary
 * use-case is an HTTP response stream with an ETag header.
 *
 * @author Neil Bartlett
 */
public class TaggedData implements Closeable {

	private final URLConnection	con;
	private final int			responseCode;
	private final String		etag;
	private final InputStream	in;
	private final URI			url;
	private final File			file;
	private final String		message;

	@Deprecated
	public TaggedData(String tag, InputStream inputStream, int responseCode, long modified, URI url) {
		throw new RuntimeException();
	}

	@Deprecated
	public TaggedData(String tag, InputStream inputStream, int responseCode) {
		throw new RuntimeException();
	}

	@Deprecated
	public TaggedData(String tag, InputStream inputStream) {
		throw new RuntimeException();
	}

	public TaggedData(URLConnection con, InputStream in) throws Exception {
		this(con, in, null);
	}

	public TaggedData(URLConnection con, InputStream in, File file) throws Exception {
		this.con = con;
		this.responseCode = con instanceof HttpURLConnection ? ((HttpURLConnection) con).getResponseCode()
			: (in != null ? 200 : -1);
		this.in = in == null && con != null && (responseCode / 100 == 2) ? con.getInputStream() : in;
		this.file = file;
		this.etag = con.getHeaderField("ETag");
		this.url = con.getURL()
			.toURI();
		this.message = getMessage(con);
	}

	private String getMessage(URLConnection con) {
		try {
			if (con == null || !(con instanceof HttpURLConnection))
				return null;

			HttpURLConnection h = (HttpURLConnection) con;
			if (h.getResponseCode() / 100 < 4)
				return null;

			StringBuilder sb = new StringBuilder();

			try {
				InputStream in = con.getInputStream();
				if (in != null)
					sb.append(IO.collect(in));
			} catch (Exception e) {
				// ignore
			}

			try {
				InputStream errorStream = h.getErrorStream();
				if (errorStream != null)
					sb.append(IO.collect(errorStream));
			} catch (Exception e) {
				// ignore
			}
			return cleanHtml(sb);
		} catch (Exception e) {
			return null;
		}
	}

	private final static Pattern	HTML_TAGS_P	= Pattern.compile("<!--.*-->|<[^>]+>");
	private final static Pattern	NEWLINES_P	= Pattern.compile("(\\s*\n\r?\\s*)+");
	private final static Pattern	ENTITIES_P	= Pattern.compile("&(#(?<nr>[0-9]+))|(?<name>[a-z]+);",
		Pattern.CASE_INSENSITIVE);

	private String cleanHtml(CharSequence sb) {
		sb = HTML_TAGS_P.matcher(sb)
			.replaceAll("");
		sb = NEWLINES_P.matcher(sb)
			.replaceAll("\n");
		StringBuilder x = new StringBuilder();
		Matcher m = ENTITIES_P.matcher(sb);
		int start = 0;
		for (; m.find(); start = m.end()) {
			x.append(sb, start, m.start());
			if (m.group("nr") != null) {
				char c = (char) Integer.parseInt(m.group("nr"));
				x.append(c);
			} else {
				x.append(entity(m.group("name")));
			}
		}
		return (start == 0) ? sb.toString()
			: x.append(sb, start, sb.length())
				.toString();
	}

	private String entity(String name) {
		switch (name) {
			case "nbsp" :
				return "\u00A0";
			case "lt" :
				return "<";
			case "gt" :
				return "<";
			case "amp" :
				return "&";
			case "cent" :
				return "¢";
			case "pound" :
				return "£";
			case "euro" :
				return "€";
			case "copy" :
				return "©";
			case "reg" :
				return "®";
			case "quot" :
				return "\"";
			case "apos" :
				return "'";
			case "yen" :
				return "¥";
			case "sect" :
				return "§";
			case "not" :
				return "¬";
			case "para" :
				return "¶";
			case "curren" :
				return "¤";
			default :
				return "&" + name + ";";
		}
	}

	public TaggedData(URI url, int responseCode, File file) throws Exception {
		this.file = file;
		this.con = null;
		this.in = null;
		this.etag = "";
		this.responseCode = responseCode;
		this.url = url;
		this.message = null;
	}

	/**
	 * Returns the ETag for the retrieved resource, or {@code null} if the ETag
	 * was not provided by the server.
	 */
	public String getTag() {
		return etag;
	}

	/**
	 * Returns the input stream containing the resource data.
	 *
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		return in;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public long getModified() {
		if (con != null)
			return con.getLastModified();
		return -1;
	}

	public boolean hasPayload() throws IOException {
		return in != null;
	}

	public URI getUrl() {
		return url;
	}

	public URLConnection getConnection() {
		return con;
	}

	@Override
	public String toString() {
		return "TaggedData [tag=" + getTag() + ", code=" + getResponseCode() + ", modified=" + new Date(getModified())
			+ ", url=" + getUrl() + ", state=" + getState() + (message == null ? "" : ", msg=" + message) + "]";
	}

	public boolean isOk() {
		return getResponseCode() / 100 == 2;
	}

	public boolean isNotModified() {
		return responseCode == HttpURLConnection.HTTP_NOT_MODIFIED;
	}

	public void throwIt() {
		throw new HttpRequestException(this);
	}

	public State getState() {
		if (isNotFound())
			return State.NOT_FOUND;
		if (isNotModified())
			return State.UNMODIFIED;
		if (isOk())
			return State.UPDATED;

		return State.OTHER;
	}

	public boolean isNotFound() {
		return responseCode == HttpURLConnection.HTTP_NOT_FOUND;
	}

	public File getFile() {
		return file;
	}

	@Override
	public void close() throws IOException {
		IO.close(getInputStream());
	}
}
