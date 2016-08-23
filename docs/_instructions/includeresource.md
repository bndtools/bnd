---
layout: default
class: Project
title: -includeresource  
summary:  Include resources from the file system
---




	/**
	 * Parse the Bundle-Includes header. Files in the bundles Include header are
	 * included in the jar. The source can be a directory or a file.
	 *
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void doIncludeResources(Jar jar) throws Exception {
		String includes = getProperty("Bundle-Includes");
		if (includes == null) {
			includes = getProperty(INCLUDERESOURCE);
			if (includes == null || includes.length() == 0)
				includes = getProperty(Constants.INCLUDE_RESOURCE);
		} else
			warning("Please use -includeresource instead of Bundle-Includes");

		doIncludeResource(jar, includes);

	}

	private void doIncludeResource(Jar jar, String includes) throws Exception {
		Parameters clauses = parseHeader(includes);
		doIncludeResource(jar, clauses);
	}

	private void doIncludeResource(Jar jar, Parameters clauses) throws ZipException, IOException, Exception {
		for (Entry<String,Attrs> entry : clauses.entrySet()) {
			doIncludeResource(jar, entry.getKey(), entry.getValue());
		}
	}

	private void doIncludeResource(Jar jar, String name, Map<String,String> extra) throws ZipException, IOException,
			Exception {

		Instructions preprocess = null;
		boolean absentIsOk = false;

		if (name.startsWith("{") && name.endsWith("}")) {
			preprocess = getPreProcessMatcher(extra);
			name = name.substring(1, name.length() - 1).trim();
		}

		String parts[] = name.split("\\s*=\\s*");
		String source = parts[0];
		String destination = parts[0];
		if (parts.length == 2)
			source = parts[1];

		if (source.startsWith("-")) {
			source = source.substring(1);
			absentIsOk = true;
		}

		if (source.startsWith("@")) {
			extractFromJar(jar, source.substring(1), parts.length == 1 ? "" : destination, absentIsOk);
		} else if (extra.containsKey("cmd")) {
			doCommand(jar, source, destination, extra, preprocess, absentIsOk);
		} else if (extra.containsKey(LITERAL_ATTRIBUTE)) {
			String literal = extra.get(LITERAL_ATTRIBUTE);
			Resource r = new EmbeddedResource(literal.getBytes("UTF-8"), 0);
			String x = extra.get("extra");
			if (x != null)
				r.setExtra(x);
			jar.putResource(name, r);
		} else {
			File sourceFile;
			String destinationPath;

			sourceFile = getFile(source);
			if (parts.length == 1) {
				// Directories should be copied to the root
				// but files to their file name ...
				if (sourceFile.isDirectory())
					destinationPath = "";
				else
					destinationPath = sourceFile.getName();
			} else {
				destinationPath = parts[0];
			}
			// Handle directories
			if (sourceFile.isDirectory()) {
				destinationPath = doResourceDirectory(jar, extra, preprocess, sourceFile, destinationPath);
				return;
			}

			// destinationPath = checkDestinationPath(destinationPath);

			if (!sourceFile.exists()) {
				if (absentIsOk)
					return;

				noSuchFile(jar, name, extra, source, destinationPath);
			} else
				copy(jar, destinationPath, sourceFile, preprocess, extra);
		}
	}

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

	/**
	 * It is possible in Include-Resource to use a system command that generates
	 * the contents, this is indicated with {@code cmd} attribute. The command
	 * can be repeated for a number of source files with the {@code for}
	 * attribute which indicates a list of repetitions, often down with the
	 * {@link Macro#_lsa(String[])} or {@link Macro#_lsb(String[])} macro. The
	 * repetition will repeat the given command for each item. The @} macro can
	 * be used to replace the current item. If no {@code for} is given, the
	 * source is used as the only item. If the destination contains a macro,
	 * each iteration will create a new file, otherwise the destination name is
	 * used.
	 *
	 * @param jar
	 * @param source
	 * @param destination
	 * @param extra
	 * @param preprocess
	 * @param absentIsOk
	 * @throws Exception
	 */
	private void doCommand(Jar jar, String source, String destination, Map<String,String> extra,
			Instructions preprocess, boolean absentIsOk) throws Exception {
		String repeat = extra.get("for"); // TODO constant
		if (repeat == null)
			repeat = source;

		Collection<String> requires = split(extra.get("requires"));
		long lastModified = 0;
		for (String required : requires) {
			File file = getFile(required);
			if (!file.exists()) {
				error(Constants.INCLUDE_RESOURCE + ".cmd for %s, requires %s, but no such file %s", source, required,
						file.getAbsoluteFile());
			} else
				lastModified = findLastModifiedWhileOlder(file, lastModified());
		}

		String cmd = extra.get("cmd");

		List<String> paths = new ArrayList<String>();

		for (String item : Processor.split(repeat)) {
			File f = IO.getFile(item);
			traverse(paths, f);
		}

		CombinedResource cr = null;

		if (!destination.contains("${@}")) {
			cr = new CombinedResource();
			cr.lastModified = lastModified;
		}

		setProperty("@requires", join(requires, " "));
		try {
			for (String item : paths) {
				setProperty("@", item);
				try {
					String path = getReplacer().process(destination);
					String command = getReplacer().process(cmd);
					File file = getFile(item);
					if (file.exists())
						lastModified = Math.max(lastModified, file.lastModified());

					CommandResource cmdresource = new CommandResource(command, this, lastModified, getBase());

					Resource r = cmdresource;

					// Turn this resource into a file resource
					// so we execute the command now and catch its
					// errors
					FileResource fr = new FileResource(r);

					addClose(fr);
					r = fr;

					if (preprocess != null && preprocess.matches(path))
						r = new PreprocessResource(this, r);

					if (cr == null)
						jar.putResource(path, r);
					else
						cr.addResource(r);
				}
				finally {
					unsetProperty("@");
				}
			}
		}
		finally {
			unsetProperty("@requires");
		}

		// Add last so the correct modification date is used
		// to update the modified time.
		if (cr != null)
			jar.putResource(destination, cr);

		updateModified(lastModified, Constants.INCLUDE_RESOURCE + ": cmd");
	}

	private void traverse(List<String> paths, File item) {

		if (item.isDirectory()) {
			for (File sub : item.listFiles()) {
				traverse(paths, sub);
			}
		} else if (item.isFile())
			paths.add(item.getAbsolutePath());
		else
			paths.add(item.getName());
	}




		public class MakeBnd implements MakePlugin, Constants {
			final static Pattern	JARFILE	= Pattern.compile("(.+)\\.(jar|ipa)");
		
			public Resource make(Builder builder, String destination, Map<String,String> argumentsOnMake) throws Exception {
				String type = argumentsOnMake.get("type");
				if (!"bnd".equals(type))
					return null;
		
				String recipe = argumentsOnMake.get("recipe");
				if (recipe == null) {
					builder.error("No recipe specified on a make instruction for " + destination);
					return null;
				}
				File bndfile = builder.getFile(recipe);
				if (bndfile.isFile()) {
					// We do not use a parent because then we would
					// build ourselves again. So we can not blindly
					// inherit the properties.
					Builder bchild = builder.getSubBuilder();
					bchild.removeBundleSpecificHeaders();
		
					// We must make sure that we do not include ourselves again!
					bchild.setProperty(Analyzer.INCLUDE_RESOURCE, "");
					bchild.setProperty(Analyzer.INCLUDERESOURCE, "");
					bchild.setProperties(bndfile, builder.getBase());
		
					Jar jar = bchild.build();
					Jar dot = builder.getTarget();
		
					if (builder.hasSources()) {
						for (String key : jar.getResources().keySet()) {
							if (key.startsWith("OSGI-OPT/src"))
								dot.putResource(key, jar.getResource(key));
						}
					}
					builder.getInfo(bchild, bndfile.getName() + ": ");
					return new JarResource(jar);
				}
				return null;
			}
		
		}
