---
layout: default
class: Processor
title: -pluginpath* PARAMETERS 
summary: Define JARs to be loaded in the local classloader for plugins. 
---

Plugins not embedded in bndlib must load their classes from JARs or directories. Though these JARs can be specified on the `-plugin` instruction, it is also possible to specify them separate. The `-pluginpath` is a merged property so it is possible to specify clauses in multiple places, these will all be merged together.





	/**
	 * Add the @link {@link Constants#PLUGINPATH} entries (which are file names)
	 * to the class loader. If this file does not exist, and there is a
	 * {@link Constants#PLUGINPATH_URL_ATTR} attribute then we download it first
	 * from that url. You can then also specify a
	 * {@link Constants#PLUGINPATH_SHA1_ATTR} attribute to verify the file.
	 * 
	 * @see PLUGINPATH
	 * @param pluginPath
	 *            the clauses for the plugin path
	 * @param loader
	 *            The class loader to extend
	 */
	private void loadPluginPath(Set<Object> instances, String pluginPath, CL loader) {
		Parameters pluginpath = new Parameters(pluginPath);

		nextClause: for (Entry<String,Attrs> entry : pluginpath.entrySet()) {

			File f = getFile(entry.getKey()).getAbsoluteFile();
			if (!f.isFile()) {

				//
				// File does not exist! Check if we need to download
				//

				String url = entry.getValue().get(PLUGINPATH_URL_ATTR);
				if (url != null) {
					try {

						trace("downloading %s to %s", url, f.getAbsoluteFile());
						URL u = new URL(url);
						URLConnection connection = u.openConnection();

						//
						// Allow the URLCOnnectionHandlers to interact with the
						// connection so they can sign it or decorate it with
						// a password etc.
						//
						for (Object plugin : instances) {
							if (plugin instanceof URLConnectionHandler) {
								URLConnectionHandler handler = (URLConnectionHandler) plugin;
								if (handler.matches(u))
									handler.handle(connection);
							}
						}

						//
						// Copy the url to the file
						//
						f.getParentFile().mkdirs();
						IO.copy(connection.getInputStream(), f);

						//
						// If there is a sha specified, we verify the download
						// of the
						// the file.
						//
						String digest = entry.getValue().get(PLUGINPATH_SHA1_ATTR);
						if (digest != null) {
							if (Hex.isHex(digest.trim())) {
								byte[] sha1 = Hex.toByteArray(digest);
								byte[] filesha1 = SHA1.digest(f).digest();
								if (!Arrays.equals(sha1, filesha1)) {
									error("Plugin path: %s, specified url %s and a sha1 but the file does not match the sha",
											entry.getKey(), url);
								}
							} else {
								error("Plugin path: %s, specified url %s and a sha1 '%s' but this is not a hexadecimal",
										entry.getKey(), url, digest);
							}
						}
					}
					catch (Exception e) {
						error("Failed to download plugin %s from %s, error %s", entry.getKey(), url, e);
						continue nextClause;
					}
				} else {
					error("No such file %s from %s and no 'url' attribute on the path so it can be downloaded",
							entry.getKey(), this);
					continue nextClause;
				}
			}
			trace("Adding %s to loader for plugins", f);
			try {
				loader.add(f.toURI().toURL());
			}
			catch (MalformedURLException e) {
				// Cannot happen since every file has a correct url
			}
		}
	}
