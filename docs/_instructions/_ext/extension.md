---
layout: default
class: Project
title: -extension 
summary: A plugin that is loaded to its url, downloaded and then provides a header used instantiate the plugin. 
---
	
	/**
	 * Add any extensions listed
	 * 
	 * @param list
	 * @param rri
	 */
	@Override
	protected void addExtensions(Set<Object> list) {
		//
		// <bsn>; version=<range>
		//
		Parameters extensions = new Parameters(getProperty(EXTENSION));
		Map<DownloadBlocker,Attrs> blockers = new HashMap<DownloadBlocker,Attrs>();

		for (Entry<String,Attrs> i : extensions.entrySet()) {
			String bsn = removeDuplicateMarker(i.getKey());
			String stringRange = i.getValue().get(VERSION_ATTRIBUTE);

			trace("Adding extension %s-%s", bsn, stringRange);

			if (stringRange == null)
				stringRange = Version.LOWEST.toString();
			else if (!VersionRange.isVersionRange(stringRange)) {
				error("Invalid version range %s on extension %s", stringRange, bsn);
				continue;
			}
			try {
				SortedSet<ResourceDescriptor> matches = resourceRepositoryImpl.find(null, bsn, new VersionRange(
						stringRange));
				if (matches.isEmpty()) {
					error("Extension %s;version=%s not found in base repo", bsn, stringRange);
					continue;
				}

				DownloadBlocker blocker = new DownloadBlocker(this);
				blockers.put(blocker, i.getValue());
				resourceRepositoryImpl.getResource(matches.last().id, blocker);
			}
			catch (Exception e) {
				error("Failed to load extension %s-%s, %s", bsn, stringRange, e);
			}
		}

		trace("Found extensions %s", blockers);

		for (Entry<DownloadBlocker,Attrs> blocker : blockers.entrySet()) {
			try {
				String reason = blocker.getKey().getReason();
				if (reason != null) {
					error("Extension load failed: %s", reason);
					continue;
				}

				URLClassLoader cl = new URLClassLoader(new URL[] {
					blocker.getKey().getFile().toURI().toURL()
				}, getClass().getClassLoader());
				Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
				while (manifests.hasMoreElements()) {
					Manifest m = new Manifest(manifests.nextElement().openStream());
					Parameters activators = new Parameters(m.getMainAttributes().getValue("Extension-Activator"));
					for (Entry<String,Attrs> e : activators.entrySet()) {
						try {
							Class< ? > c = cl.loadClass(e.getKey());
							ExtensionActivator extensionActivator = (ExtensionActivator) c.newInstance();
							customize(extensionActivator, blocker.getValue());
							List< ? > plugins = extensionActivator.activate(this, blocker.getValue());
							list.add(extensionActivator);

							if (plugins != null)
								for (Object plugin : plugins) {
									list.add(plugin);
								}
						}
						catch (ClassNotFoundException cnfe) {
							error("Loading extension %s, extension activator missing: %s (ignored)", blocker,
									e.getKey());
						}
					}
				}
			}
			catch (Exception e) {
				error("failed to install extension %s due to %s", blocker, e);
			}
		}
	}
