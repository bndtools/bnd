package aQute.bnd.http;

import java.io.File;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.util.promise.Promise;

import aQute.bnd.service.url.TaggedData;
import aQute.lib.converter.TypeReference;
import aQute.service.reporter.Reporter;

/**
 * Builds up a request
 *
 * @param <T>
 */
public class HttpRequest<T> {
	String				verb			= "GET";
	boolean				isIdemPotent	= true;
	Object				upload;
	Type				download;
	Map<String, String>	headers			= new HashMap<>();
	long				timeout			= -1;
	HttpClient			client;
	String				ifNoneMatch;
	long				ifModifiedSince;
	long				ifUnmodifiedSince;
	URL					url;
	int					redirects		= 10;
	String				ifMatch;
	boolean				cached;
	long				maxStale;
	Reporter			reporter;
	File				useCacheFile;
	boolean				updateTag;
	int					retries;
	long				retryDelay;

	HttpRequest(HttpClient client) {
		this.client = client;
		this.retries = client.retries;
		this.retryDelay = client.retryDelay;
	}

	/**
	 * Convert the result to a specific type
	 */
	@SuppressWarnings("unchecked")
	public <X> HttpRequest<X> get(Class<X> type) {
		this.download = type;
		return (HttpRequest<X>) this;
	}

	/**
	 * Convert the result to a specific type
	 */
	@SuppressWarnings("unchecked")
	public <X> HttpRequest<X> get(TypeReference<X> type) {
		this.download = type.getType();
		return (HttpRequest<X>) this;
	}

	/**
	 * Convert the result to a specific type
	 */
	@SuppressWarnings("unchecked")
	public HttpRequest<Object> get(Type type) {
		this.download = type;
		return (HttpRequest<Object>) this;
	}

	/**
	 * Set the HTTP verb. The methods GET, HEAD, PUT and DELETE are idempotent.
	 * Also, the methods OPTIONS and TRACE SHOULD NOT have side effects, and so
	 * are inherently idempotent.
	 */

	public HttpRequest<T> verb(String verb) {
		this.verb = verb;
		switch (verb.toUpperCase()) {
			case "GET" :
			case "HEAD" :
			case "PUT" :
			case "DELETE" :
			case "OPTIONS" :
			case "TRACE" :
				return idemPotent(true);

			default :
				return idemPotent(false);
		}
	}

	/**
	 * Set the verb/method to put
	 */
	public HttpRequest<T> put() {
		return verb("PUT");
	}

	/**
	 * Set the verb/method to head
	 */
	public HttpRequest<T> head() {
		return verb("HEAD");
	}

	/**
	 * Set the verb/method to get
	 */
	public HttpRequest<T> get() {
		return verb("GET");
	}

	/**
	 * Set the verb/method to post
	 */
	public HttpRequest<T> post() {
		return verb("POST");
	}

	/**
	 * Set the verb/method to option
	 */
	public HttpRequest<T> option() {
		return verb("OPTIONS");
	}

	/**
	 * Set the verb/method to option
	 */
	public HttpRequest<T> trace() {
		return verb("TRACE");
	}

	/**
	 * Set the verb/method to delete
	 */
	public HttpRequest<T> delete() {
		return verb("DELETE");
	}

	/**
	 * Set the object to upload. Can be of several types:
	 * <ul>
	 * <li>InputStream – copied verbatim
	 * <li>String – content sent
	 * <li>byte[] – content sent
	 * <li>File – content sent
	 * <li>Otherwise assumes DTO and encodes in JSON
	 * </ul>
	 */
	public HttpRequest<T> upload(Object upload) {
		this.upload = upload;
		return this;
	}

	/**
	 * Add headers to request
	 */
	public HttpRequest<T> headers(Map<String, String> map) {
		headers.putAll(map);
		return this;
	}

	/**
	 * Add header to request
	 */
	public HttpRequest<T> headers(String key, String value) {
		headers.put(key, value);
		return this;
	}

	/**
	 * Set timeout in ms
	 */
	public HttpRequest<T> timeout(long timeoutInMs) {
		this.timeout = timeoutInMs;
		return this;
	}

