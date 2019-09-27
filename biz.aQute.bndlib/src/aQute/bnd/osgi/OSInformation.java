package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.resource.Capability;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.strings.Strings;

/**
 * OS specific information, used by the native_capability macro for
 * osgi.native.* bundle properties.
 */
public class OSInformation {
	String							osnames							= null;
	Version							osversion						= null;

	private final static String		regexQualifierNotAllowedChars	= "[^-\\w]";
	private final static Pattern	digitPattern					= Pattern.compile("(\\d+).*");

	final static String[][]			processorFamilies				= {
		new String[] {
			"x86-64", "amd64", "em64t", "x86_64"
		}, new String[] {
			"x86", "pentium", "i386", "i486", "i586", "i686"
		}, new String[] {
			"68k"
		}, new String[] {
			"ARM"
		}, new String[] {
			"ARM_be"
		}, new String[] {
			"ARM_le"
		}, new String[] {
			"Alpha"
		}, new String[] {
			"ia64n"
		}, new String[] {
			"ia64w"
		}, new String[] {
			"Ignite", "psc1k"
		}, new String[] {
			"Mips"
		}, new String[] {
			"PARisc"
		}, new String[] {
			"PowerPC", "power", "ppc"
		}, new String[] {
			"Sh4"
		}, new String[] {
			"Sparc"
		}, new String[] {
			"Sparcv9"
		}, new String[] {
			"S390"
		}, new String[] {
			"V850e"
		},
	};
	final static String[]			osarch							= getProcessorAliases(
		System.getProperty("os.arch"));

	public static String[] getProcessorAliases(String osArch) {
		for (String[] pnames : processorFamilies) {
			for (String pname : pnames)
				if (pname.equalsIgnoreCase(osArch))
					return pnames;
		}
		return null;
	}

	public static String[] getProcessorAliases() {
		return osarch;
	}

	/**
	 * <p>
	 * Convert a generic Unix kernel version to an OSGi version.
	 * </p>
	 * <p>
	 * As long as we have digits separated by dots, convert the digits into the
	 * respective version segments. Anything left after that conversion is the
	 * qualifier. Illegal characters in that qualifier are converted into
	 * underscores to ensure that the final qualifier is valid.
	 * </p>
	 *
	 * @param sysPropOsVersion the system property "os.version"
	 */
	static Version convertUnixKernelVersion(String sysPropOsVersion) {
		Version osversion = new Version(0, 0, 0);
		String s = sysPropOsVersion.trim();
		int index = 0;
		do {
			Matcher matcher = digitPattern.matcher(s);
			if (matcher.matches()) {
				String matchedDigit = matcher.group(1);
				int matchedDigitNumber;
				try {
					matchedDigitNumber = Integer.parseInt(matchedDigit);
				} catch (NumberFormatException e) {
					assert (false);
					break;
				}

				switch (index) {
					case 0 :
						osversion = new Version(matchedDigitNumber, osversion.getMinor(), osversion.getMicro());
						break;

					case 1 :
						osversion = new Version(osversion.getMajor(), matchedDigitNumber, osversion.getMicro());
						break;

					case 2 :
						osversion = new Version(osversion.getMajor(), osversion.getMinor(), matchedDigitNumber);
						break;

					default :
						assert (false);
						break;
				}

				s = s.substring(matchedDigit.length());

				if (s.length() == 0 || s.charAt(0) != '.') {
					break;
				}

				s = s.substring(1);

				index++;
			}
		} while (index < 3);

		if (s.length() != 0) {
			String qualifier = s.replaceAll(regexQualifierNotAllowedChars, "_");
			osversion = new Version(osversion.getMajor(), osversion.getMinor(), osversion.getMicro(), qualifier);
		}
		return osversion;
	}

	/**
	 * Construct OS specific information
	 *
	 * @throws IllegalArgumentException
	 */
	public OSInformation() throws IllegalArgumentException {
		this(System.getProperty("os.name"), System.getProperty("os.version"));
	}

