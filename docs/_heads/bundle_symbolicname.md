---
layout: default
title: Bundle-SymbolicName ::= symbolic-name ( ';' parameter ) *
class: Header
summary: |
   The Bundle-SymbolicName header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle though it can be installed multiple times in a framework. The bundle symbolic name should be based on the reverse domain name convention, s
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-SymbolicName: com.acme.foo.daffy;singleton:=true`

- Values: `${p}`

- Pattern: `[-\w]+(:?\.[-\w]+)*`

### Options 

- `singleton:` Indicates that the bundle can only have a single version resolved. A value of true indicates that the bundle is a singleton bundle. The default value is false. The Framework must resolve at most one bundle when multiple versions of a singleton bundle with the same symbolic name are installed. Singleton bundles do not affect the resolution of non-singleton bundles with the same symbolic name.
  - Example: `singleton:=false`

  - Values: `true,false`

  - Pattern: `true|false|TRUE|FALSE`


- `fragment-attachment:` Defines how fragments are allowed to be attached, see the fragments in Fragment Bundles on page 73. The following values are valid for this directive:
  - Example: ``

  - Values: `always|never|resolve-time`

  - Pattern: `always|never|resolve-time`


- `blueprint.wait-for-dependencies` 
  - Example: ``

  - Values: `true,false`

  - Pattern: `true|false|TRUE|FALSE`


- `blueprint.timeout` 
  - Example: ``

  - Values: `30000,60000,300000`

  - Pattern: `\d+`

<!-- Manual content from: ext/bundle_symbolicname.md --><br /><br />
	
The Bundle-SymbolicName header can be set by the user. The default is the name of the main bnd file, or if the main bnd file is called bnd.bnd, it will be the name of the directory of the bnd file. An interesting variable is ${project} that will be set to this default name.


			private void verifySymbolicName() {
		Parameters bsn = parseHeader(main.get(Analyzer.BUNDLE_SYMBOLICNAME));
		if (!bsn.isEmpty()) {
			if (bsn.size() > 1)
				error("More than one BSN specified " + bsn);

			String name = bsn.keySet().iterator().next();
			if (!isBsn(name)) {
				error("Symbolic Name has invalid format: " + name);
			}
		}
	}
		
			/**
	 * @param name
	 * @return
	 */
	public static boolean isBsn(String name) {
		return SYMBOLICNAME.matcher(name).matches();
	}

		
	public final static String	SYMBOLICNAME_STRING				= "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";
		
