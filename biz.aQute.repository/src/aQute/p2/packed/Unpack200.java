package aQute.p2.packed;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

public class Unpack200 extends Processor {

	public static final String	PACKED_SUFFIX	= ".pack.gz";

	private boolean				canUnpack		= false;
	private String				unpackCommand	= null;

	private static final Logger	logger			= LoggerFactory.getLogger(Unpack200.class);

	public Unpack200(Processor parent) {
		super(parent);
		prepare();
	}

	public Unpack200() {
		super();
		prepare();
	}

	private void prepare() {
		String commandStr = getJavaExecutable("unpack200");
		StringBuffer sb = new StringBuffer();
		Command cmd = new Command();
		cmd.add(commandStr);
		cmd.add("-V");
		int result;
		try {
			logger.debug("Calling: {}", cmd.toString());
			result = cmd.execute(sb, sb);
		} catch (Exception e) {
			logger.error("Error: " + sb.toString(), e);
			result = -1;
		}
		if (result == 0) {
			unpackCommand = commandStr;
			canUnpack = true;
		}
	}

	public boolean canUnpack() {
		return canUnpack;
	}

	public File unpack(File input, File output) {
		File parent = output.getParentFile();
		if (!parent.exists())
			parent.mkdirs();

		StringBuffer sb = new StringBuffer();
		try {

			Command cmd = new Command();
			cmd.add(unpackCommand);
			cmd.add(input.getCanonicalPath());
			cmd.add(output.getCanonicalPath());

			logger.debug("Calling: {}", cmd.toString());
			int result = cmd.execute(sb, sb);

			return output;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			logger.error("Error: " + sb.toString(), e);
			throw new RuntimeException(e);
		}
	}

	public File unpackAndLinkIfNeeded(TaggedData tag, File link) throws Exception {

		File source = tag.getFile();
		if (isPacked(tag) && tag.isOk()) {
			File original = new File(source.getParentFile(), source.getName() + ".original");
			if (original.exists())
				original.delete();
			IO.rename(source, original);
			unpack(original, source);
		}
		if (link != null) {
			IO.createSymbolicLinkOrCopy(link, source);
			return link;
		}
		return source;
	}

	private boolean isPacked(TaggedData tag) {
		return tag.getUrl()
			.getPath()
			.endsWith(PACKED_SUFFIX);
	}
}
