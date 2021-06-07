package aQute.http.testservers;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

public class HttpTestServer implements AutoCloseable, Closeable {

	private static final JSONCodec	json		= new JSONCodec();
	private final List<Failure>		failures	= new ArrayList<>();
	private Config					config;
	private Server					server;

	public static class Config {
		public boolean				https;
		public int					port;
		public String				host	= "localhost";
		public Map<String, String>	users	= new HashMap<>();
		public int					backlog	= 0;
		public int					keysize	= 1024;
	}

	public static class Request implements Comparable<Request> {
		public final long				time	= System.currentTimeMillis();
		public String					method;
		public URI						uri;
		public TreeMap<String, String>	headers;
		public Map<String, String>		args	= new HashMap<>();
		public byte[]					content;
		public String					ip;

		@Override
		public int compareTo(Request o) {
			return Long.compare(time, o.time);
		}
	}

	public static class Response {
		public Map<String, String>	headers	= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		public byte[]				content;
		public int					code	= 200;
		public String				mimeType;
		public int					length	= -1;
		public InputStream			stream;
	}

	public static class Failure {
		public Request	request;
		public Response	response;
		public String	throwable;
		public String	stackTrace;
	}

	public HttpTestServer(Config config) throws Exception {
		this(config, "localhost");
	}

	public HttpTestServer(Config config, String cn) throws Exception {
		this.config = config == null ? config = new Config() : config;

		if (config.host == null)
			config.host = InetAddress.getLoopbackAddress()
				.getHostAddress();

		server = new Server(config, cn);

	}

	public void start() throws IOException {
		setMethodHandlers();
		server.start(5000, true);
	}

	public InetSocketAddress getAddress() {
		return new InetSocketAddress(server.getHostname(), server.getListeningPort());
	}

	void setMethodHandlers() {
		for (final Method m : getClass().getMethods()) {

			if (m.getName()
				.startsWith("_")) {
				String path = methodToPath(m);
				createContext(m, path);
			}
		}
	}

	public String methodToPath(final Method m) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < m.getName()
			.length(); i++) {
			char c = m.getName()
				.charAt(i);
			switch (c) {
				case '_' :
					sb.append('/');
					break;
				case '$' :
					String hex = m.getName()
						.substring(i + 1, i + 3);
					int charCode = Integer.parseInt(hex, 16);
					sb.append((char) charCode);
					i += 2;
					break;

				default :
					sb.append(c);
					break;
			}
		}
		String path = sb.toString();
		if (path.equals("//"))
			path = "/";
		return path;
	}

	void createContext(final Method m, String path) {
		server.createContext(new HttpHandler() {

			@Override
			public void handle(String path, Request request, Response response) throws IOException {
				try {

					List<Object> args = assign(m, request, response, path);

					Object[] array = args.toArray();
					Object result = m.invoke(HttpTestServer.this, array);

					if (response.content == null) {
						if (result == null) {
							response.content = new byte[0];
							response.length = 0;
						} else if (result instanceof byte[]) {
							response.content = (byte[]) result;
							response.mimeType = "application/octet-stream";
						} else if (result instanceof InputStream) {

							response.stream = (InputStream) result;
							response.mimeType = "application/octet-stream";
						} else if (result instanceof String) {
							String s = (String) result;
							response.content = s.getBytes(StandardCharsets.UTF_8);
							response.mimeType = "text/" + (s.startsWith("<") ? "html" : "plain");
						} else if (result instanceof File) {
							response.content = IO.read((File) result);
						} else {
							response.content = toJSON(result);
							response.mimeType = "application/json";
						}
					}

				} catch (Throwable e) {
					try {
						e.printStackTrace();
						Failure failure = new Failure();
						failure.throwable = e.toString();
						failure.request = request;
						failure.response = response;

						synchronized (failures) {
							failures.add(failure);
							while (failures.size() > 1000)
								failures.remove(0);
						}

					} catch (Exception ee) {
						throw new RuntimeException(ee);
					}
				}
			}

			public List<Object> assign(final Method m, Request request, Response response, String path)
				throws Exception {
				List<Object> args = new ArrayList<>();

				Type[] types = m.getGenericParameterTypes();
				int i = 0;

				if (i < types.length) {
					if (types[i] == Request.class) {
						args.add(request);
						i++;
					}

					if (i < types.length) {

						if (types[i] == Response.class) {
							args.add(response);
							i++;
						}

						String extraArgs[] = path.isEmpty() ? new String[0] : path.split("/");
						int extra = 0;

						while (i < types.length && extra < extraArgs.length) {
							Object converted = Converter.cnv(types[i], extraArgs[extra]);
							args.add(converted);
							extra++;
							i++;
						}
					}
				}
				return args;
			}

		}, path);
	}

	private byte[] toJSON(Object result) throws Exception {
		return json.enc()
			.put(result)
			.toString()
			.getBytes("UTF-8");
	}

	public URI getBaseURI() throws URISyntaxException {
		int port = server.getListeningPort();
		StringBuilder sb = new StringBuilder();
		if (config.https) {
			sb.append("https://")
				.append(config.host);
			if (port != 443)
				sb.append(":")
					.append(port);
		} else {
			sb.append("http://")
				.append(config.host);
			if (port != 80)
				sb.append(":")
					.append(port);
		}
		return new URI(sb.toString());
	}

	public URI getBaseURI(String path) throws URISyntaxException {
		while (path.startsWith("/"))
			path = path.substring(1);

		return new URI(getBaseURI() + "/" + path);
	}

	protected void getResource(Response rsp, String name, String mime) throws IOException {
		if (getResource(getClass(), rsp, name, mime))
			return;

		if (getClass() != HttpTestServer.class && getResource(HttpTestServer.class, rsp, name, mime))
			return;

		throw new FileNotFoundException(name);
	}

	protected boolean getResource(Class<?> clazz, Response rsp, String name, String mime) throws IOException {
		try (InputStream in = clazz.getResourceAsStream("www/" + name)) {

			if (in == null)
				return false;

			rsp.content = aQute.lib.io.IO.read(in);
			rsp.mimeType = mime;
			return true;
		}
	}

	public List<Failure> getFailuresAndClear() {
		synchronized (failures) {
			ArrayList<Failure> tmp = new ArrayList<>(failures);
			failures.clear();
			return tmp;
		}
	}

	@Override
	public void close() throws IOException {
		server.closeAllConnections();
	}

	public List<File> getTrustedCertificateFiles(File dir) throws Exception {
		X509Certificate[] cc = server.getCertificateChain();
		if (cc == null)
			return Collections.emptyList();

		List<File> files = new ArrayList<>();
		for (X509Certificate c : cc) {
			File f = aQute.lib.io.IO.createTempFile(dir, "cert", ".cer");
			aQute.lib.io.IO.copy(c.getEncoded(), f);
			f.deleteOnExit();
			files.add(f);
		}
		return files;
	}

	public X509Certificate[] getCertificateChain() {
		return server.getCertificateChain();
	}

}
