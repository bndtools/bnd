---
layout: default
class: Workspace
title: gestalt ';' NAME ( ';' NAME (';' ANY )? )? 
summary: provides access to the gestalt properties that describe the environment.
---

	/**
	 * Add a gestalt to all workspaces. The gestalt is a set of parts describing
	 * the environment. Each part has a name and optionally attributes. This
	 * method adds a gestalt to the VM. Per workspace it is possible to augment
	 * this.
	 */

	public static void addGestalt(String part, Attrs attrs) {
		Attrs already = overallGestalt.get(part);
		if (attrs == null)
			attrs = new Attrs();

		if (already != null) {
			already.putAll(attrs);
		} else
			already = attrs;

		overallGestalt.put(part, already);
	}

	/**
	 * Get the attrs for a gestalt part
	 */
	public Attrs getGestalt(String part) {
		if (gestalt == null) {
			gestalt = new Parameters(getProperty(Constants.GESTALT));
			gestalt.mergeWith(overallGestalt, false);
		}
		return gestalt.get(part);
	}

	/**
	 * The macro to access the gestalt
	 * <p>
	 * {@code $ gestalt;part[;key[;value]]}}
	 */

	public String _gestalt(String args[]) {
		if (args.length >= 2) {
			Attrs attrs = getGestalt(args[1]);
			if (attrs == null)
				return "";

			if (args.length == 2)
				return args[1];

			String s = attrs.get(args[2]);
			if (args.length == 3) {
				if (s == null)
					s = "";
				return s;
			}

			if (args.length == 4) {
				if (args[3].equals(s))
					return s;
				else
					return "";
			}
		}
		throw new IllegalArgumentException("${gestalt;<part>[;key[;<value>]]} has too many arguments");
	}
