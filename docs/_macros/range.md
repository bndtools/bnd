---
layout: default
class: Macro
title: range ';' RANGE_MASK ( ';' VERSION )
summary: Create a semantic version range out of a version using a mask to control the bump of the ceiling
---

The `range` macro takes a version range mask/template and uses it calculate a range from a given version. The primary reason for the `${range}` macro is to be used in the [version policy][1]. With the version policy we have a version of an exported package and we need to calculate the range for that. The rules come from the [consumer][2] or [provider][3] policy. However, this policy can be overridden on the Import-Package header by specifying the version as a range macro:

	Import-Package: com.example.myownpolicy; version="${range;[==,=+)}", *
<foo ]>

Since the version for the exported package is set as `${@}`, the macro will calculate the proper semantic range for a provider.

The syntax for the `range` macro is:

	range ::= ( '\[' |'\( ) mask ',' mask ( '\)' | '\]' )
	mask  ::= m ( m ( m )? )? q
	m     ::= [0-9=+-~]
	q     ::= [0-9=~Ss]

The meaning of the characters is:

* `=` – Keep the version part
* `-` – Decrement the version part
* `+` – Increment the version part
* `[0-9]` – Replace the version part
* `~` – Ignore the version part
* `[Ss]` – If the qualifier equals `SNAPSHOT`, then it will return a maven like snapshot version. Maven snapshot versions do not use the `.` as the separator but a `-` sign. The upper case `S` checks case sensitive, the lower case `s` is case insensitive. This template character will be treated as the last character in the template and return the version immediately. For example, `${versionmask;=S;1.2.3.SNAPSHOT}` will return `1-SNAPSHOT`.  


[1]: /chapters/170-versioning.html
[2]: /instructions/consumer_policy.html
[3]: /instructions/provider_policy.html

	/**
	 * Schortcut for version policy
	 * 
	 * <pre>
	 * -provide-policy : ${policy;[==,=+)}
	 * -consume-policy : ${policy;[==,+)}
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */

	static Pattern	RANGE_MASK		= Pattern.compile("(\\[|\\()(" + MASK_STRING + "),(" + MASK_STRING + ")(\\]|\\))");
	static String	_rangeHelp		= "${range;<mask>[;<version>]}, range for version, if version not specified lookup ${@}\n"
											+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n"
											+ "M ::= '+' | '-' | MQ\n"
											+ "MQ ::= '~' | '='";
	static Pattern	_rangePattern[]	= new Pattern[] {
			null, RANGE_MASK
									};

	public String _range(String args[]) {
		verifyCommand(args, _rangeHelp, _rangePattern, 2, 3);
		Version version = null;
		if (args.length >= 3)
			version = new Version(args[2]);
		else {
			String v = domain.getProperty("@");
			if (v == null)
				return null;
			version = new Version(v);
		}
		String spec = args[1];

		Matcher m = RANGE_MASK.matcher(spec);
		m.matches();
		String floor = m.group(1);
		String floorMask = m.group(2);
		String ceilingMask = m.group(3);
		String ceiling = m.group(4);

		String left = version(version, floorMask);
		String right = version(version, ceilingMask);
		StringBuilder sb = new StringBuilder();
		sb.append(floor);
		sb.append(left);
		sb.append(",");
		sb.append(right);
		sb.append(ceiling);

		String s = sb.toString();
		VersionRange vr = new VersionRange(s);
		if (!(vr.includes(vr.getHigh()) || vr.includes(vr.getLow()))) {
			domain.error("${range} macro created an invalid range %s from %s and mask %s", s, version, spec);
		}
		return sb.toString();
	}

