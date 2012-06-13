package aQute.libg.cryptography;

import java.util.*;

import aQute.lib.hex.*;

public abstract class Digest {
	final byte[]	digest;

	protected Digest(byte[] checksum, int width) {
		this.digest = checksum;
		if (digest.length != width)
			throw new IllegalArgumentException("Invalid width for digest: " + digest.length + " expected " + width);
	}

	public byte[] digest() {
		return digest;
	}

	@Override
	public String toString() {
		return String.format("%s(d=%s)", getAlgorithm(), Hex.toHexString(digest));
	}

	public abstract String getAlgorithm();

	public boolean equals(Object other) {
		if (!(other instanceof Digest))
			return false;

		Digest d = (Digest) other;
		return Arrays.equals(d.digest, digest);
	}

	public int hashCode() {
		return Arrays.hashCode(digest);
	}

	public byte[] toByteArray() {
		return digest();
	}
}
