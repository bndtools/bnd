package aQute.bnd.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenVersionRange {
	static final Pattern	RESTRICTION_P	= Pattern.compile(""

			+ "\\s*("											//
			+ "("												//
			+ "(?<li>\\[|\\()\\s*"								//
			+ "(?<low>[^,\\s\\]\\[()]*)\\s*"					//
			+ ",\\s*"											//
			+ "(?<high>[^,\\s\\[\\]()]*)\\s*"					//
			+ "(?<hi>\\]|\\))"									//
			+ ")"												//
			+ "|"												//
			+ "(?<single>[^,\\s\\]\\[()]+)"						//
			+ ")\\s*"											//
			+ "(?<comma>,)?\\s*", Pattern.COMMENTS);

	final boolean			li;
	final boolean			hi;
	final MavenVersion		low;
	final MavenVersion		high;

	MavenVersionRange		nextOr;

	public MavenVersionRange(String range) {
		this(RESTRICTION_P.matcher(range == null ? "0" : range));
	}

	private MavenVersionRange(Matcher m) {
		if (!m.lookingAt())
			throw new IllegalArgumentException("Invalid version range " + m);

		String single = m.group("single");
		if (single != null) {
			li = true;
			low = new MavenVersion(single);
			high = MavenVersion.HIGHEST;
			hi = true;
		} else {
			li = m.group("li").equals("[");
			hi = m.group("hi").equals("]");

			low = MavenVersion.parseMavenString(m.group("low"));
			high = MavenVersion.parseMavenString(m.group("high"));
		}

		if (m.group("comma") != null) {
			m.region(m.end(), m.regionEnd());
			nextOr = new MavenVersionRange(m);
		} else
			nextOr = null;
	}

	public boolean includes(MavenVersion mvr) {
		int l = mvr.compareTo(low);
		int h = mvr.compareTo(high);

		boolean lowOk = l > 0 || (li && l == 0);
		boolean highOk = h < 0 || (hi && h == 0);

		if (lowOk && highOk)
			return true;

		if (nextOr != null)
			return nextOr.includes(mvr);

		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	private void toString(StringBuilder sb) {
		if (li)
			sb.append("[");
		else
			sb.append("(");

		sb.append(low);
		sb.append(",");
		sb.append(high);
		if (hi)
			sb.append("]");
		else
			sb.append(")");

		if (nextOr != null) {
			sb.append(",");
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
		return (li && !hi && high == MavenVersion.HIGHEST && nextOr == null);
	}

	public static boolean isRange(String version) {
		if (version == null)
			return false;

		version = version.trim();
		return version.startsWith("[") || version.startsWith("(");
	}
}
