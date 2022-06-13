package aQute.bnd.http;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

import java.io.Closeable;
import java.io.File;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import aQute.bnd.util.home.Home;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * A simple Http Client that inter-works with the bnd registry. It provides an
 * easy way to construct a URL request. The request is then decorated with third
 * parties that are in the bnd registry for proxies and authentication models.
 */
public class HttpClient implements Closeable, URLConnector {
	private final static Logger						logger				= LoggerFactory.getLogger(HttpClient.class);
	public static final SimpleDateFormat			sdf					= new SimpleDateFormat(
		"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

	private static final ThreadLocal<DateFormat>	HTTP_DATE_FORMATTER	= new ThreadLocal<>();

	static {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	// These are not in HttpURLConnection
	private static final int					HTTP_TEMPORARY_REDIRECT		= 307;						// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/307
	private static final int					HTTP_PERMANENT_REDIRECT		= 308;						// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/308

	private final List<ProxyHandler>			proxyHandlers				= new ArrayList<>();
	private final List<URLConnectionHandler>	connectionHandlers			= new ArrayList<>();
	private ThreadLocal<PasswordAuthentication>	passwordAuthentication		= new ThreadLocal<>();
	private boolean								inited;
	private static JSONCodec					codec						= new JSONCodec();
	private URLCache							cache						= new URLCache(
		IO.getFile(Home.getUserHomeBnd() + "/urlcache"));
	private Registry							registry					= null;
	private Reporter							reporter;
	private volatile AtomicBoolean				offline;
	private final PromiseFactory				promiseFactory;
	private ConnectionSettings					connectionSettings;
	final Map<String, Semaphore>				blocker						= new ConcurrentHashMap<>();
	int											maxConcurrentConnections	= 0;

	public HttpClient() {
		promiseFactory = Processor.getPromiseFactory();
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

	}

	private static DateFormat httpDateFormat() {
		DateFormat format = HTTP_DATE_FORMATTER.get();
		if (format == null) {
			format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			HTTP_DATE_FORMATTER.set(format);
		}
		return format;
	}

	@Override
	public void close() {
		Authenticator.setDefault(null);
	}

	@Override
	public InputStream connect(URL url) throws Exception {
		return build().get(InputStream.class)
			.go(url);
	}

	@Override
	public TaggedData connectTagged(URL url) throws Exception {
		return build().get(TaggedData.class)
			.go(url);
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws Exception {
		return build().get(TaggedData.class)
			.ifNoneMatch(tag)
			.go(url);
	}

	public HttpRequest<Object> build() {
		return new HttpRequest<>(this);
	}

	public Object send(final HttpRequest<?> request) throws Exception {
		if (isOffline() || request.isCache()) {
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

	Object doCached(final HttpRequest<?> request) throws Exception, IOException {
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
				return convert(request.download, request.useCacheFile == null ? tag.getFile() : request.useCacheFile,
					tag);

		}
	}

	TaggedData doCached0(final HttpRequest<?> request) throws Exception, IOException {
		logger.debug("cached {}", request.url);

		URL url = request.url;

		try (Info info = cache.get(request.useCacheFile, request.url.toURI())) {
			//
			// Do we have a file url?
			//

			if ("file".equalsIgnoreCase(url.getProtocol())) {
				File sourceFile = new File(url.toURI());
				if (!sourceFile.isFile())
					return new TaggedData(url.toURI(), 404, null);

				if (info.file.isFile() && info.file.lastModified() == sourceFile.lastModified()
					&& info.file.length() == sourceFile.length()) {
					return new TaggedData(url.toURI(), 304, info.file);
				}

				info.update(IO.stream(sourceFile), null, sourceFile.lastModified());
				return new TaggedData(url.toURI(), 200, info.file);
			}

			request.useCacheFile = info.file;
			if (info.isPresent()) {

				//
				// We have a file in the cache, check if it is within
				// our accepted stale period
				//

				if (!isOffline() && (request.maxStale < 0
					|| info.jsonFile.lastModified() + request.maxStale < System.currentTimeMillis())) {

					//
					// Ok, expired. So check if there is a newer one on the
					// server
					//

					//
					// Use etag if present, otherwise time of file
					//

					if (info.dto.etag != null)
						request.ifNoneMatch(info.getETag());
					else {
						long time = info.file.lastModified();
						if (time > 0)
							request.ifModifiedSince(time + 1);
					}

					TaggedData in = send0(request);

					if (in.getState() == State.NOT_FOUND) {
						cache.clear(request.url.toURI());

					} else {

						if (in.getState() == State.UPDATED) {

							//
							// update the cache from the input stream
							//

							info.update(in.getInputStream(), in.getTag(), in.getModified());
						} else if (in.getState() == State.UNMODIFIED)
							info.jsonFile.setLastModified(System.currentTimeMillis());
					}
					return in;

				} else {
					return new TaggedData(request.url.toURI(), HTTP_NOT_MODIFIED, info.file);
				}
			} else {

				//
				// No entry in the cache, but we are cached
				//

				request.ifMatch = null;
				request.ifNoneMatch = null;
				request.ifModifiedSince = -1;

				if (isOffline()) {
					return new TaggedData(url.toURI(), 404, request.useCacheFile);
				}

				TaggedData in = send0(request);

				if (in.isOk()) {
					info.update(in.getInputStream(), in.getTag(), in.getModified());
				}
				return in;
			}
		}
	}

	public TaggedData send0(final HttpRequest<?> request) throws Exception {

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
			request.headers.put("If-Modified-Since", httpDateFormat().format(new Date(request.ifModifiedSince)));
		}

		if (request.ifUnmodifiedSince != 0) {
			request.headers.put("If-Unmodified-Since", httpDateFormat().format(new Date(request.ifUnmodifiedSince)));
		}

		setHeaders(request.headers, con);

		configureHttpConnection(request.verb, hcon);

		final ProgressPlugin.Task task = getTask(request);
		try {

			TaggedData td = connectWithProxy(proxy,
				() -> doConnect(request.upload, request.download, con, hcon, request, task));
			logger.debug("result {}", td);
			return td;
		} catch (Throwable t) {
			task.done("Failed " + t, t);
			throw t;
		}
	}

	ProgressPlugin.Task getTask(final HttpRequest<?> request) {
		final String name = (request.upload == null ? "Download " : "Upload ") + request.url;
		final int size = 100;
		final ProgressPlugin.Task task;
		final List<ProgressPlugin> progressPlugins = registry != null ? registry.getPlugins(ProgressPlugin.class)
			: null;

		if (progressPlugins != null && progressPlugins.size() > 1) {
			final List<ProgressPlugin.Task> multiplexedTasks = new ArrayList<>();

			for (ProgressPlugin progressPlugin : progressPlugins) {
				multiplexedTasks.add(progressPlugin.startTask(name, size));
			}

			task = new ProgressPlugin.Task() {
				@Override
				public void worked(int units) {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						task.worked(units);
					}
				}

				@Override
				public void done(String message, Throwable e) {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						task.done(message, e);
					}
				}

				@Override
				public boolean isCanceled() {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						if (task.isCanceled()) {
							return true;
						}
					}
					return false;
				}
			};
		} else if (progressPlugins != null && progressPlugins.size() == 1) {
			task = progressPlugins.get(0)
				.startTask(name, size);
		} else {
			task = new ProgressPlugin.Task() {
				@Override
				public void worked(int units) {
				}

				@Override
				public void done(String message, Throwable e) {
				}

				@Override
				public boolean isCanceled() {
					return Thread.currentThread()
						.isInterrupted();
				}
			};
		}
		return task;
	}

