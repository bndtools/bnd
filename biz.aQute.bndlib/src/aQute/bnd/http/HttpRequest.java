package aQute.bnd.http;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.converter.TypeReference;

@SuppressWarnings("unchecked")
public class HttpRequest<T> {
	String				verb		= "GET";
	Object				upload;
	Type				download;
	Map<String,String>	headers		= new HashMap<>();
	long				timeout		= -1;
	HttpClient			client;
	String				ifNoneMatch;
	long				ifModifiedSince;
	long				ifUnmodifiedSince;
	URL					url;
	int					redirects	= 10;
	String				ifMatch;

	HttpRequest(HttpClient client) {
		this.client = client;
	}

	public <X> HttpRequest<X> get(Class<X> type) {
		this.download = type;
		return (HttpRequest<X>) this;
	}

	public <X> HttpRequest<X> get(TypeReference<X> type) {
		this.download = type.getType();
		return (HttpRequest<X>) this;
	}

	public HttpRequest<Object> get(Type type) {
		this.download = type;
		return (HttpRequest<Object>) this;
	}

	public HttpRequest<T> verb(String verb) {
		this.verb = verb;
		return this;
	}

	public HttpRequest<T> put() {
		this.verb = "PUT";
		return this;
	}

	public HttpRequest<T> head() {
		this.verb = "HEAD";
		return this;
	}

	public HttpRequest<T> get() {
		this.verb = "GET";
		return this;
	}

	public HttpRequest<T> post() {
		this.verb = "POST";
		return this;
	}

	public HttpRequest<T> option() {
		this.verb = "OPTION";
		return this;
	}

	public HttpRequest<T> delete() {
		this.verb = "DELETE";
		return this;
	}

	public HttpRequest<T> upload(Object upload) {
		this.upload = upload;
		return this;
	}

	public HttpRequest<T> headers(Map<String,String> map) {
		headers.putAll(map);
		return this;
	}

	public HttpRequest<T> headers(String key, String value) {
		headers.put(key, value);
		return this;
	}

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
		return (T) client.send(this);
	}

	public HttpRequest<T> age(int n, TimeUnit tu) {
		this.headers.put("Age", "" + tu.toSeconds(n));
		return this;
	}

	public Promise<T> async(final URL url) {
		this.url = url;
		final Deferred<T> deferred = new Deferred<>();
		Executor e = Processor.getExecutor();
		e.execute(new Runnable() {

			@Override
			public void run() {
				try {
					T result = (T) client.send(HttpRequest.this);
					deferred.resolve(result);
				} catch (Exception t) {
					deferred.fail(t);
				}
			}

		});
		return deferred.getPromise();
	}

	@Override
	public String toString() {
		return "HttpRequest [verb=" + verb + ", upload=" + upload + ", download=" + download + ", headers=" + headers
				+ ", timeout=" + timeout + ", client=" + client + ", ifNoneMatch=" + ifNoneMatch + ", ifModifiedSince="
				+ ifModifiedSince + ", ifUnmodifiedSince=" + ifUnmodifiedSince + ", ifMatch=" + ifMatch + ", url=" + url
				+ "]";
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
}
