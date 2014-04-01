package test.make;

import java.io.*;
import java.security.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.lib.io.*;

public class MD5 implements MakePlugin {
	static final int BUFFER_SIZE = IOConstants.PAGE_SIZE * 1;

	@Override
	public Resource make(Builder builder, String source, Map<String,String> arguments) throws Exception {
		if (!arguments.get("type").equals("md5"))
			return null;
		source = source.substring(0, source.length() - 4);
		final File f = builder.getFile(source);
		if (f.isFile()) {
			return new AbstractResource(f.lastModified()) {
				@Override
				public byte[] getBytes() throws Exception {
					return md5(f);
				}
			};
		}
		throw new FileNotFoundException("No such file: " + source);
	}

	static byte[] md5(File f) throws Exception {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		InputStream in = new FileInputStream(f);
		try {
			byte[] b = new byte[BUFFER_SIZE];
			int size;
			while ((size = in.read()) > 0) {
				md5.update(b, 0, size);
			}
			return md5.digest();
		}
		finally {
			in.close();
		}
	}
}
