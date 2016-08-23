---
layout: default
class: Header
title: Fragment-Host       ::= bundle-description 
summary: The Fragment-Host header defines the host bundles for this fragment.
---
	
	The Fragment-Host manifest header links the fragment to its potential hosts. It must conform to the following syntax:
Fragment-Host       ::= bundle-description
bundle-description  ::= symbolic-name
                            ( ';' parameter )* // See 1.3.2
The following directives are architected by the Framework for Fragment-Host:
• extension - Indicates this extension is a system or boot class path extension. It is only applica- ble when the Fragment-Host is the System Bundle. This is discussed in Extension Bundles on page 85. The following values are supported:
• framework - The fragment bundle is a Framework extension bundle (default).
• bootclasspath - The fragment bundle is a boot class path extension bundle.
The fragment must be the bundle symbolic name of the implementation specific system bundle or the alias system.bundle. The Framework should fail to install an extension bundle when the bundle symbolic name is not referring to the system bundle.
The following attributes are architected by the Framework for Fragment-Host:
• bundle-version - The version range to select the host bundle. If a range is used, then the frag- ment can attach to multiple hosts. See Semantic Versioning on page 54. The default value is [0.0.0,∞).
The Fragment-Host header can assert arbitrary attributes that must be matched before a host is eligi- ble.
	
	
		verifyDirectives(Constants.FRAGMENT_HOST, "extension:", SYMBOLICNAME, "bsn");
