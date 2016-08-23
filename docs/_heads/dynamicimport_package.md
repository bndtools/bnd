---
layout: default
class: Header
title: DynamicImport-Package ::= dynamic-description ( ',' dynamic-description )* 
summary: The DynamicImport-Package header contains a comma-separated list of package names that should be dynamically imported when needed.
---
	
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

	