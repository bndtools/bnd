package aQute.bnd.url;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

/**
 * TODO Needs testing Can be used to override default verification of HTTPS.
 */
@aQute.bnd.annotation.plugin.BndPlugin(name = "url.https.verification", parameters = HttpsVerification.Config.class)
public class HttpsVerification extends DefaultURLConnectionHandler {
	static Logger				logger	= LoggerFactory.getLogger(HttpsVerification.class);
	private SSLSocketFactory	factory;
	private boolean				verify	= true;
	private String				certificatesPath;
	private X509Certificate[]	certificateChain;

	interface Config {
		String trusted();
	}

	public HttpsVerification() {}

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
	 * Initialize the SSL Context and factory.
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws FileNotFoundException
	 * @throws InvalidAlgorithmParameterException
	 */
	private synchronized void init() throws NoSuchAlgorithmException, KeyManagementException, FileNotFoundException,
		CertificateException, IOException, InvalidAlgorithmParameterException {
		if (factory == null) {

			TrustManager[] trustManagers = new TrustManager[0];
			trustManagers = new TrustManager[] {
				new LocalTrustManager(verify, createCertificates(certificatesPath))
			};

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, trustManagers, new SecureRandom());
			factory = context.getSocketFactory();
		}
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
			if (!verify) {
				https.setHostnameVerifier((string, session) -> true);
			}
		}
	}

	/**
	 * Set the properties
	 */
	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		super.setProperties(map);
		Config config = Converter.cnv(Config.class, map);
		certificatesPath = config.trusted();
	}

	List<X509Certificate> createCertificates(String paths)
		throws FileNotFoundException, CertificateException, IOException {
		List<X509Certificate> certificates = new ArrayList<>();
		if (paths != null) {
			getCertificates(paths, certificates);
		} else if (certificateChain != null) {
			Collections.addAll(certificates, certificateChain);
		}
		return certificates;
	}

	public static void getCertificates(String paths, List<X509Certificate> certificates)
		throws CertificateException, IOException {
		for (String path : paths.split("\\s*,\\s*")) {
			File file = new File(path); // This is a system specific path!
			if (file.isFile()) {
				try (InputStream inStream = IO.stream(file)) {
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
					certificates.add(cert);
				}
			} else {
				logger.warn("Missing trust certificates file {}", path);
			}
		}
	}

	@Override
	public String toString() {
		return "HttpsVerification [verify=" + verify + ", certificatesPath=" + certificatesPath + "]";
	}

}
