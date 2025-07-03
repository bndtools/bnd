---
layout: default
title: Bundle-NativeCode   ::= nativecode  ( ',' nativecode )* ( ',' optional ) ?
class: Header
summary: |
   The Bundle-NativeCode header contains a specification of native code libraries contained in this bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-NativeCode: /lib/http.DLL; osname = QNX; osversion = 3.1`

- Pattern: `.*`

### Options ###

- `osname`
  - Example: `osname=MacOS`

  - Values: `AIX,DigitalUnix,Embos,Epoc32,FreeBSD,HPUX,IRIX,Linux,MacOS,NetBSD,Netware,OpenBSD,OS2,QNX,Solaris,SunOS,VxWorks,Windows95,Win32,Windows98,WindowsNT,WindowsCE,Windows2000,Windows2003,WindowsXP,WindowsVista`

  - Pattern: `.*`


- `osversion`
  - Example: `osversion=3.1`

  - Pattern: `.*`


- `language`
  - Example: `language=nl`

  - Pattern: `\p{Upper}{2}`


- `processor`
  - Example: `processor=x86`

  - Values: `68k,ARM_LE,arm_le,arm_be,Alpha,ia64n,ia64w,Ignite,Mips,PArisc,PowerPC,Sh4,Sparc,Sparcv9,S390,S390x,V850E,x86,i486,x86-64`

  - Pattern: `.*`


- `selection-filter`
  - Example: `selection-filter="(com.acme.windowing=win32)"`

  - Pattern: `.*`

<!-- Manual content from: ext/bundle_nativecode.md --><br /><br />
	
	/*
	 * Bundle-NativeCode ::= nativecode ( ',' nativecode )* ( ’,’ optional) ?
	 * nativecode ::= path ( ';' path )* // See 1.4.2 ( ';' parameter )+
	 * optional ::= ’*’
	 */
	public void verifyNative() {
		String nc = get(Constants.BUNDLE_NATIVECODE);
		doNative(nc);
	}

	public void doNative(String nc) {
		if (nc != null) {
			QuotedTokenizer qt = new QuotedTokenizer(nc, ",;=", false);
			char del;
			do {
				do {
					String name = qt.nextToken();
					if (name == null) {
						error("Can not parse name from bundle native code header: " + nc);
						return;
					}
					del = qt.getSeparator();
					if (del == ';') {
						if (dot != null && !dot.exists(name)) {
							error("Native library not found in JAR: " + name);
						}
					} else {
						String value = null;
						if (del == '=')
							value = qt.nextToken();

						String key = name.toLowerCase();
						if (key.equals("osname")) {
							// ...
						} else if (key.equals("osversion")) {
							// verify version range
							verify(value, VERSIONRANGE);
						} else if (key.equals("language")) {
							verify(value, ISO639);
						} else if (key.equals("processor")) {
							// verify(value, PROCESSORS);
						} else if (key.equals("selection-filter")) {
							// verify syntax filter
							verifyFilter(value);
						} else if (name.equals("*") && value == null) {
							// Wildcard must be at end.
							if (qt.nextToken() != null)
								error("Bundle-Native code header may only END in wildcard: nc");
						} else {
							warning("Unknown attribute in native code: " + name + "=" + value);
						}
						del = qt.getSeparator();
					}
				} while (del == ';');
			} while (del == ',');
		}
	}

	public boolean verifyFilter(String value) {
		String s = validateFilter(value);
		if (s == null)
			return true;

		error(s);
		return false;
	}

	public static String validateFilter(String value) {
		try {
			verifyFilter(value, 0);
			return null;
		}
		catch (Exception e) {
			return "Not a valid filter: " + value + e.getMessage();
		}
	}
