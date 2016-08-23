---
layout: default
class: Analyzer
title: frange ';' VERSION ( ';' BOOLEAN )?
summary: a range expression for a filter from a version. By default this is based on consumer compatibility. You can specify a third argument (true) to get provider compatibility.
---


	/**
	 * Return 
	 * 
	 * <pre>
	 * ${frange;1.2.3}             -> (&(version>=1.2.3)(!(version>=1.3.0))
	 * ${frange;1.2.3, true}       -> (&(version>=1.2.3)(!(version>=2.0.0))
	 * </pre>
	 */
	public String _frange(String[] args) {
		if (args.length < 2 || args.length > 3) {
			error("Invalid filter range, 2 or 3 args ${frange;<version>[;true|false]}");
			return null;
		}

		String v = args[1];
		if (!Verifier.isVersion(v)) {
			error("Invalid version arg %s", v);
			return null;
		}

		boolean isProvider = false;
		if (args.length == 3)
			isProvider = Processor.isTrue(args[2]);

		Version low = new Version(v);
		Version high;
		if (isProvider)
			high = new Version(low.getMajor(), low.getMinor() + 1, 0);
		else
			high = new Version(low.getMajor() + 1, 0, 0);

		StringBuilder sb = new StringBuilder("(&(version>=").append(low.getWithoutQualifier()).append(")");
		sb.append("(!(version>=").append(high.getWithoutQualifier()).append(")))");

		return sb.toString();
	}
