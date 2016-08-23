---
layout: default
class: Builder
title: -sub FILE-SPEC ( ',' FILE-SPEC )*
summary:  Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.
---

	/**
	 * Answer a list of builders that represent this file or a list of files
	 * specified in -sub. This list can be empty. These builders represents to
	 * be created artifacts and are each scoped to such an artifacts. The
	 * builders can be used to build the bundles or they can be used to find out
	 * information about the to be generated bundles.
	 *
	 * @return List of 0..n builders representing artifacts.
	 * @throws Exception
	 */
	public List<Builder> getSubBuilders() throws Exception {
		String sub = getProperty(SUB);
		if (sub == null || sub.trim().length() == 0 || EMPTY_HEADER.equals(sub))
			return Arrays.asList(this);

		List<Builder> builders = new ArrayList<Builder>();
		if (isTrue(getProperty(NOBUNDLES)))
			return builders;

		Parameters subsMap = parseHeader(sub);
		for (Iterator<String> i = subsMap.keySet().iterator(); i.hasNext();) {
			File file = getFile(i.next());
			if (file.isFile() && !file.getName().startsWith(".")) {
				builders.add(getSubBuilder(file));
				i.remove();
			}
		}

		Instructions instructions = new Instructions(subsMap);

		List<File> members = new ArrayList<File>(Arrays.asList(getBase().listFiles()));

		nextFile: while (members.size() > 0) {

			File file = members.remove(0);

			// Check if the file is one of our parents
			@SuppressWarnings("resource")
			Processor p = this;
			while (p != null) {
				if (file.equals(p.getPropertiesFile()))
					continue nextFile;
				p = p.getParent();
			}

			for (Iterator<Instruction> i = instructions.keySet().iterator(); i.hasNext();) {

				Instruction instruction = i.next();
				if (instruction.matches(file.getName())) {

					if (!instruction.isNegated()) {
						builders.add(getSubBuilder(file));
					}

					// Because we matched (even though we could be negated)
					// we skip any remaining searches
					continue nextFile;
				}
			}
		}
		return builders;
	}

	public Builder getSubBuilder(File file) throws Exception {
		Builder builder = getSubBuilder();
		if (builder != null) {
			builder.setProperties(file);
			addClose(builder);
		}
		return builder;
	}

	public Builder getSubBuilder() throws Exception {
		Builder builder = new Builder(this);
		builder.setBase(getBase());

		for (Jar file : getClasspath()) {
			builder.addClasspath(file);
		}

		return builder;
	}
