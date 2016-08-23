---
layout: default
class: Builder
title: -preprocessmatchers FILE-SPEC 
summary: Specify which files can be preprocessed 
---



		String							DEFAULT_PREPROCESSS_MATCHERS				= "!*.(jpg|jpeg|jif|jfif|jp2|jpx|j2k|j2c|fpx|png|gif|swf|doc|pdf|tiff|tif|raw|bmp|ppm|pgm|pbm|pnm|pfm|webp|zip|jar|gz|tar|tgz|exe|com|bin|mp[0-9]|mpeg|mov|):i, *";


	private Instructions getPreProcessMatcher(Map<String,String> extra) {
		if (defaultPreProcessMatcher == null) {
			defaultPreProcessMatcher = new Instructions(getProperty(PREPROCESSMATCHERS,
					Constants.DEFAULT_PREPROCESSS_MATCHERS));
		}
		if (extra == null)
			return defaultPreProcessMatcher;

		String additionalMatchers = extra.get(PREPROCESSMATCHERS);
		if (additionalMatchers == null)
			return defaultPreProcessMatcher;

		Instructions specialMatcher = new Instructions(additionalMatchers);
		specialMatcher.putAll(defaultPreProcessMatcher);
		return specialMatcher;
	}
