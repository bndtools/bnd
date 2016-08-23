---
layout: default
class: Project
title:	findname ';' PATH ( ';' FILTER )
summary: A list of filtered by name resource paths with optional replacement
---

	public String _findname(String args[]) {
		return findPath("findname", args, false);
	}

	String findPath(String name, String[] args, boolean fullPathName) {
		if (args.length > 3) {
			warning("Invalid nr of arguments to " + name + " " + Arrays.asList(args) + ", syntax: ${" + name
					+ " (; reg-expr (; replacement)? )? }");
			return null;
		}

		String regexp = ".*";
		String replace = null;

		switch (args.length) {
			case 3 :
				replace = args[2];
				//$FALL-THROUGH$
			case 2 :
				regexp = args[1];
		}
		StringBuilder sb = new StringBuilder();
		String del = "";

		Pattern expr = Pattern.compile(regexp);
		for (Iterator<String> e = dot.getResources().keySet().iterator(); e.hasNext();) {
			String path = e.next();
			if (!fullPathName) {
				int n = path.lastIndexOf('/');
				if (n >= 0) {
					path = path.substring(n + 1);
				}
			}

			Matcher m = expr.matcher(path);
			if (m.matches()) {
				if (replace != null)
					path = m.replaceAll(replace);

				sb.append(del);
				sb.append(path);
				del = ", ";
			}
		}
		return sb.toString();
	}
