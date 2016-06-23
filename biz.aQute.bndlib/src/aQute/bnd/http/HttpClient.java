package aQute.bnd.http;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.http.URLCache.Info;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Registry;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;
import aQute.bnd.service.url.ProxyHandler;
import aQute.bnd.service.url.ProxyHandler.ProxySetup;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

/**
 * A simple Http Client that inter-works with the bnd registry. It provides an
 * easy way to construct a URL request. The request is then decorated with third
 * parties that are in the bnd registry for proxies and authentication models.
 */
public class HttpClient implements Closeable, URLConnector {
	public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

	static {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final List<ProxyHandler>			proxyHandlers			= new ArrayList<>();
	private final List<URLConnectionHandler>	connectionHandlers		= new ArrayList<>();
	private ThreadLocal<PasswordAuthentication>	passwordAuthentication	= new ThreadLocal<>();
	private boolean								inited;
	private static JSONCodec					codec					= new JSONCodec();
	private URLCache							cache					= new URLCache(IO.getFile("~/.bnd/urlcache"));
	private Registry							registry				= null;
	private Reporter							reporter				= new Slf4jReporter(HttpClient.class);
	private List<ProgressPlugin>				progressPlugins;

	public HttpClient() {}

	synchronized void init() {
		if (inited)
			return;

		inited = true;
		progressPlugins = registry != null ? registry.getPlugins(ProgressPlugin.class) : null;

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return passwordAuthentication.get();
			}
		});

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
		if (request.isCache()) {
			return doCached(request);
		} else {
			TaggedData in = send0(request);
			if (request.download == TaggedData.class)
				return in;

			switch (in.getState()) {
				case NOT_FOUND :
					return null;
				case OTHER :
					in.throwIt();
					return null;

				case UNMODIFIED :
				case UPDATED :
				default :
					break;

			}
			return convert(request.download, in.getInputStream());
		}
	}

	Object doCached(final HttpRequest< ? > request) throws Exception, IOException {
		TaggedData tag = doCached0(request);
		if (request.download == TaggedData.class)
			return tag;

		if (request.download == State.class)
			return tag.getState();

		switch (tag.getState()) {
			case NOT_FOUND :
				return null;

			case OTHER :
				throw new HttpRequestException(tag);

			case UNMODIFIED :
			case UPDATED :
			default :
				return convert(request.download, request.useCacheFile, tag);

		}
	}

	TaggedData doCached0(final HttpRequest< ? > request) throws Exception, IOException {
		reporter.trace("cached %s", request.url);
		try (Info info = cache.get(request.useCacheFile, request.url.toURI())) {
			request.useCacheFile = info.file;
			if (info.isPresent()) {

				//
				// We have a file in the cache, check if it is within
				// our accepted stale period
				//

				if (info.file.lastModified() + request.maxStale < System.currentTimeMillis()) {

					//
					// Ok, expired. So check if there is a newer one on the
					// server
					//

					request.ifNoneMatch(info.getETag());
					TaggedData in = send0(request);
					if (in.isOk()) {

						//
						// update the cache from the input stream
						//

						info.update(in.getInputStream(), in.getTag(), in.getModified());
					}
					return in;

				} else {
					return new TaggedData(request.url.toURI(), HttpURLConnection.HTTP_NOT_MODIFIED, info.file);
				}
			} else {

				//
				// No entry in the cache, but we are cached
				//

				request.ifMatch = null;
				request.ifNoneMatch = null;
				request.ifModifiedSince = -1;

				TaggedData in = send0(request);

				if (in.isOk()) {
					info.update(in.getInputStream(), in.getTag(), in.getModified());
				}
				return in;
			}
		}
	}

	public TaggedData send0(final HttpRequest< ? > request) throws Exception {

		final ProxySetup proxy = getProxySetup(request.url);
		final URLConnection con = getProxiedAndConfiguredConnection(request.url, proxy);
		final HttpURLConnection hcon = (HttpURLConnection) (con instanceof HttpURLConnection ? con : null);

		if (request.ifNoneMatch != null) {
			request.headers.put("If-None-Match", entitytag(request.ifNoneMatch));
		}

		if (request.ifMatch != null) {
			request.headers.put("If-Match", "\"" + entitytag(request.ifMatch));
		}

		if (request.ifModifiedSince > 0) {
			synchronized (sdf) {
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				String format = sdf.format(new Date(request.ifModifiedSince));
				request.headers.put("If-Modified-Since", format);
			}
		}

		if (request.ifUnmodifiedSince != 0) {
			synchronized (sdf) {
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				String format = sdf.format(new Date(request.ifUnmodifiedSince));
				request.headers.put("If-Unmodified-Since", format);
			}
		}

		setHeaders(request.headers, con);

		configureHttpConnection(request.verb, hcon);

		final List<Task> tasks = getTasks(request);
		try {

			TaggedData td = connectWithProxy(proxy, new Callable<TaggedData>() {
				@Override
				public TaggedData call() throws Exception {
					return doConnect(request.upload, request.download, con, hcon, request, tasks);
				}
			});
			reporter.trace("result %s", td);
			return td;
		} catch (Throwable t) {
			for (ProgressPlugin.Task task : tasks) {
				task.done("Failed " + t.getMessage(), t);
			}
			throw t;
		}
	}

	List<Task> getTasks(final HttpRequest< ? > request) {
		final List<Task> task = new ArrayList<>();
		if (progressPlugins != null) {
			final String name = "Download " + request.url;
			final int size = 100;
			for (ProgressPlugin plugin : progressPlugins) {
				task.add(plugin.startTask(name, size));
			}
		} else {
			Task noop = new Task() {

				@Override
				public void worked(int units) {}

				@Override
				public void done(String message, Throwable e) {}

				@Override
				public boolean isCanceled() {
					return Thread.currentThread().isInterrupted();
				}
			};
			task.add(noop);
		}
		return task;
	}

	private String entitytag(String entity) {
		if (entity == null || entity.isEmpty() || "*".equals(entity))
			return entity;

		return entity;

		// return "\"" + entity + "\"";
	}

	public ProxySetup getProxySetup(URL url) throws Exception {
		init();
		for (ProxyHandler ph : getProxyHandlers()) {
			ProxySetup setup = ph.forURL(url);
			if (setup != null) {
				reporter.trace("Proxy %s", setup);
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

		URLConnectionHandler matching = findMatchingHandler(url);
		if (matching == null)
			return urlc;


		matching.handle(urlc);

		return urlc;
	}

	public URLConnectionHandler findMatchingHandler(URL url) throws Exception {
		for (URLConnectionHandler urlh : getURLConnectionHandlers()) {
			if (urlh.matches(url)) {
				reporter.trace("Decorate %s with handler %s", url, urlh);
				return urlh;
			} else
				reporter.trace("No match for %s, handler %s", url, urlh);
		}
		return null;
	}

	private synchronized Collection< ? extends URLConnectionHandler> getURLConnectionHandlers() throws Exception {
		if (connectionHandlers.isEmpty() && registry != null) {
			List<URLConnectionHandler> connectionHandlers = registry.getPlugins(URLConnectionHandler.class);
			this.connectionHandlers.addAll(connectionHandlers);
			reporter.trace("URL Connection handlers %s", connectionHandlers);
		}
		return connectionHandlers;
	}

	private synchronized Collection< ? extends ProxyHandler> getProxyHandlers() throws Exception {
		if (proxyHandlers.isEmpty() && registry != null) {
			List<ProxyHandler> proxyHandlers = registry.getPlugins(ProxyHandler.class);
			proxyHandlers.addAll(proxyHandlers);
			reporter.trace("Proxy handlers %s", proxyHandlers);
		}
		return proxyHandlers;
	}

	private InputStream createProgressWrappedStream(InputStream inputStream, String name, int size, List<Task> tasks,
			long timeout) {
		if (registry == null)
			return inputStream;

		return new ProgressWrappingStream(inputStream, name, size, tasks, timeout);
	}

	private TaggedData doConnect(Object put, Type ref, final URLConnection con, final HttpURLConnection hcon,
			HttpRequest< ? > request, List<Task> tasks) throws IOException, Exception {

		if (put != null) {
			for (Task task : tasks) {
				task.worked(1);
			}
			doOutput(put, con, request);
		} else
			reporter.trace("%s %s", request.verb, request.url);

		if (request.timeout > 0) {
			con.setConnectTimeout((int) request.timeout * 10);
			con.setReadTimeout((int) (5000 > request.timeout ? request.timeout : 5000));
		} else {
			con.setConnectTimeout(120000);
			con.setReadTimeout(60000);
		}

		try {

			if (hcon == null) {
				// not http
				try {
					con.connect();
					InputStream in = con.getInputStream();
					return new TaggedData(con, in, request.useCacheFile);
				} catch (FileNotFoundException e) {
					for (Task task : tasks) {
						task.done("file not found", e);
					}
					return new TaggedData(con.getURL().toURI(), 404, request.useCacheFile);
				}
			}

			int code = hcon.getResponseCode();

			if (code == -1)
				System.out.println("WTF?");

			//
			// Though we ask Java to handle the redirects
			// it does not do it for https <-> http :-(
			//

			if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
					|| code == HttpURLConnection.HTTP_SEE_OTHER) {

				if (request.redirects-- > 0) {

					String location = hcon.getHeaderField("Location");
					request.url = new URL(location);
					for (Task task : tasks) {
						task.done("redirected", null);
					}
					return send0(request);

				}

			}

			if (isUpdateInfo(con, request, code)) {
				File file = (File) request.upload;
				String etag = con.getHeaderField("ETag");
				try (Info info = cache.get(file, con.getURL().toURI());) {
					info.update(etag);
				}
			}

			if ((code / 100) != 2) {
				for (Task task : tasks) {
					task.done("finished", null);
				}
				return new TaggedData(con, null, request.useCacheFile);
			}

			// Do not enclose in resource try! InputStream is potentially
			// used
			// later

			InputStream xin = con.getInputStream();
			InputStream in = handleContentEncoding(hcon, xin);
			in = createProgressWrappedStream(in, con.toString(), con.getContentLength(), tasks, request.timeout);
			return new TaggedData(con, in, request.useCacheFile);

		} catch (SocketTimeoutException ste) {
			ste.printStackTrace();
			for (Task task : tasks) {
				task.done("timed out", ste);
			}
			return new TaggedData(request.url.toURI(), HttpURLConnection.HTTP_GATEWAY_TIMEOUT, request.useCacheFile);
		}
	}

	boolean isUpdateInfo(final URLConnection con, HttpRequest< ? > request, int code) {
		return request.upload instanceof File && request.updateTag && code == HttpURLConnection.HTTP_CREATED
				&& con.getHeaderField("ETag") != null;
	}

	private Object convert(Type type, File in, TaggedData tag) throws IOException, Exception {
		if (type == TaggedData.class)
			return in;

		if (type == File.class)
			return in;
		try (FileInputStream fin = new FileInputStream(in)) {
			return convert(type, fin);
		}
	}

	private Object convert(Type ref, InputStream in) throws IOException, Exception {
		if (ref instanceof Class) {
			Class< ? > refc = (Class< ? >) ref;
			if (refc == byte[].class) {
				return IO.read(in);
			} else if (InputStream.class.isAssignableFrom((refc))) {
				return in;
			} else if (String.class == refc) {
				return IO.collect(in);
			}
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
				reporter.trace("inflate");
			} else if (encoding.equalsIgnoreCase("gzip")) {
				in = new GZIPInputStream(in);
				reporter.trace("gzip");
			}
		}
		return in;
	}

	private void doOutput(Object put, final URLConnection con, HttpRequest< ? > rq) throws IOException, Exception {
		con.setDoOutput(true);
		try (OutputStream out = con.getOutputStream();) {
			if (put instanceof InputStream) {
				reporter.trace("out %s input stream %s", rq.verb, rq.url);
				IO.copy((InputStream) put, out);
			} else if (put instanceof String) {
				reporter.trace("out %s string %s", rq.verb, rq.url);
				IO.store(put, out);
			} else if (put instanceof byte[]) {
				reporter.trace("out %s byte[] 5s", rq.verb, rq.url);
				IO.copy((byte[]) put, out);
			} else if (put instanceof File) {
				reporter.trace("out %s file %s %s", rq.verb, put, rq.url);
				IO.copy((File) put, out);
			} else {
				reporter.trace("out %s JSON %s %s", rq.verb, put, rq.url);
				codec.enc().to(out).put(put).flush();
			}
		}
	}

	private void configureHttpConnection(String verb, final HttpURLConnection hcon) throws ProtocolException {
		if (hcon != null) {
			hcon.setRequestProperty("Accept-Encoding", "deflate, gzip");
			hcon.setInstanceFollowRedirects(false); // we handle it
			hcon.setRequestMethod(verb);
		}
	}

	private void setHeaders(Map<String,String> headers, final URLConnection con) {
		if (headers != null) {
			for (Entry<String,String> e : headers.entrySet()) {
				reporter.trace("set header %s=%s", e.getKey(), e.getValue());
				con.setRequestProperty(e.getKey(), e.getValue());
			}
		}
	}

	public void setCache(File cache) {
		this.cache = new URLCache(cache);
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public void addURLConnectionHandler(URLConnectionHandler handler) {
		connectionHandlers.add(handler);
	}

	public Reporter getReporter() {
		return reporter;
	}

	public void addProxyHandler(ProxyHandler proxyHandler) {
		proxyHandlers.add(proxyHandler);
	}

	public void setLog(File log) throws IOException {
		log.getParentFile().mkdirs();
		reporter = new ReporterAdapter(IO.writer(log));
	}

	public String getUserFor(String base) throws MalformedURLException, Exception {
		URLConnectionHandler handler = findMatchingHandler(new URL(base));
		if (handler == null)
			return null;

		return handler.toString();
	}

	public String toName(URI url) throws Exception {
		return URLCache.toName(url);
	}

	public File getCacheFileFor(URI url) throws Exception {

		return cache.getCacheFileFor(url);
	}

	public void readSettings(Processor processor) throws IOException, Exception {
		try (ConnectionSettings cs = new ConnectionSettings(processor, this);) {
			cs.readSettings();
		}
	}

	public URI makeDir(URI uri) throws URISyntaxException {
		if (uri.getPath() != null && uri.getPath().endsWith("/")) {
			String string = uri.toString();
			return new URI(string.substring(0, string.length() - 1));
		}
		else
			return uri;
	}
}
