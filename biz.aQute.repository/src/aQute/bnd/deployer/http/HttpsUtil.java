package aQute.bnd.deployer.http;

import java.net.*;
import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

public class HttpsUtil {

	static final String	PROP_DISABLE_SERVER_CERT_VERIFY	= "disableServerVerify";

	static void disableServerVerification(URLConnection connection) throws GeneralSecurityException {
		if (!(connection instanceof HttpsURLConnection))
			return;

		HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}

				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
			}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustAllCerts, new SecureRandom());

		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		httpsConnection.setSSLSocketFactory(sslSocketFactory);

		HostnameVerifier trustAnyHost = new HostnameVerifier() {
			public boolean verify(String string, SSLSession session) {
				return true;
			}
		};
		httpsConnection.setHostnameVerifier(trustAnyHost);
	}
}