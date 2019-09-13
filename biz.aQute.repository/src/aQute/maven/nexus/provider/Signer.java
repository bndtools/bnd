package aQute.maven.nexus.provider;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.io.IO;
import aQute.libg.command.Command;

public class Signer {
	private static Logger	logger	= LoggerFactory.getLogger(Signer.class);
	private String			passphrase;
	private String			cmd;

	public Signer(String passphrase, String cmd) {
		this.passphrase = passphrase;
		this.cmd = cmd == null ? getDefault() : cmd;
	}

	private String getDefault() {
		if (IO.isWindows()) {
			return "gpg";
		}
		return "gpg";
	}

	public byte[] sign(File f) throws Exception {
		File tmp = Files.createTempFile("sign", ".asc")
			.toFile();
		IO.delete(tmp);
		try {
			int result = sign(f, cmd, passphrase, tmp);
			if (result == 0) {
				return IO.read(tmp);
			} else
				return null;
		} finally {
			IO.delete(tmp);
		}
	}

	/**
	 * Sign the given file with gpg.
	 *
	 * @param f the file to sign
	 * @param cmdName the name of the gpg command
	 * @param passphrase the passphrase to use
	 * @return null if failed, otherwise the bytes of the signature
	 */
	public static int sign(File f, String cmdName, String passphrase, File output) throws Exception {

		Command cmd = new Command();

		cmd.add(cmdName);
		cmd.add("--batch");

		if (passphrase != null) {
			cmd.add("--passphrase-fd");
			cmd.add("0");
		}

		cmd.add("--output");
		cmd.add(output.getAbsolutePath());

		cmd.add("-ab");
		cmd.add(f.getAbsolutePath());

		StringBuffer out = new StringBuffer();
		try {
			if (passphrase != null) {
				return cmd.execute(passphrase, out, System.err);
			} else {
				return cmd.execute("", out, out);
			}
		} finally {
			if (out.length() != 0) {
				logger.warn("Maven gpg signing had a problem: {}", out);
			}
		}
	}
}
