package aQute.bnd.service.specifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A specification of the Builder parameters without any special types. This
 * specification can be used to persist or for remote.
 * <p>
 * This class should not inherit DTO, however convenient this might be, since
 * this creates unwanted dependencies on clients. These clients include test
 * code that is severely handicapped by these kind of dependencies.
 * <p>
 * All file paths must be absolute and platform specific format
 */
public class BuilderSpecification {

	public static final String				WORKSPACE			= "WORKSPACE";
	public static final String				PROJECT				= "PROJECT";

	/**
	 * Defines inheritance. Either {@link #WORKSPACE}, {@link #PROJECT}, or a
	 * path to a bnd file. Last entry may be WORKSPACE or PROJECT
	 */
	public List<String>						parent				= new ArrayList<>();
	public List<String>						classpath			= new ArrayList<>();
	public String							location;
	public Map<String, Map<String, String>>	bundleSymbolicName	= new LinkedHashMap<>();
	public String							bundleVersion;
	public String							bundleActivator;
	public Map<String, Map<String, String>>	fragmentHost		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	requireBundle		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	bundleNativeCode	= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	importPackage		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	exportPackage		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	provideCapability	= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	requireCapability	= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	includeresource		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	exportContents		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	privatePackage		= new LinkedHashMap<>();
	public boolean							failOk;
	public boolean							sources;
	public Map<String, String>				other				= new HashMap<>();
	public boolean							resourceOnly;
	/**
	 * A glob expression on the file name of the bundle to merge
	 */
	public String							testBundle;
	@Deprecated
	public boolean							inherit				= false;
}
