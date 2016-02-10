package aQute.bnd.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.url.ProxyHandler;
import aQute.bnd.service.url.ProxyHandler.ProxySetup;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

public class HttpClient extends Processor implements Closeable, URLConnector {

	private final List<ProxyHandler>			proxyHandlers			= new ArrayList<>();
	private final List<URLConnectionHandler>	connectionHandlers		= new ArrayList<>();
	private ThreadLocal<PasswordAuthentication>	passwordAuthentication	= new ThreadLocal<>();
	private boolean								inited;
	private static JSONCodec					codec					= new JSONCodec();

	public HttpClient(Processor processor) {
		super(processor);
		use(processor);
	}

	public HttpClient() {
		super(new Processor());
	}

	synchronized void init() {
		if (inited)
			return;

		inited = true;

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return passwordAuthentication.get();
			}
		});

		connectionHandlers.addAll(getParent().getPlugins(URLConnectionHandler.class));
		proxyHandlers.addAll(getParent().getPlugins(ProxyHandler.class));
	}

	public void close() {
		Authenticator.setDefault(null);
	}

	@Override
	public InputStream connect(URL url) throws Exception {
		return build().get(InputStream.class).go(url);
	}

	@Override
	public TaggedData connectTagged(URL url) throws Exception {
		return build().get(TaggedData.class).go(url);
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws Exception {
		return build().get(TaggedData.class).ifNoneMatch(tag).go(url);
	}

	public HttpRequest<Object> build() {
		return new HttpRequest<Object>(this);
	}

	public Object send(final HttpRequest< ? > request) throws Exception {

		final ProxySetup proxy = getProxySetup(request.url);
		final URLConnection con = getProxiedAndConfiguredConnection(request.url, proxy);
		final HttpURLConnection hcon = (HttpURLConnection) (con instanceof HttpURLConnection ? con : null);

		setHeaders(request.headers, con);

		if (request.ifNoneMatch != null) {
			request.headers.put("If-None-Match", request.ifNoneMatch);
		}

		configureHttpConnection(request.verb, hcon);

		return connectWithProxy(proxy, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return doConnect(request.upload, request.download, con, hcon);
			}
		});
	}

	public ProxySetup getProxySetup(URL url) throws Exception {
		init();
		for (ProxyHandler ph : getProxyHandlers()) {
			ProxySetup setup = ph.forURL(url);
			if (setup != null) {
				return setup;
			}
		}
		return null;
	}

	public <T> T connectWithProxy(ProxySetup proxySetup, Callable<T> r) throws Exception {
		if (proxySetup == null)
			return r.call();

		passwordAuthentication.set(proxySetup.authentication);
		try {
			return r.call();
		} finally {
			passwordAuthentication.set(null);
		}
	}

	private URLConnection getProxiedAndConfiguredConnection(URL url, ProxySetup proxy) throws IOException, Exception {
		final URLConnection urlc = proxy != null ? url.openConnection(proxy.proxy) : url.openConnection();

		for (URLConnectionHandler urlh : getURLConnectionHandlers()) {
			if (urlh.matches(url)) {
				urlh.handle(urlc);
			}
		}
		return urlc;
	}

	private synchronized Collection< ? extends URLConnectionHandler> getURLConnectionHandlers() throws Exception {
		if (connectionHandlers.isEmpty()) {
			List<URLConnectionHandler> connectionHandlers = getParent().getPlugins(URLConnectionHandler.class);
			this.connectionHandlers.addAll(connectionHandlers);
		}
		return connectionHandlers;
	}

	private synchronized Collection< ? extends ProxyHandler> getProxyHandlers() throws Exception {
		if (proxyHandlers.isEmpty()) {
			List<ProxyHandler> proxyHandlers = getParent().getPlugins(ProxyHandler.class);
			proxyHandlers.addAll(proxyHandlers);
		}
		return proxyHandlers;
	}

	private InputStream createProgressWrappedStream(InputStream inputStream, String name, int size) {
		ProgressPlugin progressPlugin = getParent().getPlugin(ProgressPlugin.class);
		if (progressPlugin == null)
			return inputStream;

		return new ProgressWrappingStream(inputStream, name, size, progressPlugin);
	}

	private Object doConnect(Object put, Type ref, final URLConnection con, final HttpURLConnection hcon)
			throws IOException, Exception {
		if (put != null) {
			doOutput(put, con);
		}

		con.connect();

		if (hcon != null) {

			int code = hcon.getResponseCode();

			if (code / 100 != 2) {

				if (code == HttpURLConnection.HTTP_NOT_FOUND) {
					return null;
				}

				throw new HttpRequestException(hcon);
			}
		}

		// Do not enclose in resource try! InputStream is potentially used later

		InputStream xin = con.getInputStream();
		InputStream in = handleContentEncoding(hcon, xin);
		in = createProgressWrappedStream(in, con.toString(), con.getContentLength());
		return convert(ref, in, con);
	}

	private Object convert(Type ref, InputStream in, URLConnection con) throws IOException, Exception {
		if (ref instanceof Class) {
			Class< ? > refc = (Class< ? >) ref;
			if (refc == byte[].class) {
				return IO.read(in);
			} else if (InputStream.class.isAssignableFrom((refc))) {
				return in;
			} else if (String.class == refc) {
				return IO.collect(in);
			} else if (TaggedData.class == ref) {
				return new TaggedData(con.getHeaderField("ETag"), in);
			} else if (URLConnection.class.isAssignableFrom(refc))
				return con;
		}
		String s = IO.collect(in);
		return codec.dec().from(s).get(ref);
	}

	private InputStream handleContentEncoding(final HttpURLConnection con, InputStream in) throws IOException {
		if (con == null)
			return in;

		String encoding = con.getHeaderField("Content-Encoding");
		if (encoding != null) {
			if (encoding.equalsIgnoreCase("deflate")) {
				in = new InflaterInputStream(in);
				trace("inflate");
			} else if (encoding.equalsIgnoreCase("gzip")) {
				in = new GZIPInputStream(in);
				trace("gzip");
			}
		}
		return in;
	}

	private void doOutput(Object put, final URLConnection con) throws IOException, Exception {
		con.setDoOutput(true);
		try (OutputStream out = con.getOutputStream();) {
			if (put instanceof InputStream) {
				IO.copy((InputStream) put, out);
			} else if (put instanceof byte[])
				IO.copy((byte[]) put, out);
			else if (put instanceof File)
				IO.copy((File) put, out);
			else {
				codec.enc().to(out).put(put).flush();
			}
		}
	}

	private void configureHttpConnection(String verb, final HttpURLConnection hcon) throws ProtocolException {
		if (hcon != null) {
			hcon.setRequestProperty("Accept-Encoding", "deflate, gzip");
			hcon.setInstanceFollowRedirects(true);
			hcon.setRequestMethod(verb);
		}
	}

	private void setHeaders(Map<String,String> headers, final URLConnection con) {
		if (headers != null) {
			for (Entry<String,String> e : headers.entrySet()) {
				con.setRequestProperty(e.getKey(), e.getValue());
			}
		}
	}

}
