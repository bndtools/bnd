package aQute.bnd.url;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.X509TrustManager;

class LocalTrustManager implements X509TrustManager {

	final static CertificateFactory	cf;
	final static CertPathValidator	validator;
	final static X509Certificate[]	empty	= new X509Certificate[0];
	final static Base64.Decoder		DECODER	= Base64.getMimeDecoder();

	static {
		try {
			cf = CertificateFactory.getInstance("X.509");
			validator = CertPathValidator.getInstance("PKIX");
		} catch (CertificateException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private Set<TrustAnchor>		anchors;
	private List<X509Certificate>	trusted;
	private PKIXParameters			parameter;
	private boolean					verify;

	public LocalTrustManager(boolean verify, List<X509Certificate> trusted) throws InvalidAlgorithmParameterException {
		this.verify = verify;
		this.trusted = trusted;
		if (!trusted.isEmpty()) {
			this.anchors = trusted.stream()
				.map(x509 -> new TrustAnchor(x509, null))
				.collect(Collectors.toSet());
			this.parameter = new PKIXParameters(anchors);
			this.parameter.setRevocationEnabled(false);
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// ok
	}

	@Override
	public void checkServerTrusted(X509Certificate[] toBeVerified, String authType) throws CertificateException {
		if (verify) {
			if (toBeVerified == null || toBeVerified.length == 0)
				throw new IllegalArgumentException("null or empty chain");

			if (authType == null || authType.isEmpty())
				throw new IllegalArgumentException("no authentication type");

			List<X509Certificate> asList = Arrays.asList(toBeVerified);
			CertPath path = cf.generateCertPath(asList);
			try {
				validator.validate(path, parameter);
			} catch (CertPathValidatorException | InvalidAlgorithmParameterException e) {
				throw new CertificateException(e);
			}
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trusted.toArray(empty);
	}

}
