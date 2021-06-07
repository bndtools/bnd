---
layout: default
class: Processor
title: native_capability ( ';' ( 'os.name' | 'os.version' | 'os.processor' ) '=' STRING )*
summary: Create a Require-Capability header based on the current platform or explicit values
---

	/**
	 * <p>
	 * Generates a Capability string, in the format specified by the OSGi
	 * Provide-Capability header, representing the current native platform
	 * according to OSGi RFC 188. For example on Windows7 running on an x86_64
	 * processor it should generate the following:
	 * </p>
	 * 
	 * <pre>
	 * osgi.native;osgi.native.osname:List&lt;String&gt;="Windows7,Windows 7,Win32";osgi.native.osversion:Version=6.1.0;osgi.native.processor:List&lt;String&gt;="x86-64,amd64,em64t,x86_64"
	 * </pre>
	 * 
	 * @param args
	 *            The array of properties. For example: the macro invocation of
	 *            "${native_capability;osversion=3.2.4;osname=Linux}" results in
	 *            an args array of
	 *            [native_capability,&nbsp;osversion=3.2.4,&nbsp;osname=Linux]
	 */

	public String _native_capability(String[] args) throws IllegalArgumentException {
		StringBuilder builder = new StringBuilder().append(OSGI_NATIVE);

		String processorNames = null;
		OSInformation osInformation = null;
		IllegalArgumentException osInformationException = null;
		/*
		 * Determine the processor information
		 */
		String[] aliases = OSInformation.getProcessorAliases(System.getProperty("os.arch"));
		if (aliases != null)
			processorNames = Strings.join(aliases);

		/*
		 * Determine the OS information
		 */

		try {
			osInformation = new OSInformation();
		}
		catch (IllegalArgumentException e) {
			osInformationException = e;
		}

		/*
		 * Determine overrides
		 */

		String osnameOverride = null;
		Version osversionOverride = null;
		String processorNamesOverride = null;

		if (args.length > 1) {
			assert ("native_capability".equals(args[0]));
			for (int i = 1; i < args.length; i++) {
				String arg = args[i];
				String[] fields = arg.split("=", 2);
				if (fields.length != 2) {
					throw new IllegalArgumentException("Illegal property syntax in \"" + arg + "\", use \"key=value\"");
				}
				String key = fields[0];
				String value = fields[1];
				if (OS_NAME.equals(key)) {
					osnameOverride = value;
				} else if (OS_VERSION.equals(key)) {
					osversionOverride = new Version(value);
				} else if (OS_PROCESSOR.equals(key)) {
					processorNamesOverride = value;
				} else {
					throw new IllegalArgumentException("Unrecognised/unsupported property. Supported: " + OS_NAME
							+ ", " + OS_VERSION + ", " + OS_PROCESSOR + ".");
				}
			}
		}

		/*
		 * Determine effective values: put determined value into override if
		 * there is no override
		 */

		if (osnameOverride == null && osInformation != null) {
			osnameOverride = osInformation.osnames;
		}
		if (osversionOverride == null && osInformation != null) {
			osversionOverride = osInformation.osversion;
		}
		if (processorNamesOverride == null && processorNames != null) {
			processorNamesOverride = processorNames;
		}

		/*
		 * Construct result string
		 */

		builder.append(";" + OSGI_NATIVE + "." + OS_NAME + ":List<String>=\"").append(osnameOverride).append('"');
		builder.append(";" + OSGI_NATIVE + "." + OS_VERSION + ":Version=").append(osversionOverride);
		builder.append(";" + OSGI_NATIVE + "." + OS_PROCESSOR + ":List<String>=\"").append(processorNamesOverride)
				.append('"');

		/*
		 * Report error if needed
		 */

		if (osnameOverride == null || osversionOverride == null || processorNamesOverride == null) {
			throw new IllegalArgumentException(
					"At least one of the required parameters could not be detected; specify an override. Detected: "
							+ builder.toString(), osInformationException);
		}

		return builder.toString();
	}

