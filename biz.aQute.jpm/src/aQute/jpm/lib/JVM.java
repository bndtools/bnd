package aQute.jpm.lib;

import java.util.*;

import aQute.struct.*;

public class JVM extends struct {
	public static Comparator<JVM>	comparator		= new Comparator<JVM>() {

														public int compare(JVM a, JVM b) {
															return a.version.compareTo(b.version);
														}
													};

	public String					platformVersion;
	public String					path;
	public String					platformRoot;
	public String					version;
	public String					vendor;
	public List<String>				capabilities	= new ArrayList<String>();
	public String					name;

}
