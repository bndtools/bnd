package aQute.bnd.url;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

/**
 * TODO Needs testing Can be used to override default verification of HTTPS. The
 * 'trusted' property on this plugin must contain a list of issuer certificates
 * that are trusted. If none are specified the verification is NOT done.
 */
@aQute.bnd.annotation.plugin.BndPlugin(name = "url.https.verification", parameters = HttpsVerification.Config.class)
public class HttpsVerification extends DefaultURLConnectionHandler {
	private SSLSocketFactory	factory;
	private HostnameVerifier	verifier;
	private boolean				verify	= true;
	private String				certificatesPath;
	private X509Certificate[]	certificateChain;

	interface Config {
		String trusted();
	}

	public HttpsVerification() {

	}

	// http://stackoverflow.com/questions/24555890/using-a-custom-truststore-in-java-as-well-as-the-default-one
	public HttpsVerification(String certificates, boolean hostnameVerify, Reporter reporter) {
		certificatesPath = certificates;
		this.verify = hostnameVerify;
		this.setReporter(reporter);
	}

	public HttpsVerification(X509Certificate[] certificateChain, boolean b, Reporter hc) {
		this.certificateChain = certificateChain;
		this.verify = b;
		this.setReporter(hc);
	}

	/**
	 * Initialize the SSL Context, factory and verifier.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws FileNotFoundException
	 */
	private synchronized void init() throws NoSuchAlgorithmException, KeyManagementException, FileNotFoundException,
		CertificateException, IOException {
		if (factory == null) {
			List<X509Certificate> certificates = createCertificates(certificatesPath);
			final X509Certificate trusted[] = certificates.toArray(new X509Certificate[0]);

			TrustManager[] trustAllCerts = new TrustManager[] {
				getTrustManager(trusted)
			};
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, trustAllCerts, new SecureRandom());
			factory = context.getSocketFactory();

			//
			// We can already verify the name through the matchers
			// of the URL so we can ignore that for our connections
			//
			verifier = new HostnameVerifier() {
				@Override
				public boolean verify(String string, SSLSession session) {
					return verify;
				}
			};
		}
	}

	X509TrustManager getTrustManager(final X509Certificate[] trusted) {
		X509TrustManager tm = new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return trusted;
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
		};
		return tm;
	}

	/**
	 * Ensure Https verification is disabled or matches given certificates
	 */
	@Override
	public void handle(URLConnection connection) throws Exception {

		if (connection instanceof HttpsURLConnection && matches(connection)) {
			HttpsURLConnection https = (HttpsURLConnection) connection;
			init();
			https.setSSLSocketFactory(factory);
			https.setHostnameVerifier(verifier);
		}
	}

	/**
	 * Set the properties
	 */
	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		super.setProperties(map);

		certificatesPath = map.get("trusted");
	}

	List<X509Certificate> createCertificates(String paths)
		throws FileNotFoundException, CertificateException, IOException {
		List<X509Certificate> certificates = new ArrayList<>();
		if (paths != null) {
			for (String path : paths.split("\\s*,\\s*")) {
				File file = new File(path); // This is a system specific path!
				if (file.isFile()) {
					try (InputStream inStream = IO.stream(file)) {
						CertificateFactory cf = CertificateFactory.getInstance("X.509");
						X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
						certificates.add(cert);
					}
				}
			}
		} else if (certificateChain != null) {
			Collections.addAll(certificates, certificateChain);
		}
		return certificates;
	}

	@Override
	public String toString() {
		return "HttpsVerification [verify=" + verify + ", certificatesPath=" + certificatesPath + "]";
	}
}
