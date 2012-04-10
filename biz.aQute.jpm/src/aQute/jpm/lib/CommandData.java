package aQute.jpm.lib;

import java.io.*;

import aQute.lib.data.*;
import aQute.libg.version.*;

public class CommandData {
	public long																time	= System.currentTimeMillis();
	@Validator(JustAnotherPackageManager.COMMAND_PATTERN) public String		name;
	public boolean															force;
	@AllowNull public String												vmArgs	= "";
	@Validator(JustAnotherPackageManager.BSN_PATTERN) public String			bsn;
	@Validator(JustAnotherPackageManager.VERSION_PATTERN) public Version	version = Version.LOWEST;
	public File																repoFile;
	@AllowNull public String												path;
	@Validator(JustAnotherPackageManager.MAINCLASS_PATTERN) public String	main;

	public String toString() {
		return "[" + name + "]";
	}
}
