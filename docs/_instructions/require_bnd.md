---
layout: default
class: Project
title: -require-bnd  (FILTER ( ',' FILTER )* )?
summary: The filter can test aginst 'version', which will contain the bnd version. If it does not match, bnd will generate an error.  
---

	/**
	 * Ensure that we are running on the correct bnd.
	 */
	void doRequireBnd() {
		Attrs require = OSGiHeader.parseProperties(getProperty(REQUIRE_BND));
		if (require == null || require.isEmpty())
			return;

		Hashtable<String,String> map = new Hashtable<String,String>();
		map.put(Constants.VERSION_FILTER, getBndVersion());

		for (String filter : require.keySet()) {
			try {
				Filter f = new Filter(filter);
				if (f.match(map))
					continue;
				error("%s fails %s", REQUIRE_BND, require.get(filter));
			}
			catch (Exception t) {
				error("%s with value %s throws exception", t, REQUIRE_BND, require);
			}
		}
	}

