package aQute.bnd.jpm.util;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import aQute.bnd.http.HttpClient;
import aQute.jsonrpc.domain.JSON.Request;
import aQute.jsonrpc.domain.JSON.Response;
import aQute.lib.collections.ExtList;
import aQute.lib.converter.Converter;
import aQute.lib.hex.Hex;
import aQute.lib.json.JSONCodec;

@SuppressWarnings("unchecked")
public class JSONRPCProxy implements InvocationHandler {

	public static final String		JSONRPC_2_0	= "jsonrpc/2.0/";
	static AtomicLong				counter		= new AtomicLong(System.currentTimeMillis());
	static JSONCodec				codec		= new JSONCodec();
	static Converter				converter	= new Converter();
	static ThreadLocal<Future< ? >>	lastcall	= new ThreadLocal<Future< ? >>();

	static {
		converter.hook(byte[].class, new Converter.Hook() {

			@Override
			public Object convert(Type dest, Object o) throws Exception {
				if (o instanceof String) {
					return Hex.toByteArray((String) o);
				}
				return null;
			}
		});
	}

	final HttpClient		host;
	final URI				endpoint;

	private JSONRPCProxy(HttpClient host, URI endpoint) {
		this.endpoint = endpoint;
		this.host = host;
	}

	public static <T> T createRPC(Class<T> interf, HttpClient host, URI endpoint) throws Exception {
		return createRPC(interf, host, endpoint, null);
	}

	public static <T> T createRPC(Class<T> interf, HttpClient host, URI endpoint, Executor executor) throws Exception {
		return (T) Proxy.newProxyInstance(interf.getClassLoader(), new Class[] {
			interf
		}, new JSONRPCProxy(host, endpoint));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Request request = new Request();
		request.id = counter.incrementAndGet();
		request.method = method.getName();
		request.params = new ExtList<Object>(args);

		Response response = host.build().upload(request).put().get(Response.class).go(endpoint.toURL());

		if (response == null)
			throw new FileNotFoundException("Not found url endpoint: " + endpoint);

		if (method.getReturnType() == void.class || method.getReturnType() == Void.class || response.result == null)
			return null;

		return converter.convert(method.getGenericReturnType(), response.result);
	}

}
