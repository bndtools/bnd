package aQute.bnd.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenVersionRange {
	private static final Pattern	RESTRICTION_P	= Pattern.compile(""	//
		+ "\\s*("															//
		+ "(?<pair>"														//
		+ "(?<li>\\[|\\()\\s*"												//
		+ "(?<low>[^,\\s\\]\\[()]*)\\s*"									//
		+ ",\\s*"															//
		+ "(?<high>[^,\\s\\[\\]()]*)\\s*"									//
		+ "(?<hi>\\]|\\))"													//
		+ ")"																//
		+ "|"																//
		+ "(?<single>[^,\\s\\]\\[()]+)"										//
		+ "|"																//
		+ "(\\["															//
		+ "(?<exact>[^,\\s\\]\\[()]+)"										//
		+ "\\])"															//
		+ ")\\s*"															//
		+ "(?<comma>,)?"													//
		, Pattern.COMMENTS);

	private final boolean			pair;
	private final boolean			li;
	private final boolean			hi;
	private final MavenVersion		low;
	private final MavenVersion		high;
	private final MavenVersionRange	nextOr;

	public MavenVersionRange(String range) {
		this(RESTRICTION_P.matcher(range == null ? "0" : range));
	}

	private MavenVersionRange(Matcher m) {
		if (!m.lookingAt())
			throw new IllegalArgumentException("Invalid version range " + m);

		pair = m.group("pair") != null;
		if (pair) {
			li = m.group("li")
				.equals("[");
			hi = m.group("hi")
				.equals("]");

			String v = m.group("low")
				.trim();
			if (v.isEmpty()) {
				low = MavenVersion.LOWEST;
			} else {
				low = MavenVersion.parseMavenString(v);
			}

			v = m.group("high")
				.trim();
			if (v.isEmpty()) {
				high = MavenVersion.HIGHEST;
			} else {
				high = MavenVersion.parseMavenString(v);
			}
		} else {
			String single = m.group("single");
			if (single != null) {
				li = hi = true;
				low = new MavenVersion(single);
				high = MavenVersion.HIGHEST;
			} else {
				String exact = m.group("exact");
				li = hi = true;
				low = high = new MavenVersion(exact);
			}
		}

		if (m.group("comma") != null) {
			m.region(m.end(), m.regionEnd());
			nextOr = new MavenVersionRange(m);
		} else {
			nextOr = null;
		}
	}

	public boolean includes(MavenVersion mvr) {
		int l = mvr.compareTo(low);
		int h = (high == MavenVersion.HIGHEST) ? -1 : mvr.compareTo(high);

		boolean lowOk = l > 0 || (li && l == 0);
		boolean highOk = h < 0 || (hi && h == 0);

		if (lowOk && highOk)
			return true;

		if (nextOr != null)
			return nextOr.includes(mvr);

		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	private void toString(StringBuilder sb) {
		if (pair) {
			sb.append(li ? '[' : '(');
			if (low != MavenVersion.LOWEST) {
				sb.append(low);
			}
			sb.append(',');
			if (high != MavenVersion.HIGHEST) {
				sb.append(high);
			}
			sb.append(hi ? ']' : ')');
		} else if (low == high) { // exact
			sb.append('[')
				.append(low)
				.append(']');
		} else { // single
			sb.append(low);
		}

		if (nextOr != null) {
			sb.append(',');
			nextOr.toString(sb);
		}
	}

	public static MavenVersionRange parseRange(String version) {
		try {
			return new MavenVersionRange(version);
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public boolean wasSingle() {
		return !pair && (high == MavenVersion.HIGHEST) && (nextOr == null);
	}

	public static boolean isRange(String version) {
		if (version == null)
			return false;

		version = version.trim();
		return version.startsWith("[") || version.startsWith("(");
	}
}
