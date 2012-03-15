package aQute.lib.getopt;

import java.util.*;

public interface Options {
	List<String> _();
	CommandLine _command();
	Map<String,String> _properties();
	boolean _ok();
	boolean _help();
}
