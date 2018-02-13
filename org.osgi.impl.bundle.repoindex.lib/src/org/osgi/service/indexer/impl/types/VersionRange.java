/*
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.indexer.impl.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

public class VersionRange implements Comparable<VersionRange> {

	final Version	high;
	final Version	low;

	char			start	= '[';
	char			end		= ']';

	static String	V		= "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[a-zA-Z0-9_-]+)?)?)?";
	static Pattern	RANGE	= Pattern.compile("(\\(|\\[)\\s*(" + V + ")\\s*,\\s*(" + V + ")\\s*(\\)|\\])");

	public VersionRange(boolean lowInclusive, Version low, Version high, boolean highInclusive) {
		if (low.compareTo(high) > 0)
			throw new IllegalArgumentException("Low Range is higher than High Range: " + low + "-" + high);

		this.low = low;
		this.high = high;
		this.start = lowInclusive ? '[' : '(';
		this.end = highInclusive ? ']' : ')';
	}

	public VersionRange(String string) {
		string = string.trim();
		Matcher m = RANGE.matcher(string);
		if (m.matches()) {
			start = m.group(1).charAt(0);
			low = new Version(m.group(2));
			high = new Version(m.group(6));
			end = m.group(10).charAt(0);
			if (low.compareTo(high) > 0)
				throw new IllegalArgumentException("Low Range is higher than High Range: " + low + "-" + high);

		} else {
			start = '[';
			high = low = new Version(string);
			end = ']';
		}
	}

	public boolean isRange() {
		return high != low;
	}

	public boolean includeLow() {
		return start == '[';
	}

	public boolean includeHigh() {
		return end == ']';
	}

	public String toString() {
		if (high == low)
			return high.toString();

		StringBuilder sb = new StringBuilder();
		sb.append(start);
		sb.append(low);
		sb.append(',');
		sb.append(high);
		sb.append(end);
		return sb.toString();
	}

	public boolean equals(VersionRange other) {
		return compareTo(other) == 0;
	}

	public int hashCode() {
		return low.hashCode() * high.hashCode();
	}

	public int compareTo(VersionRange range) {
		VersionRange a = this, b = range;
		if (range.isRange()) {
			a = range;
			b = this;
		} else {
			if (!isRange())
				return low.compareTo(range.high);
		}
		int l = a.low.compareTo(b.low);
		boolean ll = false;
		if (a.includeLow())
			ll = l <= 0;
		else
			ll = l < 0;

		if (!ll)
			return -1;

		int h = a.high.compareTo(b.high);
		if (a.includeHigh())
			ll = h >= 0;
		else
			ll = h > 0;

		if (ll)
			return 0;
		else
			return 1;
	}

	public Version getHigh() {
		return high;
	}

	public Version getLow() {
		return low;
	}

	public boolean match(Version version) {
		int lowmatch = version.compareTo(low);
		if (lowmatch < 0)
			return false;
		if (lowmatch == 0 && !includeLow())
			return false;

		int highmatch = version.compareTo(high);
		if (highmatch > 0)
			return false;
		if (highmatch == 0 && !includeHigh())
			return false;

		return true;
	}
}