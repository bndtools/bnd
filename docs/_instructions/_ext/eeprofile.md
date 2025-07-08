---
layout: default
class: Project
title: -eeprofile 'auto' | PROFILE +
summary: Provides control over what Java 8 profile to use.
---
	
	
		/**
	 * Added for 1.8 profiles. A 1.8 profile is a set of packages so the VM can
	 * be delivered in smaller versions. This method will look at the
	 * {@link Constants#EEPROFILE} option. If it is set, it can be "auto" or it
	 * can contain a list of profiles specified as name="a,b,c" values. If we
	 * find a package outside the profiles, no profile is set. Otherwise the
	 * highest found profile is added. This only works for java packages.
	 */
	private String doEEProfiles(JAVA highest) throws IOException {
		String ee = getProperty(EEPROFILE);
		if (ee == null)
			return highest.getFilter();

		ee = ee.trim();

		Map<String,Set<String>> profiles;

		if (ee.equals(EEPROFILE_AUTO_ATTRIBUTE)) {
			profiles = highest.getProfiles();
			if (profiles == null)
				return highest.getFilter();
		} else {
			Attrs t = OSGiHeader.parseProperties(ee);
			profiles = new HashMap<String,Set<String>>();

			for (Map.Entry<String,String> e : t.entrySet()) {
				String profile = e.getKey();
				String l = e.getValue();
				SortedList<String> sl = new SortedList<String>(l.split("\\s*,\\s*"));
				profiles.put(profile, sl);
			}
		}
		SortedSet<String> found = new TreeSet<String>();
		nextPackage: for (PackageRef p : referred.keySet()) {
			if (p.isJava()) {
				String fqn = p.getFQN();
				for (Entry<String,Set<String>> entry : profiles.entrySet()) {
					if (entry.getValue().contains(fqn)) {

						found.add(entry.getKey());

						//
						// Check if we found all the possible profiles
						// that means we're finished
						//

						if (found.size() == profiles.size())
							break nextPackage;

						//
						// Profiles should be exclusive
						// so we can break if we found one
						//
						continue nextPackage;
					}
				}

				//
				// Ouch, outside any profile
				//
				return highest.getFilter();
			}
		}

		String filter = highest.getFilter();
		if (!found.isEmpty())
			filter = filter.replaceAll("JavaSE", "JavaSE/" + found.last());
		// TODO a more elegant way to build the filter, we now assume JavaSE
		return filter;

	}