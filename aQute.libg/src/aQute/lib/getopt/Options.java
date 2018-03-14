package aQute.lib.getopt;

import java.util.List;
import java.util.Map;

public interface Options {
	List<String> _arguments();

	CommandLine _command();

	Map<String, String> _properties();

	boolean _ok();

	boolean _help();
}