	public OSInformation(String sysPropOsName, String sysPropOsVersion) throws IllegalArgumentException {

		if (sysPropOsName == null || sysPropOsName.length() == 0 || sysPropOsVersion == null
			|| sysPropOsVersion.length() == 0) {
			return;
		}
		OSNameVersion pair = getOperatingSystemAliases(sysPropOsName, sysPropOsVersion);
		if (pair == null)
			throw new IllegalArgumentException(
				"Unknown OS/version combination: " + sysPropOsName + " " + sysPropOsVersion);

		osversion = pair.osversion;
		osnames = pair.osnames;
	}

	static class NativeCapability {
		public List<String>	processor	= new ArrayList<>();
		public List<String>	osname		= new ArrayList<>();
		public Version		osversion;
		public String		language	= "en";
	}

	/**
	 * Helper for the Processor._native_capability macro
	 *
	 * @param args the arguments of the macro
	 * @return a provide capability clause for the native environment
	 */
	public static String getNativeCapabilityClause(Processor p, String args[]) throws Exception {
		NativeCapability clause = new NativeCapability();

		parseNativeCapabilityArgs(p, args, clause);

		validateNativeCapability(clause);

		Capability cap = createCapability(clause);

		return ResourceUtils.toProvideCapability(cap);
	}

	static Capability createCapability(NativeCapability clause) throws Exception {
		CapabilityBuilder c = new CapabilityBuilder("osgi.native");
		c.addAttribute("osgi.native.osname", clause.osname);
		c.addAttribute("osgi.native.osversion", clause.osversion);
		c.addAttribute("osgi.native.processor", clause.processor);
		c.addAttribute("osgi.native.language", clause.language);
		Capability cap = c.synthetic();
		return cap;
	}

	static void validateNativeCapability(NativeCapability clause) {
		if (clause.osversion == null)
			throw new IllegalArgumentException("osversion/osgi.native.osversion not set in ${native_capability}");
		if (clause.osname.isEmpty())
			throw new IllegalArgumentException("osname/osgi.native.osname not set in ${native_capability}");
		if (clause.processor.isEmpty())
			throw new IllegalArgumentException("processor/osgi.native.processor not set in ${native_capability}");
	}

	static void parseNativeCapabilityArgs(Processor p, String[] args, NativeCapability clause) throws Exception {
		if (args.length == 1) {

			OSInformation osi = new OSInformation();
			clause.osname.addAll(Strings.split(osi.osnames));
			clause.osversion = osi.osversion;
			clause.processor.addAll(Arrays.asList(getProcessorAliases(System.getProperty("os.arch"))));
			clause.language = Locale.getDefault()
				.toString();

			StringBuilder sb = new StringBuilder();
			sb.append("osname=")
				.append(System.getProperty("os.name"));
			sb.append(";")
				.append("osversion=")
				.append(MavenVersion.cleanupVersion(System.getProperty("os.version")));
			sb.append(";")
				.append("processor=")
				.append(System.getProperty("os.arch"));
			sb.append(";")
				.append("lang=")
				.append(clause.language);
			String advice = sb.toString();
		} else {

			String osname = null;

			for (int i = 1; i < args.length; i++) {

				String parts[] = args[i].split("\\s*=\\s*");
				if (parts.length != 2)
					throw new IllegalArgumentException(
						"Illegal property syntax in \"" + args[i] + "\", use \"key=value\"");

				String key = Strings.trim(parts[0]);
				String value = Strings.trim(parts[1]);
				boolean isList = value.indexOf(',') > 0;

				switch (key) {
					case "processor" :
					case "osgi.native.processor" :
						if (isList)
							clause.processor.addAll(Strings.split(value));
						else {
							if ("arm".equals(value)) {
								p.warning("The 'arm' processor is deprecated. Specify either 'arm_le' or 'arm_be'");
							}
							String[] processorAliases = getProcessorAliases(value);
							if (processorAliases != null && processorAliases.length > 0) {
								clause.processor.addAll(Arrays.asList(processorAliases));
							} else {
								clause.processor.add(value);
							}
						}
						break;

					case "osname" :
					case "osgi.native.osname" :
						if (isList)
							clause.osname.addAll(Strings.split(value));
						else {
							if (osname == null) {
								osname = value;
							} else {
								clause.osname.add(osname);
								osname = value;
							}
						}
						break;

					case "osversion" :
					case "osgi.native.osversion" :
						if (clause.osversion == null) {
							clause.osversion = Version.parseVersion(value);
						} else
							throw new IllegalArgumentException(
								"osversion/osgi.native.osversion can only be set once in ${native_capability}");
						break;

					case "osgi.native.language" :
					case "lang" :
						if (clause.language != null)
							throw new IllegalArgumentException(
								"lang/osgi.native.lang can only be set once in ${native_capability}");

						clause.language = value;
						break;
				}

			}
			if (osname != null) {
				try {
					OSInformation osi = new OSInformation(osname, clause.osversion.toString());
					clause.osname.addAll(Strings.split(osi.osnames));
				} catch (Exception e) {
					clause.osname.add(osname);
				}
			}
		}
	}

