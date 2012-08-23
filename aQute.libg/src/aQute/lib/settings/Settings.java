package aQute.lib.settings;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

import aQute.lib.io.*;
import aQute.lib.json.*;

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
public class Settings implements Map<String,String> {
	static JSONCodec	codec	= new JSONCodec();

	private File		where;
	private PublicKey	publicKey;
	private PrivateKey	privateKey;
	private boolean		loaded;
	private boolean		dirty;

	public static class Data {
		public int					version	= 1;
		public byte[]				secret;
		public byte[]				id;
		public Map<String,String>	map		= new HashMap<String,String>();
	}

	Data	data	= new Data();

	public Settings() {
		this("~/.bnd/settings.json");
	}

	public Settings(String where) {
		assert where != null;
		this.where = IO.getFile(IO.work, where);
	}

	public boolean load() {
		if (this.where.isFile() && this.where.length() > 1) {
			try {
				data = codec.dec().from(this.where).get(Data.class);
				loaded = true;
				return true;
			}
			catch (Exception e) {
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
		if (!this.where.getParentFile().isDirectory() && !this.where.getParentFile().mkdirs())
			throw new RuntimeException("Cannot create directory in " + this.where.getParent());

		try {
			codec.enc().to(this.where).put(data).flush();
			assert this.where.isFile();
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot write settings file " + this.where, e);
		}
	}

	public void generate() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		keyGen.initialize(1024, random);
		KeyPair pair = keyGen.generateKeyPair();
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();
		data.secret = privateKey.getEncoded();
		data.id = publicKey.getEncoded();
		save();
	}

	public String getEmail() {
		return get("email");
	}

	public void setEmail(String email) {
		put("email", email);
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
		if (publicKey != null)
			return;

		if (data.id == null || data.secret == null) {
			generate();
		} else {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(data.secret);
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(data.id);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(privateKeySpec);
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

	public void clear() {
		data = new Data();
		IO.delete(where);
	}

	public boolean containsKey(Object key) {
		check();
		return data.map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		check();
		return data.map.containsValue(value);
	}

	public Set<java.util.Map.Entry<String,String>> entrySet() {
		check();
		return data.map.entrySet();
	}

	public String get(Object key) {
		check();

		return data.map.get(key);
	}

	public boolean isEmpty() {
		check();
		return data.map.isEmpty();
	}

	public Set<String> keySet() {
		check();
		return data.map.keySet();
	}

	public String put(String key, String value) {
		check();
		dirty = true;
		return data.map.put(key, value);
	}

	public void putAll(Map< ? extends String, ? extends String> v) {
		check();
		dirty = true;
		data.map.putAll(v);
	}

	public String remove(Object key) {
		check();
		dirty = true;
		return data.map.remove(key);
	}

	public int size() {
		check();
		return data.map.size();
	}

	public Collection<String> values() {
		check();
		return data.map.values();
	}

	public boolean isDirty() {
		return dirty;
	}

}
