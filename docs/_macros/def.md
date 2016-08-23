---
layout: default
class: Macro
title: def ';' KEY (';' STRING)?
summary: The value of the given property or a default if macro is not defined. The default is an empty string if not specified.
---
layout: default


	public String _def(String args[]) {
		if (args.length < 2)
			throw new RuntimeException("Need a value for the ${def;<value>} macro");

		if (args.length > 3)
			throw new RuntimeException("Too many args for ${def;<value>} macro");

		return domain.getProperty(args[1], args.length == 3 ? args[2] : "");
	}