	public static class OSNameVersion {
		public Version	osversion;
		public String	osnames;
	}

	public static OSNameVersion getOperatingSystemAliases(String sysPropOsName, String sysPropOsVersion) {
		OSNameVersion nc = new OSNameVersion();

		if (sysPropOsName.startsWith("Windows")) {
			if (sysPropOsVersion.startsWith("10.0")) {
				nc.osversion = new Version(10, 0, 0);
				nc.osnames = "Windows10,Windows 10,Win32";
			} else if (sysPropOsVersion.startsWith("6.3")) {
				nc.osversion = new Version(6, 3, 0);
				nc.osnames = "Windows8.1,Windows 8.1,Win32";
			} else if (sysPropOsVersion.startsWith("6.2")) {
				nc.osversion = new Version(6, 2, 0);
				nc.osnames = "Windows8,Windows 8,Win32";
			} else if (sysPropOsVersion.startsWith("6.1")) {
				nc.osversion = new Version(6, 1, 0);
				nc.osnames = "Windows7,Windows 7,Win32";
			} else if (sysPropOsVersion.startsWith("6.0")) {
				nc.osversion = new Version(6, 0, 0);
				nc.osnames = "WindowsVista,WinVista,Windows Vista,Win32";
			} else if (sysPropOsVersion.startsWith("5.1")) {
				nc.osversion = new Version(5, 1, 0);
				nc.osnames = "WindowsXP,WinXP,Windows XP,Win32";
			} else {
				nc = null;
			}
		} else if (sysPropOsName.startsWith("Mac OS X")) {
			nc.osversion = convertUnixKernelVersion(sysPropOsVersion);
			nc.osnames = "MacOSX,Mac OS X";
			return nc;
		} else if (sysPropOsName.toLowerCase()
			.startsWith("linux")) {
			nc.osversion = convertUnixKernelVersion(sysPropOsVersion);
			nc.osnames = "Linux";
		} else if (sysPropOsName.startsWith("Solaris")) {
			nc.osversion = convertUnixKernelVersion(sysPropOsVersion);
			nc.osnames = "Solaris";
		} else if (sysPropOsName.startsWith("AIX")) {
			nc.osversion = convertUnixKernelVersion(sysPropOsVersion);
			nc.osnames = "AIX";
		} else if (sysPropOsName.startsWith("HP-UX")) {
			nc.osversion = convertUnixKernelVersion(sysPropOsVersion);
			nc.osnames = "HPUX,hp-ux";
		}
		return nc;
	}
}
