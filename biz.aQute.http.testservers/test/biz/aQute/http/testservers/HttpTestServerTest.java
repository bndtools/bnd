package biz.aQute.http.testservers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import aQute.http.testservers.HttpTestServer;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import junit.framework.TestCase;

public class HttpTestServerTest extends TestCase {
	final static TypeReference<Map<String, Object>> MAP_REF = new TypeReference<Map<String, Object>>() {};

	public void testSimple() throws Exception {
		try (HttpTestServer http = getHttps()) {
			System.out.println(http.getBaseURI());
			System.out.println(Arrays.toString(http.getCertificateChain()));
			assertFalse(0 == http.getAddress()
				.getPort());
		}
	}

	static Pattern DN_P = Pattern.compile("(^|\\s)CN=(?<cn>[^, ]+)", Pattern.CASE_INSENSITIVE);

	public void testCorrectCommonName() throws Exception {
		try (HttpTestServer http = getHttps()) {
			X509Certificate cert = http.getCertificateChain()[0];
			String name = cert.getSubjectDN()
				.getName();
			Matcher m = DN_P.matcher(name);
			assertTrue(m.find());
			assertEquals(m.group("cn"), http.getAddress()
				.getHostName());
		}
	}

	public void testURI() throws Exception {
		try (HttpTestServer http = getHttps();) {
			URI uri = http.getBaseURI();
			assertEquals(http.getAddress()
				.getPort(), uri.getPort());
			assertEquals(http.getAddress()
				.getHostName(), uri.getHost());
			assertEquals("https", uri.getScheme());
		}
	}

	public void testDefault() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL uri = http.getBaseURI()
				.toURL();
			String s = aQute.lib.io.IO.collect(uri.openStream());
			System.out.println(s);
		}
	}

	public void testStatus() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL uri = new URI(http.getBaseURI() + "/status/500").toURL();
			HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
			assertEquals(500, connection.getResponseCode());
		}
	}

	public void testIp() throws Exception {
		try (HttpTestServer http = getHttp();) {
			assertNotNull(get(http, MAP_REF, "/ip").get("origin"));
		}
	}

	public void testUserAgent() throws Exception {
		try (HttpTestServer http = getHttp();) {
			assertNotNull(toCaseInsensitive(get(http, MAP_REF, "/user-agent")).get("user-agent"));
		}
	}

	public void testHeaders() throws Exception {
		try (HttpTestServer http = getHttp();) {
			assertNotNull(toCaseInsensitive(get(http, MAP_REF, "/headers")).get("user-agent"));
		}
	}

	public void testBasicAuth() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL uri = new URL(http.getBaseURI() + "/basic-auth/john/doe");
			HttpURLConnection hrc = (HttpURLConnection) uri.openConnection();
			hrc.setRequestProperty("Authorization",
				"Basic " + aQute.lib.base64.Base64.encodeBase64("john:doe".getBytes("UTF-8")));
			hrc.connect();
			assertEquals(200, hrc.getResponseCode());
		}
	}

	public void testBasicFalseAuth() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL uri = new URL(http.getBaseURI() + "/basic-auth/john/xxxxxxx");
			HttpURLConnection hrc = (HttpURLConnection) uri.openConnection();
			hrc.setRequestProperty("Authorization",
				"Basic " + aQute.lib.base64.Base64.encodeBase64("john:doe".getBytes("UTF-8")));
			hrc.connect();
			assertEquals(401, hrc.getResponseCode());
		}
	}

	public void testGet() throws Exception {
		try (HttpTestServer http = getHttp();) {
			Map<String, ?> map = get(http, MAP_REF, "/get");
			assertNotNull(map.get("headers"));
		}
	}

	public void testEncoding() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL url = new URL(http.getBaseURI() + "/encoding/utf8");
			String s = IO.collect(url.openStream(), "UTF-8");
			assertNotNull(s);
			assertEquals(9995, s.length());

			url = new URL(http.getBaseURI() + "/encoding/utf16");
			s = IO.collect(url.openStream(), "UTF-16");
			assertNotNull(s);
			assertEquals(9995, s.length());

			url = new URL(http.getBaseURI() + "/encoding/ascii");
			s = IO.collect(url.openStream(), "ascii");
			assertNotNull(s);
			assertEquals(9995, s.length());

			url = new URL(http.getBaseURI() + "/encoding/ISO_8859_1");
			s = IO.collect(url.openStream(), "ISO_8859_1");
			assertNotNull(s);
			assertEquals(9995, s.length());
		}
	}

	public void testGzip() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL url = new URL(http.getBaseURI() + "/gzip");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Accept-Encoding", "gzip");
			byte data[] = IO.read(con.getInputStream());

			ByteArrayInputStream bin = new ByteArrayInputStream(data);
			GZIPInputStream gzin = new GZIPInputStream(bin);

			String s = IO.collect(gzin, "UTF-8");
			System.out.println(s);
		}
	}

	public void testDeflate() throws Exception {
		try (HttpTestServer http = getHttp();) {
			URL url = new URL(http.getBaseURI() + "/deflate?abc=def&abc%24=12");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Accept-Encoding", "gzip");
			byte data[] = IO.read(con.getInputStream());

			ByteArrayInputStream bin = new ByteArrayInputStream(data);
			InflaterInputStream gzin = new InflaterInputStream(bin);

			String s = IO.collect(gzin, "UTF-8");
			System.out.println(s);
		}
	}

	public void testPut() throws IOException, Exception {
		try (HttpTestServer http = getHttp();) {
			URL url = new URL(http.getBaseURI() + "/put");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("PUT");
			IO.copy("ABC".getBytes(), con.getOutputStream());
			assertEquals(200, con.getResponseCode());
			String s = IO.collect(con.getInputStream());

			assertEquals("ABC", s);
		}
	}

	public void testPost() throws IOException, Exception {
		try (HttpTestServer http = getHttp();) {
			URL url = new URL(http.getBaseURI() + "/put");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			IO.copy("ABC".getBytes(), con.getOutputStream());
			assertEquals(200, con.getResponseCode());
			String s = IO.collect(con.getInputStream());

			assertEquals("ABC", s);
		}
	}

	private SortedMap<String, Object> toCaseInsensitive(Map<String, Object> map) {
		TreeMap<String, Object> tm = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		tm.putAll(map);
		return tm;
	}

	private <T> T get(HttpTestServer http, TypeReference<T> tref, String path) throws Exception {
		URL uri = new URI(http.getBaseURI() + path).toURL();
		HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
		String collect = IO.collect(connection.getInputStream());
		return new JSONCodec().dec()
			.from(collect)
			.get(tref);
	}

	public HttpTestServer getHttps() throws Exception {
		HttpTestServer.Config config = new Config();
		config.https = true;
		HttpTestServer http = new HttpTestServer(config);
		http.start();
		return http;
	}

	public HttpTestServer getHttp() throws Exception {
		HttpTestServer.Config config = new Config();
		config.https = false;
		HttpTestServer http = new Httpbin(config);
		http.start();
		return http;
	}
}