	private String entitytag(String entity) {
		if (entity == null || entity.isEmpty() || "*".equals(entity))
			return entity;

		return entity;
	}

	public ProxySetup getProxySetup(URL url) throws Exception {
		init();
		for (ProxyHandler ph : getProxyHandlers()) {
			ProxySetup setup = ph.forURL(url);
			if (setup != null) {
				logger.debug("Proxy {}", setup);
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
				logger.debug("Decorate {} with handler {}", url, urlh);
				return urlh;
			} else
				logger.debug("No match for {}, handler {}", url, urlh);
		}
		return null;
	}

	private synchronized Collection<? extends URLConnectionHandler> getURLConnectionHandlers() throws Exception {
		if (connectionHandlers.isEmpty() && registry != null) {
			List<URLConnectionHandler> connectionHandlers = registry.getPlugins(URLConnectionHandler.class);
			this.connectionHandlers.addAll(connectionHandlers);
			logger.debug("URL Connection handlers {}", connectionHandlers);
		}
		return connectionHandlers;
	}

	private synchronized Collection<? extends ProxyHandler> getProxyHandlers() throws Exception {
		if (proxyHandlers.isEmpty() && registry != null) {
			List<ProxyHandler> proxyHandlers = registry.getPlugins(ProxyHandler.class);
			this.proxyHandlers.addAll(proxyHandlers);
			logger.debug("Proxy handlers {}", proxyHandlers);
		}
		return proxyHandlers;
	}

