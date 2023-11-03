package bndtools.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a label-string into various parts.
 */
public class LabelParser {

	/**
	 * <p>
	 * Regex for parsing the compoundLabelExpression in the constructor:
	 * </p>
	 * <ol>
	 * <li>
	 * <p>
	 * <strong>Group 1 <code>(-)?</code></strong>: Represents an optional minus
	 * sign <code>-</code>.
	 * </p>
	 * <ul>
	 * <li>This is used to determine if something is <code>enabled</code>. If
	 * this group is present, then <code>enabled</code> is set to
	 * <code>false</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 2 <code>(!)?</code></strong>: Represents an optional
	 * exclamation mark <code>!</code>.
	 * </p>
	 * <ul>
	 * <li>This is used to determine if something is <code>checked</code>. If
	 * this group is present, then <code>checked</code> is set to
	 * <code>true</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 3 <code>([^{}]+)</code></strong>: Matches one or more
	 * characters that are not curly braces <code>{</code> or <code>}</code>.
	 * </p>
	 * <ul>
	 * <li>This captures the actual <code>labeltext</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 4 <code>(?:\\{([^}]+)\\})?</code></strong>: This is an
	 * optional group that captures text inside curly braces.
	 * </p>
	 * <ul>
	 * <li><code>(?: ... )</code> is a non-capturing group.</li>
	 * <li><code>\\{([^}]+)\\}</code> captures characters between <code>{</code>
	 * and <code>}</code>.</li>
	 * <li>This captures the <code>description</code>.</li>
	 * </ul>
	 * </li> See Constructor for Example Strings
	 * </ol>
	 * </p>
	 */
	final static Pattern	LABEL_PATTERN	= Pattern.compile("(-)?(!)?([^{}]+)(?:\\{([^}]+)\\})?");

	String					label;
	boolean					enabled			= true;
	boolean					checked			= false;
	String					description		= null;

	/**
	 * @param labelExpression a string consisting of multiple parts which is
	 *            parse by a regex into 4 parts. E.g.
	 *            <h3 id="example-strings-">Example Strings:</h3>
	 *            <ol>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;-!MyLabel{This is a description}&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: <code>-</code> =&gt; <code>enabled</code> is
	 *            <code>false</code>.</li>
	 *            <li>Group 2: <code>!</code> =&gt; <code>checked</code> is
	 *            <code>true</code>.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: <code>This is a description</code> =&gt;
	 *            Description is &quot;This is a description&quot;.</li>
	 *            </ul>
	 *            </li>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;!MyLabel&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: Not present =&gt; <code>enabled</code> is not
	 *            affected (remains whatever value it previously had).</li>
	 *            <li>Group 2: <code>!</code> =&gt; <code>checked</code> is
	 *            <code>true</code>.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: Not present =&gt; Description is
	 *            <code>null</code>.</li>
	 *            </ul>
	 *            </li>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;MyLabel{Description here}&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: Not present =&gt; <code>enabled</code> is not
	 *            affected.</li>
	 *            <li>Group 2: Not present =&gt; <code>checked</code> is not
	 *            affected.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: <code>Description here</code> =&gt; Description
	 *            is &quot;Description here&quot;.</li>
	 *            </ul>
	 *            </li>
	 *            </ol>
	 *            <p>
	 *            These example strings illustrate how the regex pattern breaks
	 *            down and captures each part of the input string.
	 *            </p>
	 */
	public LabelParser(String labelExpression) {

		Matcher m = LABEL_PATTERN.matcher(labelExpression);
		if (m.matches()) {
			if (m.group(1) != null)
				enabled = false;

			if (m.group(2) != null)
				checked = true;

			label = m.group(3);

			description = m.group(4);
		}
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isChecked() {
		return checked;
	}

}
