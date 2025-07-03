---
layout: default
class: Ant
title: -noee  BOOLEAN
since: 2.3
summary:  Donot add an automatic requirement on an EE capability based on the class format.
---

			//
			// We want to add the minimum EE as a requirement
			// based on the class version
			//

			if (!isTrue(getProperty(NOEE)) //
					&& !ees.isEmpty() // no use otherwise
					&& since(About._2_3) // we want people to not have to
											// automatically add it
					&& !requirements.containsKey(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE) // and
																												// it
																												// should
																												// not
																												// be
																												// there
																												// already
			) {

				JAVA highest = ees.last();
				Attrs attrs = new Attrs();

				String filter = doEEProfiles(highest);

				attrs.put(Constants.FILTER_DIRECTIVE, filter);

				//
				// Java 1.8 introduced profiles.
				// If -eeprofile= auto | (<profile>="...")+ is set then
				// we add a

				requirements.add(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, attrs);
			}
