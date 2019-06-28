package aQute.lib.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * Maintains persistent settings for bnd (or other apps). The default is
 * ~/.bnd/settings.json). The settings are normal string properties but it
 * specially maintains a public/private key pair and it provides a method to
 * sign a byte array with this pair.
 * <p/>
 * Why not keystore and preferences? Well, keystore is hard to use (you can only
 * store a private key when you have a certificate, but you cannot create a
 * certificate without using com.sun classes) and preferences are not editable.
 */
public class Settings implements Map<String, String> {
	static JSONCodec	codec	= new JSONCodec();
	private final File	where;
	private PublicKey	publicKey;
	private PrivateKey	privateKey;
	private boolean		loaded;
	private boolean		dirty;

	public static class Data {
		public int					version	= 1;
		public byte[]				secret;
		public byte[]				id;
		public Map<String, String>	map		= new HashMap<>();
	}

	Data			data	= new Data();
	private char[]	password;

	public Settings() {
		this("~/.bnd/settings.json");
	}

	public Settings(String where) {
		assert where != null;
		this.where = IO.getFile(IO.work, where);
	}

	public boolean load() {
		return load(password);

	}

	@SuppressWarnings("resource")
	public boolean load(char[] password) {
		this.password = password;
		if (this.where.isFile() && this.where.length() > 1) {
			try {
				InputStream in = IO.stream(this.where);
				try {
					if (password != null) {
						PasswordCryptor cryptor = new PasswordCryptor();
						in = cryptor.decrypt(password, in);
					} else {
						String secret = System.getenv()
							.get("BND_SETTINGS_PASSWORD");
						if (secret != null && secret.length() > 0) {
							PasswordCryptor cryptor = new PasswordCryptor();
							in = cryptor.decrypt(secret.toCharArray(), in);
						}
					}

					data = codec.dec()
						.from(in)
						.get(Data.class);
					loaded = true;
					return true;
				} finally {
					in.close();
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot read settings file " + this.where, e);
			}
		}

		if (!data.map.containsKey("name"))
			data.map.put("name", System.getProperty("user.name"));
		return false;
	}

	private void check() {
		if (loaded)
			return;
		load();
		loaded = true;
	}

	public void save() {
		save(password);
	}

	@SuppressWarnings("resource")
	public void save(char[] password) {
		try {
			IO.mkdirs(this.where.getParentFile());
		} catch (IOException e) {
			throw new RuntimeException("Cannot create directory in " + this.where.getParent(), e);
		}

		try {
			OutputStream out = IO.outputStream(this.where);
			try {
				if (password != null) {
					PasswordCryptor cryptor = new PasswordCryptor();
					out = cryptor.encrypt(password, out);
				} else {
					String secret = System.getenv()
						.get("BND-SETTINGS-PASSWORD");
					if (secret != null) {
						PasswordCryptor cryptor = new PasswordCryptor();
						out = cryptor.encrypt(secret.toCharArray(), out);
					}
				}
				codec.enc()
					.to(out)
					.put(data)
					.flush();
			} finally {
				out.close();
			}
			assert this.where.isFile();
		} catch (Exception e) {
			throw new RuntimeException("Cannot write settings file " + this.where, e);
		}
	}

	public void generate() throws Exception {
		generate(password);
	}

	public void generate(char[] password) throws Exception {
		check();
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		keyGen.initialize(1024, random);
		KeyPair pair = keyGen.generateKeyPair();
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();
		data.secret = privateKey.getEncoded();
		data.id = publicKey.getEncoded();
		save(password);
	}

	public String getEmail() {
		return get("email");
	}

	public void setEmail(String email) {
		put("email", email);
	}

	public void setKeyPair(byte[] id, byte[] secret) throws Exception {
		data.secret = secret;
		data.id = id;
		privateKey = null;
		publicKey = null;
		initKeys();
		save();
	}

	public void setName(String v) {
		put("name", v);
	}

	public String getName() {
		String name = get("name");
		if (name != null)
			return name;
		return System.getProperty("user.name");
	}

	/**
	 * Return an encoded public RSA key. this key can be decoded with an
	 * X509EncodedKeySpec
	 *
	 * @return an encoded public key.
	 * @throws Exception
	 */
	public byte[] getPublicKey() throws Exception {
		initKeys();
		return data.id;
	}

	/**
	 * Return an encoded private RSA key. this key can be decoded with an
	 * PKCS8EncodedKeySpec
	 *
	 * @return an encoded private key.
	 * @throws Exception
	 */
	public byte[] getPrivateKey() throws Exception {
		initKeys();
		return data.secret;
	}

	/*
	 * Initialize the keys.
	 */
	private void initKeys() throws Exception {
		check();
		if (privateKey != null)
			return;

		if (data.id == null || data.secret == null) {
			generate();
		} else {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(data.secret);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(privateKeySpec);

			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(data.id);
			publicKey = keyFactory.generatePublic(publicKeySpec);
		}
	}

	/**
	 * Sign a byte array
	 */
	public byte[] sign(byte[] con) throws Exception {
		initKeys();

		Signature hmac = Signature.getInstance("SHA1withRSA");
		hmac.initSign(privateKey);
		hmac.update(con);
		return hmac.sign();
	}

	/**
	 * Verify a signed byte array
	 */
	public boolean verify(byte[] con) throws Exception {
		initKeys();

		Signature hmac = Signature.getInstance("SHA1withRSA");
		hmac.initVerify(publicKey);
		hmac.update(con);
		return hmac.verify(con);
	}

	@Override
	public void clear() {
		data = new Data();
		IO.delete(where);
	}

	@Override
	public boolean containsKey(Object key) {
		check();
		return data.map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		check();
		return data.map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		check();
		return data.map.entrySet();
	}

	@Override
	public String get(Object key) {
		check();

		return data.map.get(key);
	}

	@Override
	public boolean isEmpty() {
		check();
		return data.map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		check();
		return data.map.keySet();
	}

	@Override
	public String put(String key, String value) {
		check();
		dirty = true;
		return data.map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> v) {
		check();
		dirty = true;
		data.map.putAll(v);
	}

	@Override
	public String remove(Object key) {
		check();
		dirty = true;
		return data.map.remove(key);
	}

	@Override
	public int size() {
		check();
		return data.map.size();
	}

	@Override
	public Collection<String> values() {
		check();
		return data.map.values();
	}

	public boolean isDirty() {
		return dirty;
	}

	@Override
	public String toString() {
		return "Settings[" + where + "]";
	}
}
