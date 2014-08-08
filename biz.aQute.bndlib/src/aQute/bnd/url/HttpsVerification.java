package aQute.bnd.url;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import javax.net.ssl.*;

import aQute.lib.io.*;

/**
 * TODO Needs testing Can be used to override default verification of HTTPS. The
 * 'trusted' property on this plugin must contain a list of issuer certificates
 * that are trusted. If none are specified the verification is NOT done.
 */
@aQute.bnd.annotation.plugin.BndPlugin(name="url.https.verification", parameters=HttpsVerification.Config.class)
public class HttpsVerification extends DefaultURLConnectionHandler {
	private SSLSocketFactory			factory;
	private HostnameVerifier			verifier;
	private final List<X509Certificate>	certificates	= new ArrayList<X509Certificate>();

	interface Config {
		String trusted();
	}
	/**
	 * Initialize the SSL Context, factory and verifier.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private synchronized void init() throws NoSuchAlgorithmException, KeyManagementException {
		if (factory == null) {
			final X509Certificate trusted[] = certificates.toArray(new X509Certificate[certificates.size()]);

			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return trusted;
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType)
							throws CertificateException {}

					public void checkClientTrusted(X509Certificate[] certs, String authType)
							throws CertificateException {}
				}
			};
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, trustAllCerts, new SecureRandom());
			factory = context.getSocketFactory();

			//
			// We can already verify the name through the matchers
			// of the URL so we can ignore that for our connections
			//
			verifier = new HostnameVerifier() {
				public boolean verify(String string, SSLSession session) {
					return true;
				}
			};
		}
	}

	/**
	 * Ensure Https verification is disabled or matches given certificates
	 */
	public void handle(URLConnection connection) throws Exception {

		if (connection instanceof HttpsURLConnection && matches(connection)) {
			init();
			if ( certificates.isEmpty())
				trace("Https verification for %s is DISABLED", connection.getURL());
			
			HttpsURLConnection https = (HttpsURLConnection) connection;
			https.setSSLSocketFactory(factory);
			https.setHostnameVerifier(verifier);
		}
	}

	/**
	 * Set the properties
	 */
	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		super.setProperties(map);

		String paths = map.get("trusted");
		if (paths != null) {
			for (String path : paths.split("\\s*,\\s*")) {
				File file = IO.getFile(path);
				if (file.isFile()) {
					InputStream inStream = new FileInputStream(file);
					try {
						CertificateFactory cf = CertificateFactory.getInstance("X.509");
						X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
						certificates.add(cert);
					}
					finally {
						inStream.close();
					}
				}
			}
		}
	}
}
