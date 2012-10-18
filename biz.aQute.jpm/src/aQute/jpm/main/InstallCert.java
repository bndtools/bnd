package aQute.jpm.main;

/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * http://blogs.sun.com/andreas/resource/InstallCert.java
 * Use:
 * java InstallCert hostname
 * Example:
 *% java InstallCert ecc.fedora.redhat.com
 */

import java.io.*;
import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

/**
 * Class used to add the server's certificate to the KeyStore with your trusted
 * certificates.
 */
public class InstallCert {

	public static void installCert(Reporter reporter, String host, int port, String passphrase, File file, boolean install)
			throws Exception {
		if (file == null) {
			File java = new File(System.getProperty("java.home"));
			file = IO.getFile(java, "lib/security/jssecacerts");
			if (!file.isFile())
				file = IO.getFile(java, "lib/security/cacerts");
			if (!file.isFile())
				throw new IllegalArgumentException(
						"Cannot find certifcate file in $JAVA_HOME/lib/security/(jsse)?cacerts");
		}
		KeyStore ks;
		InputStream in = new FileInputStream(file);
		try {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(in, passphrase.toCharArray());
		}
		finally {
			in.close();
		}

		SSLContext context = SSLContext.getInstance("TLS");
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
		context.init(null, new TrustManager[] {
			tm
		}, null);
		SSLSocketFactory factory = context.getSocketFactory();

		reporter.trace("Opening connection to %s:%s", host, port);
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(10000);
		try {
			reporter.trace("Starting SSL handshake...");
			socket.startHandshake();
			socket.close();
			reporter.trace("No errors, certificate is already trusted");
		}
		catch (SSLException e) {
			reporter.trace("expected exception");
		}

		X509Certificate[] chain = tm.chain;
		if (chain == null) {
			reporter.trace("Could not obtain server certificate chain");
			return;
		}

		reporter.trace("Server sent " + chain.length + " certificate(s):");
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		MessageDigest md5 = MessageDigest.getInstance("MD5");

		for (int i = 0; i < chain.length; i++) {
			X509Certificate cert = chain[i];
			System.out.printf("%3s Subject %s Issuer %s sha1=%s md5=%s\n" ,i, cert.getSubjectDN(), cert.getIssuerDN(), Hex.toHexString(sha1.digest()),
					Hex.toHexString(md5.digest()));
			sha1.update(cert.getEncoded());
			md5.update(cert.getEncoded());
		}
		if (!install)
			return;

		X509Certificate cert = chain[0];
		String alias = host;
		ks.setCertificateEntry(alias, cert);

		OutputStream out = new FileOutputStream(file);
		try {
			ks.store(out, passphrase.toCharArray());
		}
		finally {
			out.close();
		}

		reporter.trace("Added certificate to keystore '%s' using alias '%s'", file, alias);
	}

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager	tm;
		X509Certificate[]		chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

}
