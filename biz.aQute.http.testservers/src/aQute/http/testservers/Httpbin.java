package aQute.http.testservers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import aQute.lib.base64.Base64;

public class Httpbin extends HttpTestServer {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public Httpbin(Config config) throws Exception {
		super(config);
	}

	public Httpbin(Config config, String cn) throws Exception {
		super(config, cn);
	}

	/**
	 * Default page
	 */

	public void __(Response rsp) throws Exception {
		getResource(rsp, "default.html", "text/html");
	}

	/**
	 * Returns the request
	 */

	public Request _get(Request rq) throws Exception {
		return rq;
	}

	public Request _get$2dtag(Request rq, Response rsp, String tag) throws Exception {
		rsp.headers.put("ETag", tag);
		return rq;
	}

	public void _post(Request rq, Response rsp) throws Exception {
		turnAround(rq, rsp);
	}

	public void _patch(Request rq, Response rsp) throws Exception {
		turnAround(rq, rsp);
	}

	public void _delete(Request rq, Response rsp) throws Exception {
		turnAround(rq, rsp);
	}

	public void _put(Request rq, Response rsp) throws Exception {
		turnAround(rq, rsp);
	}

	public void _encoding(Request req, Response rsp, String charsetName) throws IOException {
		getResource(rsp, "utf8.html", "text/html;charset=" + charsetName);
		Charset c1 = Charset.forName(charsetName);

		if (c1.equals(StandardCharsets.UTF_8))
			return;

		String s = new String(rsp.content, StandardCharsets.UTF_8);
		rsp.content = s.getBytes(c1);
	}

	public void _gzip(Request req, Response rsp) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		GZIPOutputStream gzout = new GZIPOutputStream(bout);
		getResource(rsp, "utf8.html", "text/html;charset=utf8");
		gzout.write(rsp.content);
		gzout.close();
		rsp.content = bout.toByteArray();
		rsp.length = rsp.content.length;
		rsp.headers.put("Content-Encoding", "gzip");
	}

	public void _deflate(Request req, Response rsp) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DeflaterOutputStream gzout = new DeflaterOutputStream(bout);
		getResource(rsp, "utf8.html", "text/html;charset=utf8");
		gzout.write(rsp.content);
		gzout.close();
		rsp.content = bout.toByteArray();
		rsp.length = rsp.content.length;
		rsp.headers.put("Content-Encoding", "deflate");
	}

	void turnAround(Request rq, Response rsp) {
		rsp.content = rq.content;
		String type = rq.headers.get("Content-Type");
		if (type != null)
			rsp.headers.put("Content-Type", type);
	}

	/**
	 * Returns status code
	 */

	public void _status(Response rsp, int code) throws Exception {
		rsp.code = code;
	}

	public Map<String, String> _ip(Request rq) {
		return Collections.singletonMap("origin", rq.ip);
	}

	public Map<String, String> _headers(Request rq) {
		return rq.headers;
	}

	public Map<String, String> _user$2dagent(Request rq) {
		return Collections.singletonMap("user-agent", rq.headers.get("user-agent"));
	}

	static Pattern	BASIC_AUTH_P	= Pattern.compile("Basic\\s+(?<auth>[^\\s]+)\\s*", Pattern.CASE_INSENSITIVE);
	static Pattern	BEARER_AUTH_P	= Pattern.compile("Bearer\\s+(?<auth>[^\\s]+)\\s*", Pattern.CASE_INSENSITIVE);

	public Request _basic$2dauth(Request req, Response response, String user, String passwrd)
		throws UnsupportedEncodingException {
		String auth = Base64.encodeBase64((user + ":" + passwrd).getBytes("UTF-8"));

		String authorization = req.headers.get("Authorization");
		if (authorization != null) {
			Matcher m = BASIC_AUTH_P.matcher(authorization);
			if (m.matches()) {
				if (auth.equals(m.group("auth"))) {
					return req;
				}
			}
		}
		response.code = 401;
		response.headers.put("WWW-Authenticate", "Basic realm=\"Test\"");
		return null;
	}

	public Request _bearer$2dauth(Request req, Response response, String token) throws UnsupportedEncodingException {

		String authorization = req.headers.get("Authorization");
		if (authorization != null) {
			Matcher m = BEARER_AUTH_P.matcher(authorization);
			if (m.matches()) {
				if (token.equals(m.group("auth"))) {
					return req;
				}
			}
		}
		response.code = 401;
		response.headers.put("WWW-Authenticate", "Bearer realm=\"Test\"");
		return null;
	}

	public void _index(Request rq, Response rsp) throws IOException {
		getResource(rsp, "index.xml", "text/xml");
	}

	public void _index$2dauth(Request rq, Response rsp, String user, String password) throws IOException {
		_basic$2dauth(rq, rsp, user, password);
		if (rsp.code == HttpURLConnection.HTTP_UNAUTHORIZED)
			return;

		getResource(rsp, "index.xml", "text/xml");
	}

	/**
	 * Redirect count times and return response code response
	 */
	public void _redirect(Request rq, Response rsp, int count, int response) throws URISyntaxException {
		if (count > 0) {
			System.out.println("redirect " + count);
			String location = (!rq.args.containsKey("relative") ? getBaseURI() : "") + "/redirect/" + (--count) + "/"
				+ response;
			rsp.headers.put("Location", location);
			rsp.code = 301;
		} else {
			rsp.code = response;
		}
	}

	/**
	 * Redirect to https
	 */
	public void _xlocation(Request rq, Response rsp) throws URISyntaxException {
		String to = rq.headers.get("XLocation");
		rsp.headers.put("Location", to);
		rsp.code = 301;
	}

	public Request _etag(Request rq, Response rsp, String etag, long resourceModifiedTime) throws Exception {
		String requestedTag = rq.headers.get("If-None-Match");
		String requestedDate = rq.headers.get("If-Modified-Since");

		String qetag = etag;
		if (!etag.isEmpty())
			rsp.headers.put("ETag", qetag);

		if (requestedDate != null) {
			long modifiedSince = sdf.parse(requestedDate)
				.getTime();
			if (modifiedSince >= resourceModifiedTime) {
				rsp.code = HttpURLConnection.HTTP_NOT_MODIFIED;
				return null;
			}
		}

		if (requestedTag != null) {
			if (requestedTag.equals("*") || requestedTag.equals(qetag)) {
				rsp.code = HttpURLConnection.HTTP_NOT_MODIFIED;
				return null;
			}
		}

		rsp.content = etag != null ? etag.getBytes(StandardCharsets.UTF_8) : new byte[0];
		return null;
	}

	public InputStream _timeout(Request rq, Response rsp, final long timeout) throws InterruptedException {
		rsp.length = 20100;
		return new InputStream() {
			int counter = 20100;

			@Override
			public int read() throws IOException {
				try {
					if (counter-- > 100)
						return ' ';
					else {
						System.out.println("is " + counter);
						Thread.sleep(timeout);
						if (counter >= 0)
							return ' ';
						return -1;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return ' ';
			}
		};
	}
}
