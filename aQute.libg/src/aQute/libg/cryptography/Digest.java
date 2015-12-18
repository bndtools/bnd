package aQute.libg.cryptography;

import java.util.Arrays;

import aQute.lib.hex.Hex;

public abstract class Digest {
	final byte[] digest;

	protected Digest(byte[] checksum, int width) {
		this.digest = checksum;
		if (digest.length != width)
			throw new IllegalArgumentException("Invalid width for digest: " + digest.length + " expected " + width);
	}

	public byte[] digest() {
		return digest;
	}

	public String asHex() {
		return Hex.toHexString(digest());
	}

	@Override
	public String toString() {
		return String.format("%s(d=%s)", getAlgorithm(), Hex.toHexString(digest));
	}

	public abstract String getAlgorithm();

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Digest))
			return false;

		Digest d = (Digest) other;
		return Arrays.equals(d.digest, digest);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(digest);
	}

	public byte[] toByteArray() {
		return digest();
	}
}
