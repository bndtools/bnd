---
layout: default
class: Builder
title: -namesection RESOURCE-SPEC ( ',' RESOURCE-SPEC ) *   
summary:  Create a name section (second part of manifest) with optional property expansion and addition of custom attributes. 
---

	/**
	 * Parse the namesection as instructions and then match them against the
	 * current set of resources For example:
	 * 
	 * <pre>
	 * 	-namesection: *;baz=true, abc/def/bar/X.class=3
	 * </pre>
	 * 
	 * The raw value of {@link Constants#NAMESECTION} is used but the values of
	 * the attributes are replaced where @ is set to the resource name. This
	 * allows macro to operate on the resource
	 */

	private void doNamesection(Jar dot, Manifest manifest) {

		Parameters namesection = parseHeader(getProperties().getProperty(NAMESECTION));
		Instructions instructions = new Instructions(namesection);
		Set<String> resources = new HashSet<String>(dot.getResources().keySet());

		//
		// For each instruction, iterator over the resources and filter
		// them. If a resource matches, it must be removed even if the
		// instruction is negative. If positive, add a name section
		// to the manifest for the given resource name. Then add all
		// attributes from the instruction to that name section.
		//
		for (Map.Entry<Instruction,Attrs> instr : instructions.entrySet()) {
			boolean matched = false;

			// For each instruction

			for (Iterator<String> i = resources.iterator(); i.hasNext();) {
				String path = i.next();
				// For each resource

				if (instr.getKey().matches(path)) {

					// Instruction matches the resource

					matched = true;
					if (!instr.getKey().isNegated()) {

						// Positive match, add the attributes

						Attributes attrs = manifest.getAttributes(path);
						if (attrs == null) {
							attrs = new Attributes();
							manifest.getEntries().put(path, attrs);
						}

						//
						// Add all the properties from the instruction to the
						// name section
						//

						for (Map.Entry<String,String> property : instr.getValue().entrySet()) {
							setProperty("@", path);
							try {
								String processed = getReplacer().process(property.getValue());
								attrs.putValue(property.getKey(), processed);
							}
							finally {
								unsetProperty("@");
							}
						}
					}
					i.remove();
				}
			}

			if (!matched && resources.size() > 0)
				warning("The instruction %s in %s did not match any resources", instr.getKey(), NAMESECTION);
		}

	}
