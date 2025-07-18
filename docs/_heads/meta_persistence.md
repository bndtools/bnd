---
layout: default
title: Meta-Persistence ::= ( RESOURCE ( ',' RESOURCE )* )?
class: Header
summary: |
   A Persistence Bundle is a bundle that contains the Meta-Persistence header. If this header is not present, then this specification does not apply and a JPA Provider should ignore the corresponding bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Meta-Persistence: persistence/myPu.xml`

- Pattern: `.*`

<!-- Manual content from: ext/meta_persistence.md --><br /><br />

# Meta-Persistence

The `Meta-Persistence` header marks a bundle as a JPA (Java Persistence API) persistence bundle. It lists one or more resource paths (typically XML files) that describe the persistence units. If this header is not present, the bundle is not considered a persistence bundle and should be ignored by JPA providers.

Example:

```
Meta-Persistence: META-INF/persistence.xml
```

bnd will verify that the listed resources exist in the bundle. This header is required for JPA persistence bundles.
	
	/**
	 * Verify the Meta-Persistence header
	 * 
	 * @throws Exception
	 */

	public void verifyMetaPersistence() throws Exception {
		List<String> list = new ArrayList<String>();
		String mp = dot.getManifest().getMainAttributes().getValue(META_PERSISTENCE);
		for (String location : OSGiHeader.parseHeader(mp).keySet()) {
			String[] parts = location.split("!/");

			Resource resource = dot.getResource(parts[0]);
			if (resource == null)
				list.add(location);
			else {
				if (parts.length > 1) {
					Jar jar = new Jar("", resource.openInputStream());
					try {
						resource = jar.getResource(parts[1]);
						if (resource == null)
							list.add(location);
					}
					catch (Exception e) {
						list.add(location);
					}
					finally {
						jar.close();
					}
				}
			}
		}
		if (list.isEmpty())
			return;

		error(Constants.META_PERSISTENCE + " refers to resources not in the bundle: %s", list).header(Constants.META_PERSISTENCE);
	}


TODO Needs review - AI Generated content
