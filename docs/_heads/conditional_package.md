---
layout: default
class: Header
title: Conditional-Package  PACKAGE-SPEC (',' PACKAGE-SPEC) *
summary: Recursively add packages from the class path when referred and when they match one of the package specifications.
---

	/**
	 * Answer extra packages. In this case we implement conditional package. Any
	 */
	@Override
	protected Jar getExtra() throws Exception {
		Parameters conditionals = getParameters(CONDITIONAL_PACKAGE);
		conditionals.putAll(getParameters(CONDITIONALPACKAGE));
		if (conditionals.isEmpty())
			return null;
		trace("do Conditional Package %s", conditionals);
		Instructions instructions = new Instructions(conditionals);

		Collection<PackageRef> referred = instructions.select(getReferred().keySet(), false);
		referred.removeAll(getContained().keySet());

		Jar jar = new Jar("conditional-import");
		addClose(jar);
		for (PackageRef pref : referred) {
			for (Jar cpe : getClasspath()) {
				Map<String,Resource> map = cpe.getDirectories().get(pref.getPath());
				if (map != null) {
					copy(jar, cpe, pref.getPath(), false);
					// Now use copy so that bnd.info is processed, next line
					// should be
					// removed in the future TODO
					// jar.addDirectory(map, false);
					break;
				}
			}
		}
		if (jar.getDirectories().size() == 0) {
			trace("extra dirs %s", jar.getDirectories());
			return null;
		}
		return jar;
	}