	public HttpRequest<T> ifNoneMatch(String etag) {
		this.ifNoneMatch = etag;
		return this;
	}

	public HttpRequest<T> ifModifiedSince(long epochTime) {
		this.ifModifiedSince = epochTime;
		return this;
	}

	public HttpRequest<T> maxRedirects(int n) {
		this.redirects = n;
		return this;
	}

	public T go(URL url) throws Exception {
		this.url = url;
		return client.send(this);
	}

	public T go(URI url) throws Exception {
		return go(url.toURL());
	}

	public HttpRequest<T> age(int n, TimeUnit tu) {
		this.headers.put("Age", "" + tu.toSeconds(n));
		return this;
	}

	public Promise<T> async(URL url) {
		this.url = url;
		return client.sendAsync(this);
	}

	public Promise<T> async(URI uri) {
		try {
			return async(uri.toURL());
		} catch (MalformedURLException e) {
			return client.promiseFactory()
				.failed(e);
		}
	}

	@Override
	public String toString() {
		return "HttpRequest [verb=" + verb + ", upload=" + upload + ", download=" + download + ", headers=" + headers
			+ ", timeout=" + timeout + ", client=" + client + ", url=" + url + "]";
	}

	public HttpRequest<T> ifUnmodifiedSince(long ifNotModifiedSince) {
		this.ifUnmodifiedSince = ifNotModifiedSince;
		return this;
	}

	public HttpRequest<T> ifMatch(String etag) {
		this.ifMatch = etag;
		return this;
	}

	public HttpRequest<TaggedData> asTag() {
		return get(TaggedData.class);
	}

	public HttpRequest<String> asString() {
		return get(String.class);
	}

	public boolean isCache() {
		return ("GET".equalsIgnoreCase(verb) && cached) || download == File.class;
	}

	public HttpRequest<File> useCache(long maxStale) {
		this.maxStale = maxStale;
		this.cached = true;
		return get(File.class);
	}

	public HttpRequest<File> useCache() {
		return useCache(-1);
	}

	public HttpRequest<File> useCache(File file) {
		this.useCacheFile = file;
		return useCache(-1);
	}

	public HttpRequest<File> useCache(File file, long maxStale) {
		this.useCacheFile = file;
		return useCache(maxStale);
	}

	public HttpRequest<T> report(Reporter reporter) {
		this.reporter = reporter;
		return this;
	}

	public HttpRequest<T> timeout(long timeout, TimeUnit unit) {
		this.timeout = unit.toMillis(timeout);
		return this;
	}

	public boolean isTagResult() {
		return download == null || download == TaggedData.class;
	}

	public HttpRequest<T> updateTag() {
		updateTag = true;
		return this;
	}

	/**
	 * Set the number of retries. Retries are only attempted when the method
	 * verb implies idempotency, or it is explicitly set to be idempotent, see
	 * {@link #idemPotent(boolean)}.
	 *
	 * @param retries number of retries, default is 3.
	 * @return this
	 */
	public HttpRequest<T> retries(int retries) {
		this.retries = retries;
		return this;
	}

	public HttpRequest<T> retryDelay(int retryDelay) {
		this.retryDelay = TimeUnit.SECONDS.toMillis(retryDelay);
		return this;
	}

	/**
	 * Idempotent Methods
	 * <p>
	 * Methods can also have the property of "idempotence" in that (aside from
	 * error or expiration issues) the side-effects of N > 0 identical requests
	 * is the same as for a single request. The methods GET, HEAD, PUT and
	 * DELETE share this property. Also, the methods OPTIONS and TRACE SHOULD
	 * NOT have side effects, and so are inherently idempotent.
	 * <p>
	 * The {@link #verb(String)} method will set the idempotency according to
	 * this specification. This method can then override the default
	 * idempotency.
	 *
	 * @param isIdemPotent if the to be used method is idempotent. (Is
	 *            overridden if the method verb is set after this method!)
	 * @return this
	 */
	public HttpRequest<T> idemPotent(boolean isIdemPotent) {
		this.isIdemPotent = isIdemPotent;
		return this;
	}

}
