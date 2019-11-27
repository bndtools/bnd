package aQute.bnd.http;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.util.Objects.requireNonNull;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.promise.TimeoutException;
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
import aQute.bnd.stream.MapStream;
import aQute.bnd.util.home.Home;
import aQute.lib.date.Dates;
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
	final static Logger								logger				= LoggerFactory.getLogger(HttpClient.class);
	@Deprecated
	public static final SimpleDateFormat			sdf					= new SimpleDateFormat(
		"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

	static final long								INITIAL_TIMEOUT		= TimeUnit.MINUTES.toMillis(3);
	static final long								FINAL_TIMEOUT		= TimeUnit.MINUTES.toMillis(5);
	static final long								MAX_RETRY_DELAY		= TimeUnit.MINUTES.toMillis(10);

	static {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final List<ProxyHandler>			proxyHandlers			= new ArrayList<>();
	private final List<URLConnectionHandler>	connectionHandlers		= new ArrayList<>();
	private ThreadLocal<PasswordAuthentication>	passwordAuthentication	= new ThreadLocal<>();
	private boolean								inited;
	static final JSONCodec						codec					= new JSONCodec();
	private URLCache							cache					= new URLCache(
		IO.getFile(Home.getUserHomeBnd() + "/urlcache"));
	private Registry							registry				= null;
	private Reporter							reporter;
	private volatile AtomicBoolean				offline;
	private final PromiseFactory				promiseFactory;
	private ConnectionSettings					connectionSettings;
	int											retries					= 3;
	long										retryDelay				= 0L;

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

	<T> Promise<T> sendAsync(HttpRequest<T> request) {
		int retries = "GET".equalsIgnoreCase(request.verb) ? request.retries : 0;
		long delay = (request.retryDelay == 0L) ? 1000L : request.retryDelay;
		return sendAsync(request, retries, delay);
	}

	private <T> Promise<T> sendAsync(HttpRequest<T> request, int retries, long delay) {
		HttpConnection<T> connection = new HttpConnection<>(request);
		return promiseFactory().submit(connection)
			.timeout(Math.max((retries < 1) ? FINAL_TIMEOUT : INITIAL_TIMEOUT, request.timeout * 10L))
			.recoverWith(failed -> {
				Throwable failure = failed.getFailure();
				Throwable logFailure = null;
				if (failure instanceof TimeoutException) {
					Thread requestThread = connection.requestThread();
					if (requestThread != null) {
						failure.setStackTrace(requestThread.getStackTrace());
					}
					connection.cancel();
					logFailure = failure;
				}
				if (retries < 1) {
					if (failure instanceof RetryException) {
						TaggedData tag = ((RetryException) failure).getTag();
						if (request.download == TaggedData.class) {
							// recover with TaggedData object
							@SuppressWarnings("unchecked")
							Promise<T> recovery = (Promise<T>) promiseFactory().resolved(tag);
							return recovery;
						}
						// replace failure exception
						return promiseFactory().failed(new HttpRequestException(tag, failure.getCause()));
					}
					return null; // no recovery
				}
				String message = failure.getMessage();
				if (message == null) {
					message = failure.toString();
				}
				logger.info("Retrying failed connection. url={}, message={}, delay={}, retries={}", request.url,
					message, delay, retries, logFailure);
				@SuppressWarnings("unchecked")
				Promise<T> delayed = (Promise<T>) failed.delay(delay);
				// double delay for next retry; 10 minutes max delay
				long nextDelay = (request.retryDelay == 0L) ? Math.min(delay * 2L, MAX_RETRY_DELAY) : delay;
				return delayed.recoverWith(f -> sendAsync(request, retries - 1, nextDelay));
			});
	}

	public <T> T send(HttpRequest<T> request) throws Exception {
		Promise<T> promise = sendAsync(request);
		Throwable failure = promise.getFailure(); // wait for completion
		if (failure != null) {
			throw Exceptions.duck(failure);
		}
		return promise.getValue();
	}

	public TaggedData send0(HttpRequest<?> request) throws Exception {
		final Type download = request.download;
		try {
			return send(request.asTag());
		} finally {
			request.download = download;
		}
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

	public URLConnectionHandler findMatchingHandler(URL url) {
		for (URLConnectionHandler urlh : getURLConnectionHandlers()) {
			if (urlh.matches(url)) {
				logger.debug("Decorate {} with handler {}", url, urlh);
				return urlh;
			} else
				logger.debug("No match for {}, handler {}", url, urlh);
		}
		return null;
	}

	private synchronized Collection<? extends URLConnectionHandler> getURLConnectionHandlers() {
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

	InputStream createProgressWrappedStream(InputStream inputStream, String name, int size, Task task, long timeout) {
		if (registry == null) {
			return inputStream;
		}

		return new ProgressWrappingStream(inputStream, name, size, task, timeout);
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
		return cache().getCacheFileFor(url);
	}

	public void readSettings(Processor processor) throws IOException, Exception {
		connectionSettings = new ConnectionSettings(processor, this);
		connectionSettings.readSettings();
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

	public HttpClient retries(int retries) {
		this.retries = retries;
		return this;
	}

	public HttpClient retryDelay(int retryDelay) {
		this.retryDelay = TimeUnit.SECONDS.toMillis(retryDelay);
		return this;
	}

	class HttpConnection<T> implements Callable<T> {
		// These are not in HttpURLConnection
		private static final int		HTTP_TEMPORARY_REDIRECT			= 307;	// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/307
		private static final int		HTTP_PERMANENT_REDIRECT			= 308;	// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/308
		private static final int		HTTP_UNKNOWN_ERROR				= 520;	// https://support.cloudflare.com/hc/en-us/articles/200171936-Error-520-Web-server-is-returning-an-unknown-error
		private static final int		HTTP_INVALID_SSL_CERTIFICATE	= 526;	// https://support.cloudflare.com/hc/en-us/articles/200721975-Error-526-Invalid-SSL-certificate
		private final HttpRequest<T>	request;
		private volatile Thread			requestThread;
		private volatile TaggedData		connected;

		HttpConnection(HttpRequest<T> request) {
			this.request = requireNonNull(request);
			requireNonNull(request.url);
		}

		@Override
		@SuppressWarnings("unchecked")
		public T call() throws Exception {
			final Thread thread = requestThread = Thread.currentThread();
			final String threadName = thread.getName();
			thread.setName(toString());
			try {
				if (isOffline() || request.isCache()) {
					return doCached();
				}
				TaggedData tag = connect();
				if (request.download == TaggedData.class) {
					return (T) tag;
				}

				if (request.download == State.class) {
					return (T) tag.getState();
				}

				switch (tag.getState()) {
					case NOT_FOUND :
						return null;

					case OTHER :
						throw new HttpRequestException(tag);

					case UNMODIFIED :
					case UPDATED :
					default :
						return (T) convert(request.download, tag.getInputStream());
				}
			} finally {
				thread.setName(threadName);
			}
		}

		@Override
		public String toString() {
			return "HttpClient," + request.url;
		}

		Thread requestThread() {
			return requestThread;
		}

		void cancel() {
			TaggedData tag = connected;
			if (tag != null) {
				IO.close(tag);
				File file = tag.getFile();
				if (file != null) {
					IO.delete(file);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private T doCached() throws Exception {
			TaggedData tag = doCached0();
			if (request.download == TaggedData.class) {
				return (T) tag;
			}

			if (request.download == State.class) {
				return (T) tag.getState();
			}

			switch (tag.getState()) {
				case NOT_FOUND :
					return null;

				case OTHER :
					throw new HttpRequestException(tag);

				case UNMODIFIED :
				case UPDATED :
				default :
					return (T) convert(request.download,
						request.useCacheFile == null ? tag.getFile() : request.useCacheFile, tag);
			}
		}

		private TaggedData doCached0() throws Exception {
			final URL url = request.url;
			final URI uri = url.toURI();
			logger.debug("cached {}", url);

			try (Info info = cache().get(request.useCacheFile, uri)) {
				//
				// Do we have a file url?
				//

				if ("file".equalsIgnoreCase(url.getProtocol())) {
					File sourceFile = new File(uri);
					if (!sourceFile.isFile()) {
						return new TaggedData(uri, HTTP_NOT_FOUND, null);
					}

					if (info.file.isFile() && info.file.lastModified() == sourceFile.lastModified()
						&& info.file.length() == sourceFile.length()) {
						return new TaggedData(uri, HTTP_NOT_MODIFIED, info.file);
					}

					info.update(IO.stream(sourceFile), null, sourceFile.lastModified());
					return new TaggedData(uri, HTTP_OK, info.file);
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

						if (info.dto.etag != null) {
							request.ifNoneMatch(info.getETag());
						} else {
							long time = info.file.lastModified();
							if (time > 0) {
								request.ifModifiedSince(time + 1);
							}
						}

						TaggedData tag = connect();

						if (tag.getState() == State.NOT_FOUND) {
							cache().clear(uri);
						} else if (tag.getState() == State.UPDATED) {
							//
							// update the cache from the input stream
							//

							info.update(tag.getInputStream(), tag.getTag(), tag.getModified());
						} else if (tag.getState() == State.UNMODIFIED) {
							info.jsonFile.setLastModified(System.currentTimeMillis());
						}

						return tag;
					}
					return new TaggedData(uri, HTTP_NOT_MODIFIED, info.file);
				}
				//
				// No entry in the cache, but we are cached
				//

				request.ifMatch = null;
				request.ifNoneMatch = null;
				request.ifModifiedSince = -1;

				if (isOffline()) {
					return new TaggedData(uri, HTTP_NOT_FOUND, request.useCacheFile);
				}

				TaggedData tag = connect();

				if (tag.isOk()) {
					info.update(tag.getInputStream(), tag.getTag(), tag.getModified());
				}
				return tag;
			}
		}

		private TaggedData connect() throws Exception {
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
				request.headers.put("If-Modified-Since",
					Dates.formatMillis(Dates.RFC_7231_DATE_TIME, request.ifModifiedSince));
			}

			if (request.ifUnmodifiedSince != 0) {
				request.headers.put("If-Unmodified-Since",
					Dates.formatMillis(Dates.RFC_7231_DATE_TIME, request.ifUnmodifiedSince));
			}

			setHeaders(request.headers, con);

			configureHttpConnection(request.verb, hcon);

			TaggedData tag = connectWithProxy(proxy, () -> doConnect(request.upload, request.download, con, hcon));
			logger.debug("result {}", tag);
			return connected = tag;
		}

		private TaggedData doConnect(Object put, Type ref, URLConnection con, HttpURLConnection hcon) throws Exception {
			final ProgressPlugin.Task task = getTask();

			if (put != null) {
				task.worked(1);
				doOutput(put, con);
			} else {
				logger.debug("{} {}", request.verb, request.url);
			}

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
						URI uri = con.getURL()
							.toURI();
						task.done("File not found " + uri, e);
						return new TaggedData(uri, HTTP_NOT_FOUND, request.useCacheFile);
					}
				}

				int code = hcon.getResponseCode();

				// -1 can be returned if no code can be discerned
				// from the response (i.e., the response is not valid HTTP).
				if (code == -1) {
					throw new IOException("Invalid response code (-1) from connection");
				}

				//
				// Though we ask Java to handle the redirects
				// it does not do it for https <-> http :-(
				//
				if (code == HTTP_MOVED_TEMP || code == HTTP_MOVED_PERM || code == HTTP_SEE_OTHER
					|| code == HTTP_TEMPORARY_REDIRECT || code == HTTP_PERMANENT_REDIRECT) {
					if (request.redirects-- > 0) {
						String location = hcon.getHeaderField("Location");
						request.url = new URL(request.url, location);
						requestThread().setName(toString());
						task.done("Redirected " + code + " " + location, null);
						return connect();
					}
				}

				if (isUpdateInfo(code, con)) {
					File file = (File) request.upload;
					String etag = con.getHeaderField("ETag");
					try (Info info = cache().get(file, con.getURL()
						.toURI())) {
						info.update(etag);
					}
				}

				if ((code / 100) != 2) {
					String message = "Finished " + code + " " + con.getURL()
						.toURI();
					task.done(message, null);
					TaggedData tag = new TaggedData(con, null, request.useCacheFile);
					if ((code / 100) == 5) {
						throw new RetryException(tag, message);
					}
					return tag;
				}

				// Do not enclose in resource try! InputStream is potentially
				// used
				// later

				InputStream xin = con.getInputStream();
				InputStream in = handleContentEncoding(xin, hcon);
				in = createProgressWrappedStream(in, con.toString(), con.getContentLength(), task, request.timeout);
				return new TaggedData(con, in, request.useCacheFile);
			} catch (javax.net.ssl.SSLHandshakeException e) {
				task.done(Exceptions.causes(e), null);
				// 526 Invalid SSL Certificate
				// Cloudflare could not validate the SSL/TLS certificate that
				// the origin server presented.
				throw new RetryException(
					new TaggedData(request.url.toURI(), HTTP_INVALID_SSL_CERTIFICATE, request.useCacheFile), e);
			} catch (SocketTimeoutException e) {
				task.done(e.toString(), null);
				throw new RetryException(
					new TaggedData(request.url.toURI(), HTTP_GATEWAY_TIMEOUT, request.useCacheFile), e);
			} catch (IOException e) {
				task.done(e.toString(), null);
				// 520 Unknown Error (Cloudflare)
				// A 520 Unknown Error code is used as a “catch-all response” in
				// the event that the origin server yields something unexpected.
				// This could include listing large headers, connection resets
				// or invalid or empty responses.
				throw new RetryException(new TaggedData(request.url.toURI(), HTTP_UNKNOWN_ERROR, request.useCacheFile),
					e);
			} catch (RetryException e) {
				throw e;
			} catch (Throwable t) {
				task.done("Failed " + t, t);
				throw t;
			}
		}

		private void configureHttpConnection(String verb, HttpURLConnection hcon) throws ProtocolException {
			if (hcon != null) {
				hcon.setRequestProperty("Accept-Encoding", "deflate, gzip");
				hcon.setInstanceFollowRedirects(false); // we handle it
				hcon.setRequestMethod(verb);
			}
		}

		private void setHeaders(Map<String, String> headers, URLConnection con) {
			MapStream<String, String> stream = MapStream.ofNullable(headers);
			if (logger.isDebugEnabled()) {
				stream = stream.peek((k, v) -> logger.debug("set header {}={}", k, v));
			}
			stream.forEachOrdered(con::setRequestProperty);
		}

		private Object convert(Type type, File in, TaggedData tag) throws Exception {
			if (type == TaggedData.class) {
				return tag;
			}

			if (type == File.class) {
				return in;
			}
			try (InputStream fin = IO.stream(in)) {
				return convert(type, fin);
			}
		}

		private Object convert(Type ref, InputStream in) throws Exception {
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

		private void doOutput(Object put, URLConnection con) throws Exception {
			con.setDoOutput(true);
			try (OutputStream out = con.getOutputStream()) {
				if (put instanceof InputStream) {
					logger.debug("out {} input stream {}", request.verb, request.url);
					IO.copy((InputStream) put, out);
				} else if (put instanceof String) {
					logger.debug("out {} string {}", request.verb, request.url);
					IO.store(put, out);
				} else if (put instanceof byte[]) {
					logger.debug("out {} byte[] {}", request.verb, request.url);
					IO.copy((byte[]) put, out);
				} else if (put instanceof File) {
					logger.debug("out {} file {} {}", request.verb, put, request.url);
					IO.copy((File) put, out);
				} else {
					logger.debug("out {} JSON {} {}", request.verb, put, request.url);
					codec.enc()
						.to(out)
						.put(put)
						.flush();
				}
			}
		}

		private String entitytag(String entity) {
			if (entity == null || entity.isEmpty() || "*".equals(entity))
				return entity;

			return entity;
		}

		private URLConnection getProxiedAndConfiguredConnection(URL url, ProxySetup proxy) throws Exception {
			final URLConnection urlc = proxy != null ? url.openConnection(proxy.proxy) : url.openConnection();

			URLConnectionHandler matching = findMatchingHandler(url);
			if (matching == null) {
				return urlc;
			}

			matching.handle(urlc);

			return urlc;
		}

		private ProgressPlugin.Task getTask() {
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
					public void worked(int units) {}

					@Override
					public void done(String message, Throwable e) {}

					@Override
					public boolean isCanceled() {
						return Thread.currentThread()
							.isInterrupted();
					}

				};
			}
			return task;
		}

		private InputStream handleContentEncoding(InputStream in, HttpURLConnection hcon) throws IOException {
			if (hcon == null) {
				return in;
			}

			String encoding = hcon.getHeaderField("Content-Encoding");
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

		private boolean isUpdateInfo(int code, URLConnection con) {
			return request.upload instanceof File && request.updateTag && code == HTTP_CREATED
				&& con.getHeaderField("ETag") != null;
		}

	}
}