	private InputStream createProgressWrappedStream(InputStream inputStream, String name, int size, Task task,
		long timeout) {
		if (registry == null)
			return inputStream;

		return new ProgressWrappingStream(inputStream, name, size, task, timeout);
	}

	private TaggedData doConnect(Object put, Type ref, final URLConnection con, final HttpURLConnection hcon,
		HttpRequest<?> request, ProgressPlugin.Task task) throws IOException, Exception {

		if (put != null) {
			task.worked(1);
			doOutput(put, con, request);
		} else
			logger.debug("{} {}", request.verb, request.url);

		if (request.timeout > 0) {
			con.setConnectTimeout((int) request.timeout * 10);
			con.setReadTimeout((int) (5000 > request.timeout ? request.timeout : 5000));
		} else {
			con.setConnectTimeout(120000);
			con.setReadTimeout(60000);
		}
		try (AutoCloseable semaphore = getConnectionBlocker(hcon)) {

			if (hcon == null) {
				// not http
				try {
					con.connect();
					InputStream in = con.getInputStream();
					return new TaggedData(con, in, request.useCacheFile);
				} catch (FileNotFoundException e) {
					URI uri = con.getURL()
						.toURI();
					task.done("File not found " + uri, e);
					return new TaggedData(uri, 404, request.useCacheFile);
				}
			}

			int code = hcon.getResponseCode();

			if (code == -1)
				System.out.println("WTF?");

			//
			// Though we ask Java to handle the redirects
			// it does not do it for https <-> http :-(
			//
			if (code == HTTP_MOVED_TEMP || code == HTTP_MOVED_PERM || code == HTTP_SEE_OTHER
				|| code == HTTP_TEMPORARY_REDIRECT || code == HTTP_PERMANENT_REDIRECT) {
				if (request.redirects-- > 0) {
					String location = hcon.getHeaderField("Location");
					request.url = new URL(request.url, location);
					task.done("Redirected " + code + " " + location, null);
					return send0(request);
				}
			}

			if (isUpdateInfo(con, request, code)) {
				File file = (File) request.upload;
				String etag = con.getHeaderField("ETag");
				try (Info info = cache.get(file, con.getURL()
					.toURI())) {
					info.update(etag);
				}
			}

			if ((code / 100) != 2) {
				task.done("Finished " + code + " " + con.getURL()
					.toURI(), null);
				return new TaggedData(con, null, request.useCacheFile);
			}

			// Do not enclose in resource try! InputStream is potentially
			// used
			// later

			InputStream xin = con.getInputStream();
			InputStream in = handleContentEncoding(hcon, xin);
			in = createProgressWrappedStream(in, con.toString(), con.getContentLength(), task, request.timeout);
			return new TaggedData(con, in, request.useCacheFile);
		} catch (javax.net.ssl.SSLHandshakeException ste) {
			task.done(Exceptions.causes(ste), null);
			//
			// 526 Invalid SSL Certificate
			// Cloudflare could not validate the SSL/TLS certificate that the
			// origin server presented.
			return new TaggedData(request.url.toURI(), 526, request.useCacheFile);
		} catch (SocketTimeoutException ste) {
			task.done(ste.toString(), null);
			return new TaggedData(request.url.toURI(), HTTP_GATEWAY_TIMEOUT, request.useCacheFile);
		}
	}

	/*
	 * Blocks on maxConcurrentConnections per host:port combination.
	 */
	private AutoCloseable getConnectionBlocker(HttpURLConnection hcon) throws InterruptedException {
		if (maxConcurrentConnections != 0) {
			if (hcon != null) {
				URL url = hcon.getURL();
				if (url != null) {
					String host = url.getHost();
					if (host != null) {
						String key = host + ":" + url.getPort();
						Semaphore s = blocker.computeIfAbsent(key, k -> new Semaphore(maxConcurrentConnections));
						s.acquire();
						return s::release;
					}
				}
			}
		}
		return () -> {
			// not blocked
		};
	}

