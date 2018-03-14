package aQute.maven.nexus.provider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import aQute.lib.io.IO;
import aQute.libg.command.Command;

public class Signer {

	private byte[]	passphrase;
	private String	cmd;

	public Signer(String passphrase, String cmd) {
		this.passphrase = passphrase.getBytes();
		this.cmd = cmd == null ? getDefault() : cmd;
	}

	private String getDefault() {
		boolean windows = File.separatorChar == '\\';
		if (windows) {
			return "gpg";
		}
		return "gpg";
	}

	public byte[] sign(File f) throws Exception {
		Path tmp = Files.createTempFile("sign", ".asc");
		try {
			Command cmd = new Command();

			cmd.add(this.cmd);
			cmd.add("--batch");
			cmd.add("--passphrase-fd");
			cmd.add("0");
			cmd.add("--output");
			cmd.add(tmp.toAbsolutePath()
				.toString());
			Files.delete(tmp);
			cmd.add("-ab");
			cmd.add(f.getAbsolutePath());
			ByteArrayInputStream bin = new ByteArrayInputStream(this.passphrase);
			StringBuffer out = new StringBuffer();
			int result = cmd.execute(bin, out, System.err);
			if (result != 0)
				return null;

			return IO.read(tmp.toFile());
		} finally {
			IO.delete(tmp.toFile());
		}
	}
}
