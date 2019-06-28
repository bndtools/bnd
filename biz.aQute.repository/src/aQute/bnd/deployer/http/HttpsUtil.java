package aQute.bnd.deployer.http;

import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsUtil {

	public static final String PROP_DISABLE_SERVER_CERT_VERIFY = "disableServerVerify";

	static void disableServerVerification(URLConnection connection) throws GeneralSecurityException {
		if (!(connection instanceof HttpsURLConnection))
			return;

		HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
			}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustAllCerts, new SecureRandom());

		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		httpsConnection.setSSLSocketFactory(sslSocketFactory);

		HostnameVerifier trustAnyHost = (string, session) -> true;
		httpsConnection.setHostnameVerifier(trustAnyHost);
	}
}