	boolean isUpdateInfo(final URLConnection con, HttpRequest<?> request, int code) {
		return request.upload instanceof File && request.updateTag && code == HTTP_CREATED
			&& con.getHeaderField("ETag") != null;
	}

	private Object convert(Type type, File in, TaggedData tag) throws IOException, Exception {
		if (type == TaggedData.class)
			return tag;

		if (type == File.class)
			return in;
		try (InputStream fin = IO.stream(in)) {
			return convert(type, fin);
		}
	}

	private Object convert(Type ref, InputStream in) throws IOException, Exception {
		if (ref instanceof Class) {
			Class<?> refc = (Class<?>) ref;
			if (refc == byte[].class) {
				return IO.read(in);
			} else if (InputStream.class.isAssignableFrom((refc))) {
				return in;
			} else if (String.class == refc) {
				return IO.collect(in);
			}
		}
		String s = IO.collect(in);
		return codec.dec()
			.from(s)
			.get(ref);
	}

	private InputStream handleContentEncoding(final HttpURLConnection con, InputStream in) throws IOException {
		if (con == null)
			return in;

		String encoding = con.getHeaderField("Content-Encoding");
		if (encoding != null) {
			if (encoding.equalsIgnoreCase("deflate")) {
				in = new InflaterInputStream(in);
				logger.debug("inflate");
			} else if (encoding.equalsIgnoreCase("gzip")) {
				in = new GZIPInputStream(in);
				logger.debug("gzip");
			}
		}
		return in;
	}

	private void doOutput(Object put, final URLConnection con, HttpRequest<?> rq) throws IOException, Exception {
		con.setDoOutput(true);
		try (OutputStream out = con.getOutputStream()) {
			if (put instanceof InputStream) {
				logger.debug("out {} input stream {}", rq.verb, rq.url);
				IO.copy((InputStream) put, out);
			} else if (put instanceof String) {
				logger.debug("out {} string {}", rq.verb, rq.url);
				IO.store(put, out);
			} else if (put instanceof byte[]) {
				logger.debug("out {} byte[] {}", rq.verb, rq.url);
				IO.copy((byte[]) put, out);
			} else if (put instanceof File) {
				logger.debug("out {} file {} {}", rq.verb, put, rq.url);
				IO.copy((File) put, out);
			} else {
				logger.debug("out {} JSON {} {}", rq.verb, put, rq.url);
				codec.enc()
					.to(out)
					.put(put)
					.flush();
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

	private void setHeaders(Map<String, String> headers, final URLConnection con) {
		if (headers != null) {
			for (Entry<String, String> e : headers.entrySet()) {
				logger.debug("set header {}={}", e.getKey(), e.getValue());
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
		IO.mkdirs(log.getParentFile());
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
		connectionSettings = new ConnectionSettings(processor, this);
		connectionSettings.readSettings();
		try {
			String maxConcurrentConnections = processor.get("-x-max-concurrent-connections");
			if (maxConcurrentConnections == null)
				return;
			this.maxConcurrentConnections = Integer.parseInt(maxConcurrentConnections);
		} catch (Exception e) {
			processor.error("-x-max-concurrent-connections is set to %s but this is not a proper integer",
				maxConcurrentConnections);
		}
	}

	public URI makeDir(URI uri) throws URISyntaxException {
		if (uri.getPath() != null && uri.getPath()
			.endsWith("/")) {
			String string = uri.toString();
			return new URI(string.substring(0, string.length() - 1));
		} else
			return uri;
	}

	public boolean isOffline() {
		AtomicBoolean localOffline = offline;
		if (localOffline == null) {
			return false;
		}
		return localOffline.get();
	}

	public void setOffline(AtomicBoolean offline) {
		this.offline = offline;
	}

	public PromiseFactory promiseFactory() {
		return promiseFactory;
	}

	public URLCache cache() {
		return cache;
	}

	public void reportSettings(Formatter out) {
		if (connectionSettings != null) {
			connectionSettings.report(out);
		}
	}
}
