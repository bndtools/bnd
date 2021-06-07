package aQute.http.testservers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import aQute.http.testservers.HttpTestServer.Config;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

class Server extends NanoHTTPD {
	private static final char[]		PASSWORD	= "123456789".toCharArray();
	private X509Certificate[]		certificateChain;
	private Config					config;
	final static SecureRandom		random		= new SecureRandom();
	final Map<String, HttpContext>	contexts	= new HashMap<>();

	public Server(HttpTestServer.Config config) throws Exception {
		this(config, "localhost");
	}

	public Server(HttpTestServer.Config config, String cn) throws Exception {
		super(config.host, config.port);

		this.config = config;
		if (config.https) {

			KeyPair pair = createKey();
			certificateChain = createSelfSignedCertifcate(pair, cn);
			KeyStore keystore = createKeystore(pair);
			SSLContext ctx = createTLSContext(keystore);
			makeSecure(ctx.getServerSocketFactory(), null);
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		try {
			final aQute.http.testservers.HttpTestServer.Response response = new HttpTestServer.Response();
			final aQute.http.testservers.HttpTestServer.Request request = new HttpTestServer.Request();

			Map<String, String> headers = session.getHeaders();
			if (headers.containsKey("content-length")) {
				int length = Integer.parseInt(headers.get("content-length"));
				request.content = new byte[length];
				DataInputStream din = new DataInputStream(session.getInputStream());
				din.readFully(request.content);
			}

			HttpContext context = findHandler(session.getUri());
			if (context == null) {
				return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain",
					"Method not found for " + session.getUri());

			}
			request.uri = new URI(session.getUri());
			request.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			request.headers.putAll(session.getHeaders());
			request.args = session.getParms();
			request.ip = session.getHeaders()
				.get("remote-addr");
			request.method = session.getMethod()
				.name();

			String path = request.uri.getPath()
				.substring(context.path.length());
			if (path.startsWith("/"))
				path = path.substring(1);

			context.handler.handle(path, request, response);

			if (response.content == null)
				response.content = new byte[0];

			if (response.length < 0)
				response.length = response.content.length;

			IStatus status = NanoHTTPD.Response.Status.OK;

			for (IStatus v : fi.iki.elonen.NanoHTTPD.Response.Status.values()) {
				if (v.getRequestStatus() == response.code) {
					status = v;
					break;
				}
			}

			Response r;
			if (response.stream != null)
				r = newFixedLengthResponse(status, response.mimeType, response.stream, response.length);
			else
				r = newFixedLengthResponse(status, response.mimeType, new ByteArrayInputStream(response.content),
					response.length);

			for (Map.Entry<String, String> entry : response.headers.entrySet()) {
				r.addHeader(entry.getKey(), entry.getValue());
			}

			if (response.mimeType != null)
				r.setMimeType(response.mimeType);

			return r;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.close();

			Response r = newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", sw.toString());
			return r;
		}
	}

	private HttpContext findHandler(String uri) {
		while (uri.length() > 1 && uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);

		while (true) {

			HttpContext context = contexts.get(uri);
			if (context != null) {
				return context;
			}

			if (uri.equals("/"))
				return null;

			int n = uri.lastIndexOf('/');
			if (n == -1)
				return null;

			uri = uri.substring(0, n);
		}
	}

	private KeyStore createKeystore(KeyPair pair)
		throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(null, null);
		keystore.setKeyEntry(config.host, pair.getPrivate(), PASSWORD, certificateChain);
		return keystore;
	}

	private SSLContext createTLSContext(KeyStore keystore)
		throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
		SSLContext ctx = SSLContext.getInstance("TLS");

		KeyManagerFactory keyManagers = KeyManagerFactory.getInstance("SunX509");
		keyManagers.init(keystore, PASSWORD);

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keystore);

		ctx.init(keyManagers.getKeyManagers(), trustManagerFactory.getTrustManagers(), random);
		return ctx;
	}

	private X509Certificate[] createSelfSignedCertifcate(KeyPair keyPair, String cn) throws Exception {
		X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
		nameBuilder.addRDN(BCStyle.CN, cn);

		Date notBefore = new Date();
		Date notAfter = new Date(System.currentTimeMillis() + 24 * 3 * 60 * 60 * 1000);

		BigInteger serialNumber = new BigInteger(128, random);

		X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(nameBuilder.build(), serialNumber,
			notBefore, notAfter, nameBuilder.build(), keyPair.getPublic());
		ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
			.build(keyPair.getPrivate());
		X509Certificate certificate = new JcaX509CertificateConverter()
			.getCertificate(certificateBuilder.build(contentSigner));
		return new X509Certificate[] {
			certificate
		};
	}

	private KeyPair createKey() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(config.keysize, random);
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}

	public void createContext(HttpHandler handler, String path) {
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		if (path.isEmpty())
			path = "/";

		HttpContext context = new HttpContext();
		context.handler = handler;
		context.path = path;

		contexts.put(path, context);
	}

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}

	@Override
	protected boolean useGzipWhenAccepted(Response r) {
		return false;
	}
}
