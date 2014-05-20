package aQute.bnd.url;

import java.net.*;
import java.security.*;
import java.security.spec.*;
import java.text.*;
import java.util.*;

import javax.net.ssl.*;

import aQute.bnd.build.*;
import aQute.lib.base64.*;
import aQute.lib.hex.*;
import aQute.lib.settings.*;

/**
 * bnd has a builtin delegated authentication mechanism, see {@link Settings}.
 * This URL Connection Handler plugin will use this information to add signing
 * information to the URL.
 * <p>
 * We add a {@link #X_A_QUTE_AUTHORIZATION} header with a formatted string that
 * contains the email of the user, the machine name (for documentation), the
 * public key, and a signed date header (SHA1WithRSA). This information can be
 * parameterized with the following plugin properties or the default settings
 * can be used.
 * <ul>
 * <li>{@link URLConnectionHandler#MATCH} — URL matcher
 * <li>email — Email address of the account holder
 * <li>privateKey — Hex private RSA key
 * <li>publicKey — Hex public RSA key
 * <li>machine — Machine name (default the internet name of this machine
 * </ul>
 * T
 */
public class BndAuthentication extends DefaultURLConnectionHandler {
	private static final String		MACHINE					= "machine";
	private static final String		PRIVATE_KEY				= "privateKey";
	private static final String		PUBLIC_KEY				= "publicKey";
	private static final String		EMAIL					= "email";
	private static SimpleDateFormat httpFormat				= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static final String		X_A_QUTE_AUTHORIZATION	= "X-aQute-Authorization";
	private String					identity;
	private String					email;
	private String					machine;
	private PrivateKey				privateKey;
	private PublicKey				publicKey;

	public void handle(URLConnection connection) throws Exception {

		// TODO switch to https since this signing is only secure with https
		// since we only sign the date.

		if (!(connection instanceof HttpURLConnection) || !matches(connection))
			return;

		if (!(connection instanceof HttpsURLConnection))
			trace("bnd authentication should only be used with https: %s", connection.getURL());

		init();
		// Build up Authorization header
		StringBuilder sb = new StringBuilder(identity);

		// Get the date header, set it if not set

		String dateHeader = connection.getRequestProperty("Date");
		if (dateHeader == null) {
			synchronized (httpFormat) {
				dateHeader = httpFormat.format(new Date());
			}
			connection.setRequestProperty("Date", dateHeader);
		}

		// Ok, calculate the signature

		Signature hmac = Signature.getInstance("SHA1withRSA");
		hmac.initSign(privateKey);
		hmac.update(dateHeader.getBytes()); // never non-ascii

		// Finish the header
		sb.append(Base64.encodeBase64(hmac.sign()));

		connection.setRequestProperty(X_A_QUTE_AUTHORIZATION, sb.toString());
	}

	private synchronized void init() throws UnknownHostException {
		if (identity != null)
			return;

		machine = InetAddress.getLocalHost().getHostName();

		StringBuilder sb = new StringBuilder();

		sb.append(email).append("!");

		if (machine != null)
			sb.append(machine);

		sb.append("!").append(Base64.encodeBase64(publicKey.getEncoded())).append(":");

		this.identity = sb.toString();
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		super.setProperties(map);
		String email = map.get(EMAIL);
		if (email == null) {
			Workspace ws = registry.getPlugin(Workspace.class);
			Settings settings = registry.getPlugin(Settings.class);
			email = settings.getEmail();
			if (email == null) {
				error("The bnd authentication URL connection handler has no email set as property, nor have the bnd settings been set");
				return;
			}
			credentials(email, settings.getPublicKey(), settings.getPrivateKey());
		} else {
			String pub = map.get(PUBLIC_KEY);
			String prv = map.get(PRIVATE_KEY);
			if (pub == null || !Hex.isHex(pub)) {
				error("The bnd authentication URL public key for email %s is not a hex string %s", email, pub);
				return;
			}
			if (prv == null || !Hex.isHex(prv)) {
				error("The bnd authentication URL private key for email %s is not a hex string", email);
				return;
			}
			credentials(email, Hex.toByteArray(pub), Hex.toByteArray(prv));
		}
		machine = map.get(MACHINE);
	}

	private void credentials(String email, byte[] publicKey, byte[] privateKey) throws InvalidKeySpecException,
			NoSuchAlgorithmException {
		this.email = email;
		if (publicKey != null && privateKey != null) {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			this.privateKey = keyFactory.generatePrivate(privateKeySpec);
			this.publicKey = keyFactory.generatePublic(publicKeySpec);
		}
	}
}
