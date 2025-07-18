---
layout: default
title: DynamicImport-Package ::= dynamic-description ( ',' dynamic-description )*
class: Header
summary: |
   The DynamicImport-Package header contains a comma-separated list of package names that should be dynamically imported when needed.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `DynamicImport-Package: com.acme.plugin.*`

- Values: ``

- Pattern: `.*`

### Options 

- `version` A version range to select the version of an export definition. The default value is 0.0.0.
  - Example: `version="[1.2,3.0)"`

  - Pattern: `((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`


- `bundle-symbolic-name` The bundle symbolic name of the exporting bundle.
  - Example: `bundle-symbolic-name=com.acme.foo.daffy`

  - Pattern: `[-\w]+(:?\.[-\w]+)*`


- `bundle-version` A version range to select the bundle version of the exporting bundle. The default value is 0.0.0.
  - Example: `bundle-version=1.3`

  - Pattern: `((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/dynamicimport_package.md --><br /><br />

# DynamicImport-Package

The `DynamicImport-Package` header allows a bundle to declare packages that should be imported dynamically at runtime, rather than being resolved at deployment time. This is useful for cases where the set of required packages is not known in advance.

The header contains a comma-separated list of package names, which can include wildcards and directives. For example:

```
DynamicImport-Package: com.example.*, org.osgi.service.*
```

Packages listed here will be resolved by the OSGi framework when they are first referenced by the bundle. Use this feature sparingly, as it can make dependency management less predictable.

	/**
	 * <pre>
	 *          DynamicImport-Package ::= dynamic-description
	 *              ( ',' dynamic-description )*
	 *              
	 *          dynamic-description::= wildcard-names ( ';' parameter )*
	 *          wildcard-names ::= wildcard-name ( ';' wildcard-name )*
	 *          wildcard-name ::= package-name 
	 *                         | ( package-name '.*' ) // See 1.4.2
	 *                         | '*'
	 * </pre>
	 */
	private void verifyDynamicImportPackage() {
		verifyListHeader(Constants.DYNAMICIMPORT_PACKAGE, WILDCARDPACKAGE, true);
		String dynamicImportPackage = get(Constants.DYNAMICIMPORT_PACKAGE);
		if (dynamicImportPackage == null)
			return;

		Parameters map = main.getDynamicImportPackage();
		for (String name : map.keySet()) {
			name = name.trim();
			if (!verify(name, WILDCARDPACKAGE))
				error(Constants.DYNAMICIMPORT_PACKAGE + " header contains an invalid package name: " + name);

			Map<String,String> sub = map.get(name);
			if (r3 && sub.size() != 0) {
				error("DynamicPackage-Import has attributes on import: " + name
						+ ". This is however, an <=R3 bundle and attributes on this header were introduced in R4. ");
			}
		}
	}



TODO Needs review - AI Generated content
