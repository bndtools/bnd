package aQute.bnd.http;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Processor;
import aQute.lib.converter.TypeReference;

@SuppressWarnings("unchecked")
public class HttpRequest<T> {
	String				verb	= "GET";
	Object				upload;
	Type				download;
	Map<String,String>	headers	= new HashMap<>();
	long				timeout	= -1;
	HttpClient			client;
	String				ifNoneMatch;
	long				since;
	URL					url;

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
		this.since = epochTime;
		return this;
	}

	public T go(URL url) throws Exception {
		this.url = url;
		return (T) client.send(this);
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
				+ ", timeout=" + timeout + ", client=" + client + ", ifNoneMatch=" + ifNoneMatch + ", since=" + since
				+ ", url=" + url + "]";
	}
}
