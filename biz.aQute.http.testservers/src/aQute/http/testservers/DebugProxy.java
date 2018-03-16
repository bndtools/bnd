package aQute.http.testservers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class DebugProxy extends NanoHTTPD {

	private URL forward;

	public DebugProxy(int port, URL uri) {
		super(port);
		this.forward = uri;
	}

	@Override
	public synchronized Response serve(IHTTPSession session) {
		try {
			System.out.println(session);
			URL uri = new URL(forward + session.getUri());

			final HttpURLConnection c = (HttpURLConnection) uri.openConnection();

			c.setRequestMethod(session.getMethod()
				.toString());
			System.out.println("-> " + session.getMethod() + " " + session.getUri());

			for (Entry<String, String> e : session.getHeaders()
				.entrySet()) {
				if (e.getKey()
					.equalsIgnoreCase("host")) {
					String host = forward.getHost();
					if (forward.getPort() != forward.getDefaultPort())
						host += ":" + forward.getPort();

					c.setRequestProperty("host", host);
				} else if (!e.getKey()
					.equals("accept-encoding"))
					c.setRequestProperty(e.getKey(), e.getValue());
				System.out.println("-> " + e.getKey() + "=" + e.getValue());
			}

			byte[] data = null;

			Map<String, String> headers = session.getHeaders();
			if (headers.containsKey("content-length")) {
				int length = Integer.parseInt(headers.get("content-length"));
				data = new byte[length];
				DataInputStream din = new DataInputStream(session.getInputStream());
				din.readFully(data);
				c.setDoOutput(true);
				c.getOutputStream()
					.write(data);
				c.getOutputStream()
					.flush();
			}

			c.connect();
			IStatus status = new IStatus() {

				@Override
				public String getDescription() {
					try {
						return c.getResponseMessage();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

				@Override
				public int getRequestStatus() {
					try {
						return c.getResponseCode();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			};

			System.out.println("== " + c.getResponseCode() + " : " + c.getResponseMessage());
			InputStream in = null;
			if (c.getContentLength() > 0) {
				data = new byte[c.getContentLength()];
				DataInputStream din = new DataInputStream(c.getInputStream());
				din.readFully(data);
				String s = new String(data);
				// System.out.println("--------------------\n");
				// System.out.println(s);
				// System.out.println("--------------------\n");
				in = new ByteArrayInputStream(data);
			}

			Response r = new Response(status, c.getContentType(), in, c.getContentLengthLong()) {
				{
					for (Map.Entry<String, List<String>> l : c.getHeaderFields()
						.entrySet()) {
						for (String value : l.getValue()) {
							if (l.getKey() != null)
								addHeader(l.getKey(), value);
							System.out.println("<- " + l.getKey() + "=" + value);
						}
					}
				}
			};

			return r;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void main(String args[]) throws Exception {
		DebugProxy dp = new DebugProxy(9999, new URL("http://localhost:8081"));
		dp.start();

		Thread.sleep(1000000);
	}
}
