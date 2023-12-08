package aQute.libg.re;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;

import aQute.libg.re.RE.C;
import aQute.libg.re.RE.F;
import aQute.libg.re.RE.F.Flag;
import aQute.libg.re.RE.G;
import aQute.libg.re.RE.Q;

/**
 * This class provides an implementation of the RE types. The class is useful as
 * static imports. (For Eclipse users, look at favorites in the preferences.)
 * However, it can also be used as base class. If that is the case, field names
 * can be used as named capture groups. In this constellation, the static
 * methods are also in scope, not requiring many static imports.
 *
 * <pre>
 * void foo() {
 * 	class X extends Catalog {
 * 		RE match = lit("abc");
 * 		RE namedMatch = named(match);
 * 	}
 * 	X x;
 *  x.....
 * }
 * </pre>
 */
public class Catalog {

	/**
	 * If this class is extended, the named fields in that class can be used in
	 * named groups. This method will lookup the name of a field and create a
	 * capturing group with this name. It finds the field by comparing the
	 * content.
	 *
	 * @param re the RE that should be in a field in this class.
	 * @return a group RE
	 */
	public RE named(RE re) {
		String name = findFieldWith(re);
		assert name != null;
		return g(name, re);
	}

	String findFieldWith(RE re) {
		Class<?> c = getClass();
		for (Field f : c.getDeclaredFields())
			try {
				f.setAccessible(true);
				if (f.get(this) == re)
					return f.getName();
			} catch (Exception e) {
				// ignore
			}
		return null;
	}

	/**
	 * Return a control char. For example, `control('b') returns ^b. See the
	 * sequence `\\cb`.
	 *
	 * @param c the control character
	 * @return an RE representing the control character
	 */
	public static RE control(char c) {
		return new REImpl("\\c" + c);
	}

	/**
	 * Create a non capturing group
	 *
	 * @param res the members
	 * @return a non capturing group
	 */
	public static RE g(RE... res) {
		return new Group(Group.Type.NONCAPTURING, res);
	}

	/**
	 * Create an OR combination of a number of RE's
	 *
	 * @param res the set of RE's that are the members of the OR
	 * @return the RE representing the OR
	 */
	public static RE or(RE... res) {

		assert res != null;

		return switch (res.length) {
			case 0 -> empty;
			case 1 -> res[0];
			default -> {
				StringBuilder sb = new StringBuilder();
				String del = "";
				for (RE re : res) {
					sb.append(del)
						.append(re);
					del = "|";
				}
				yield new Group(Group.Type.NONCAPTURING, sb.toString());
			}
		};
	}

	/**
	 * Create an OR combination of a number of Strings. The strings are
	 * converted with {@link #lit(String)}.
	 *
	 * @param res the strings
	 * @return the RE representing the OR
	 */
	public static RE or(String... res) {
		assert res != null;
		return or(Stream.of(res)
			.map(Catalog::lit)
			.toArray(RE[]::new));
	}

	/**
	 * Create an or combination of character classes.
	 *
	 * @param res the character classes
	 * @return an RE representing the combined clases
	 */
	public static RE or(C... res) {
		assert res != null;
		return switch (res.length) {
			case 0 -> empty;
			case 1 -> res[0];
			default -> {
				StringBuilder sb = new StringBuilder();
				for (C re : res) {
					sb.append(re.asSetContent());
				}
				yield new CharacterClass(sb.toString());
			}
		};
	}

	/**
	 * Create a named capturing group
	 *
	 * @param name the name of the group. This must be a valid Java identifier
	 * @param res the members.
	 * @return a new named capture group
	 */
	public static RE g(String name, RE... res) {
		assert isValidGroupName(name);
		if (res == null || res.length == 0)
			return empty;

		return new Group(name, res);
	}

	private static boolean isValidGroupName(String name) {
		return name == null || javaId.matches(name)
			.isPresent();
	}

	/**
	 * Return a named group but where each member that is not a whitespace, will
	 * be preceded with a #setWs.
	 *
	 * @param name the name of the group or null for a non-named group
	 * @param res the members
	 * @return a group, either named or capturing
	 */
	public static RE term(@Nullable
	String name, RE... res) {
		assert isValidGroupName(name) : name;
		if (res == null || res.length == 0)
			return empty;

		List<RE> out = new ArrayList<>();

		RE last = setWs;
		out.add(last);
		boolean lastWs = true;

		for (int i = 0; i < res.length; i++) {
			RE next = res[i];
			boolean nextWs = isWhiteSpace(next);

			int n = 0;
			if (lastWs)
				n = 1;
			if (nextWs)
				n += 2;

			switch (n) {
				case 0 -> {
					out.add(setWs);
					out.add(next);
				}
				case 1, 2 -> {
					out.add(next);
				}
				case 3 -> {
				}
			}
			last = next;
			lastWs = nextWs;
		}
		return new Group(name, out.toArray(RE[]::new));
	}

	/**
	 * See {@link #term(String,RE...)} with a null for name
	 *
	 * @param res the members
	 * @return a new
	 */
	public static RE term(RE... res) {
		return term(null, res);
	}

	/**
	 * Create a character class. I.e. `[abc]`. Do not include the ^ to negate
	 * the set, use the not() method.
	 *
	 * @param allowed the allowed characters
	 * @return a character class.
	 */

	public static C cc(String allowed) {
		return new CharacterClass(allowed, true, null);
	}

	/**
	 * Provide a literal text. This lit can contain characters that normally
	 * have a special meaning. All characters that have a special meaning are
	 * escaped with the backslash ('\').
	 *
	 * @param s the literal string
	 * @return an RE
	 */
	public static RE lit(String s) {
		StringBuilder sb = null;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("()$\\{}[]^+*?.| ".indexOf(c) >= 0) {
				if (sb == null) {
					sb = new StringBuilder();
					sb.append(s, 0, i);
				}
				sb.append("\\")
					.append(c);
			} else if (sb != null)
				sb.append(c);
		}
		return new REImpl(sb == null ? s : sb.toString());
	}

	/**
	 * Useful if you need a number of literal REs
	 *
	 * @param s the strings
	 * @return an array of RE
	 */
	public static RE[] lit(String... s) {
		return Stream.of(s)
			.map(ss -> lit(ss))
			.toArray(RE[]::new);
	}

	/**
	 * Use the quoting facility built into {@link Pattern#quote(String)}
	 *
	 * @param s the string
	 * @return the quoted string
	 */
	public static RE quote(String s) {
		return new REImpl(Pattern.quote(s));
	}

	/**
	 * Use the Unicode name. Is \\N
	 *
	 * @param name the unicode name.
	 * @return the RE representing the unicode name.
	 */
	public static RE unicode(String name) {
		return new REImpl("\\N{".concat(name)
			.concat("}"));
	}

	/**
	 * Used to reference a previous capturing group. Unfortunately this cannot
	 * be done by name. This class will by default create non-capturing groups,
	 * so only explicit groups need to be counted.
	 *
	 * @param group the group number
	 * @return a new RE referencing a previous group
	 */
	public static RE back(int group) {
		assert group < 10 && group > 0;
		return new REImpl("\\" + group);
	}

	/**
	 * Used to reference a previous named capturing group.
	 *
	 * @param group the group name
	 * @return a new RE referencing a previous group
	 */
	public static RE back(String group) {
		return new REImpl("\\k<" + group + ">");
	}

	/**
	 * Create a list of clauses separated by a separator. The clauses and
	 * separators will be separated by zero or more whitespace.
	 *
	 * @param clause
	 * @param separator
	 * @return a new RE that presents a list of clauses
	 */
	public static RE list(RE clause, RE separator) {
		return term(clause, set(term(separator, clause)));
	}

	/**
	 * Create a list of clauses separated by a comma. The clauses and separators
	 * will be separated by zero or more whitespace.
	 *
	 * @param clause
	 * @return a new RE that presents a list of clauses separated by commas
	 */
	public static RE list(RE clause) {
		return list(clause, Catalog.comma);
	}

	/**
	 * Return an optional RE
	 *
	 * @param res the members of the optional
	 * @return a Q representing the optional
	 */
	public static Q opt(RE... res) {
		return new Quantified(0, 1, Quantified.Type.greedy, res);
	}

	/**
	 * Return an optional literal (see {@link #lit(String)}t
	 *
	 * @param s the literal
	 * @return a Q representing the optional
	 */
	public static Q opt(String s) {
		return opt(lit(s));
	}

	/**
	 * Return a group of some members.
	 *
	 * @param res the members
	 * @return a Q representing the some
	 */
	public static Q some(RE... res) {
		return new Quantified(1, Integer.MAX_VALUE, Q.Type.greedy, res);
	}

	/**
	 * Return a group of some members.
	 *
	 * @param res the members
	 * @return a Q representing the some
	 */
	public static Q set(RE... res) {
		return new Quantified(0, Integer.MAX_VALUE, Q.Type.greedy, res);
	}

	/**
	 * Creates a regular expression that matches the negation of the provided
	 * regular expression. This method wraps the given regex pattern in a
	 * negative construct.
	 *
	 * @param re the regular expression to be negated
	 * @return a new RE instance representing the negated version of the
	 *         provided regular expression
	 */
	public static RE not(RE re) {
		return re.not();
	}

	/**
	 * Modifies the given quantified regular expression to match reluctantly. A
	 * reluctant quantifier matches as few characters as possible.
	 *
	 * @param re the quantified regular expression to be modified
	 * @return a new RE instance with a reluctant quantification
	 */
	public static RE reluctant(Q re) {
		return re.reluctant();
	}

	/**
	 * Modifies the given quantified regular expression to match greedily. A
	 * greedy quantifier matches as many characters as possible.
	 *
	 * @param re the quantified regular expression to be modified
	 * @return a new RE instance with a greedy quantification
	 */
	public static RE greedy(Q re) {
		return re.greedy();
	}

	/**
	 * Modifies the given quantified regular expression to match in a possessive
	 * manner. A possessive quantifier does not give up matches as the regex
	 * engine backtracks.
	 *
	 * @param re the quantified regular expression to be modified
	 * @return a new RE instance with a possessive quantification
	 */
	public static RE possesive(Q re) {
		return re.possesive();
	}

	/**
	 * Creates a regular expression that matches anything except the specified
	 * string. For a single character, it creates a negated character class;
	 * otherwise, it negates the literal string.
	 *
	 * @param s the string to be negated in the match
	 * @return an RE instance that matches anything but the specified string
	 */
	public static RE anythingBut(String s) {
		if (s.length() == 1)
			return set(new CharacterClass(s).not());
		else
			return set(new REImpl(s).not());
	}

	/**
	 * Creates a regular expression that optionally matches the given string.
	 * The string is wrapped in a non-capturing group with a quantifier allowing
	 * zero or one occurrence.
	 *
	 * @param s the string to be optionally matched
	 * @return an RE instance that optionally matches the specified string
	 */
	public static RE maybe(String s) {
		return g(setAll, opt(s));
	}

	/**
	 * Creates a quantified regular expression that matches a specified minimum
	 * and maximum number of occurrences. This method applies a greedy
	 * quantifier.
	 *
	 * @param minimum the minimum number of occurrences to match
	 * @param maximum the maximum number of occurrences to match
	 * @param res the regular expressions to be quantified
	 * @return a Q instance representing the specified quantification
	 */
	public static Q multiple(int minimum, int maximum, RE... res) {
		return new Quantified(minimum, maximum, Quantified.Type.greedy, res);
	}

	/**
	 * Creates a quantified regular expression that matches a specified minimum
	 * and maximum number of occurrences of a literal string. This method
	 * applies a greedy quantifier.
	 *
	 * @param minimum the minimum number of occurrences to match
	 * @param maximum the maximum number of occurrences to match
	 * @param lit the literal string to be quantified
	 * @return a Q instance representing the specified quantification of the
	 *         literal string
	 */
	public static Q multiple(int minimum, int maximum, String lit) {
		return new Quantified(minimum, maximum, Quantified.Type.greedy, lit(lit));
	}

	/**
	 * Creates a quantified regular expression that matches at least a specified
	 * minimum number of occurrences. This method applies a greedy quantifier.
	 *
	 * @param minimum the minimum number of occurrences to match
	 * @param res the regular expressions to be quantified
	 * @return a Q instance representing the quantification with the specified
	 *         minimum and no maximum limit
	 */
	public static Q atLeast(int minimum, RE... res) {
		return new Quantified(minimum, Integer.MAX_VALUE, Quantified.Type.greedy, res);
	}

	/**
	 * Creates a quantified regular expression that matches at least a specified
	 * minimum number of occurrences of a literal string. This method applies a
	 * greedy quantifier.
	 *
	 * @param minimum the minimum number of occurrences to match
	 * @param lit the literal string to be quantified
	 * @return a Q instance representing the quantification with the specified
	 *         minimum and no maximum limit
	 */
	public static Q atLeast(int minimum, String lit) {
		return atLeast(minimum, lit(lit));
	}

	/**
	 * Applies the case-insensitive flag to the provided regular expressions.
	 * This method makes the given patterns match characters regardless of their
	 * case.
	 *
	 * @param res the regular expressions to be affected by the case-insensitive
	 *            flag
	 * @return an F instance with the case-insensitive flag applied
	 */
	public static F caseInsenstive(RE... res) {
		return new Option(EnumSet.of(F.Flag.CASE_INSENSITIVE), null, res);
	}

	/**
	 * Turns off the case-insensitive flag for the provided regular expressions.
	 * This method reverts the given patterns to match characters considering
	 * their case.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            case-insensitive flag
	 * @return an F instance with the case-insensitive flag turned off
	 */
	public static F caseInsenstiveOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.CASE_INSENSITIVE), res);
	}

	/**
	 * Applies the dotall flag to the provided regular expressions. With this
	 * flag, the dot ('.') pattern matches any character, including a line
	 * terminator.
	 *
	 * @param res the regular expressions to be affected by the dotall flag
	 * @return an F instance with the dotall flag applied
	 */
	public static F dotall(RE... res) {
		return new Option(EnumSet.of(F.Flag.DOTALL), null, res);
	}

	/**
	 * Turns off the dotall flag for the provided regular expressions. With the
	 * flag turned off, the dot ('.') pattern does not match line terminators by
	 * default.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            dotall flag
	 * @return an F instance with the dotall flag turned off
	 */
	public static F dotallOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.DOTALL), res);
	}

	/**
	 * Applies the comments flag to the provided regular expressions. This flag
	 * allows whitespace and comments within the pattern for better readability.
	 *
	 * @param res the regular expressions to be affected by the comments flag
	 * @return an F instance with the comments flag applied
	 */
	public static F comments(RE... res) {
		return new Option(EnumSet.of(F.Flag.COMMENTS), null, res);
	}

	/**
	 * Turns off the comments flag for the provided regular expressions. With
	 * the flag turned off, whitespace and comments within the pattern are no
	 * longer ignored.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            comments flag
	 * @return an F instance with the comments flag turned off
	 */
	public static F commentsOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.COMMENTS), res);
	}

	/**
	 * Applies the multiline flag to the provided regular expressions. This flag
	 * changes the behavior of '^' and '$' from matching at the start and end of
	 * the input string to matching at the start and end of each line.
	 *
	 * @param res the regular expressions to be affected by the multiline flag
	 * @return an F instance with the multiline flag applied
	 */
	public static F multiline(RE... res) {
		return new Option(EnumSet.of(F.Flag.MULTILINE), null, res);
	}

	/**
	 * Turns off the multiline flag for the provided regular expressions. With
	 * the flag turned off, '^' and '$' match only at the start and end of the
	 * entire input string.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            multiline flag
	 * @return an F instance with the multiline flag turned off
	 */
	public static F multilineOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.MULTILINE), res);
	}

	/**
	 * Applies the unicode character class flag to the provided regular
	 * expressions. This flag enables the Unicode versions of predefined
	 * character classes and POSIX character classes.
	 *
	 * @param res the regular expressions to be affected by the unicode
	 *            character class flag
	 * @return an F instance with the unicode character class flag applied
	 */
	public static F unicodeCharacterClass(RE... res) {
		return new Option(EnumSet.of(F.Flag.UNICODE_CHARACTER_CLASS), null, res);
	}

	/**
	 * Turns off the unicode character class flag for the provided regular
	 * expressions. With the flag turned off, the ASCII versions of predefined
	 * character classes and POSIX character classes are used.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            unicode character class flag
	 * @return an F instance with the unicode character class flag turned off
	 */
	public static F unicodeCharacterClassOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.UNICODE_CHARACTER_CLASS), res);
	}

	/**
	 * Applies the unicode case flag to the provided regular expressions. This
	 * flag enables the correct handling of character cases in Unicode when
	 * applying case-insensitive matching.
	 *
	 * @param res the regular expressions to be affected by the unicode case
	 *            flag
	 * @return an F instance with the unicode case flag applied
	 */
	public static F unicodeCase(RE... res) {
		return new Option(EnumSet.of(F.Flag.UNICODE_CASE), null, res);
	}

	/**
	 * Turns off the unicode case flag for the provided regular expressions.
	 * With the flag turned off, character cases in Unicode are not correctly
	 * handled when applying case-insensitive matching.
	 *
	 * @param res the regular expressions to be affected by turning off the
	 *            unicode case flag
	 * @return an F instance with the unicode case flag turned off
	 */
	public static F unicodeCaseOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.UNICODE_CASE), res);
	}

	/**
	 * Applies the unix lines flag to the provided regular expressions. This
	 * flag affects how line terminators are matched. With this flag, only the
	 * '\n' line terminator is recognized.
	 *
	 * @param res the regular expressions to be affected by the unix lines flag
	 * @return an F instance with the unix lines flag applied
	 */
	public static F unixLines(RE... res) {
		return new Option(EnumSet.of(F.Flag.UNIX_LINES), null, res);
	}

	/**
	 * Turns off the unix lines flag for the provided regular expressions. With
	 * the flag turned off, line terminators are matched in a
	 * platform-independent manner.
	 *
	 * @param res the regular expressions to be affected by turning off the unix
	 *            lines flag
	 * @return an F instance with the unix lines flag turned off
	 */
	public static F unixLinesOff(RE... res) {
		return new Option(null, EnumSet.of(F.Flag.UNIX_LINES), res);
	}

	/**
	 * Combine the res into a single atomic Group.
	 *
	 * @see #atomic(String)
	 * @param res the constituents.
	 */
	public static G atomic(RE... res) {
		return new Group(G.Type.ATOMIC, res);
	}

	/**
	 * Creates an atomic group with the provided string. An atomic group
	 * prevents the regex engine from backtracking once the group has matched.
	 *
	 * @param string the literal string to be included in the atomic group
	 * @return a G instance representing an atomic group containing the provided
	 *         string
	 */
	public static G atomic(String string) {
		return new Group(G.Type.ATOMIC, lit(string));
	}

	/**
	 * Creates a lookahead group with the provided regular expressions. A
	 * lookahead group asserts that the given pattern must be matched next in
	 * the input sequence.
	 *
	 * @param res the regular expressions to be included in the lookahead group
	 * @return a G instance representing a lookahead group containing the
	 *         provided expressions
	 */
	public static G ahead(RE... res) {
		return new Group(G.Type.AHEAD, res);
	}

	/**
	 * Creates a lookbehind group with the provided regular expressions. A
	 * lookbehind group asserts that the given pattern must precede the current
	 * position in the input sequence.
	 *
	 * @param res the regular expressions to be included in the lookbehind group
	 * @return a G instance representing a lookbehind group containing the
	 *         provided expressions
	 */
	public static G behind(RE... res) {
		return new Group(G.Type.BEHIND, res);
	}

	/**
	 * Creates a sequence of regular expressions. This method groups the
	 * provided expressions in the order they are given, without any additional
	 * logic.
	 *
	 * @param res the regular expressions to be sequenced
	 * @return a G instance representing a sequence of the provided regular
	 *         expressions
	 */
	public static G seq(RE... res) {
		return new Group(G.Type.NONE, res);
	}

	/**
	 * Creates a conditional regular expression. The resulting pattern matches
	 * 'thenExpect' if 'condition' matches, otherwise it matches
	 * 'otherWiseExpect'.
	 *
	 * @param condition the conditional regular expression
	 * @param thenExpect the regular expression to match if the condition is
	 *            true
	 * @param otherWiseExpect the regular expression to match if the condition
	 *            is false
	 * @return an RE instance representing the conditional regular expression
	 */
	public static RE if_(RE condition, RE thenExpect, RE otherWiseExpect) {
		RE pos = seq(ahead(condition), thenExpect);
		RE all = or(pos, otherWiseExpect);
		return all;
	}

	/**
	 * Creates a conditional regular expression with no alternative case. The
	 * resulting pattern matches 'then' only if 'condition' matches.
	 *
	 * @param condition the conditional regular expression
	 * @param then the regular expression to match if the condition is true
	 * @return an RE instance representing the conditional regular expression
	 *         with no alternative case
	 */
	public static RE if_(RE condition, RE then) {
		return seq(ahead(condition), g(then));
	}

	/**
	 * Creates a regular expression that repeats the 'thenExpect' pattern as
	 * long as 'condition' matches.
	 *
	 * @param condition the condition for repetition
	 * @param thenExpect the regular expression to be repeated
	 * @return an RE instance representing the repeated regular expression
	 */
	public static RE while_(RE condition, RE thenExpect) {
		return set(seq(ahead(condition), thenExpect));
	}

	/**
	 * Creates a regular expression that matches 'thenExpect' until 'condition'
	 * becomes true.
	 *
	 * @param condition the condition to terminate matching
	 * @param thenExpect the regular expression to match until the condition is
	 *            met
	 * @return an RE instance representing the regular expression matching until
	 *         the condition
	 */
	public static RE until(RE condition, RE thenExpect) {
		return set(seq(thenExpect, ahead(condition)));
	}

	/**
	 * Creates a capturing group with the provided regular expressions. This
	 * method groups the expressions and captures them for later reference.
	 *
	 * @param res the regular expressions to be included in the capturing group
	 * @return a G instance representing a capturing group containing the
	 *         provided expressions
	 */
	public static G capture(RE... res) {
		return new Group(G.Type.CAPTURING, res);
	}

	final public static C	ws						= new Special("\\s");
	final public static Q	setWs					= set(ws);
	final public static Q	someWs					= some(ws);
	final public static RE	all						= new REImpl(".");
	final public static Q	setAll					= set(all);
	final public static RE	someAll					= some(all);
	final public static C	backslash				= new CharacterClass("\\\\");
	final public static C	Lu						= new Predefined("Lu", true);
	final public static C	Ll						= new Predefined("Ll", true);
	final public static C	Lt						= new Predefined("Lt", true);
	final public static C	Lm						= new Predefined("Lm", true);
	final public static C	Lo						= new Predefined("Lo", true);
	final public static C	Nd						= new Predefined("Nd", true);
	final public static C	Nl						= new Predefined("Nl", true);
	final public static C	No						= new Predefined("No", true);
	final public static C	Z						= new Predefined("Z", true);
	final public static C	P						= new Predefined("P", true);
	final public static C	S						= new Predefined("S", true);
	final public static C	Cc						= new Predefined("Cc", true);
	final public static C	Cf						= new Predefined("Cf", true);
	final public static C	Cn						= new Predefined("Cn", true);
	final public static C	Lower					= new Predefined("Lower", true);
	final public static C	Upper					= new Predefined("Upper", true);
	final public static C	ASCII					= new Predefined("ASCII", true);
	final public static C	Alpha					= new Predefined("Alpha", true);
	final public static C	Digit					= new Predefined("Digit", true);
	final public static C	Alnum					= new Predefined("Alnum", true);
	final public static C	Punct					= new Predefined("Punct", true);
	final public static C	Graph					= new Predefined("Graph", true);
	final public static C	Print					= new Predefined("Print", true);
	final public static C	Blank					= new Predefined("Blank", true);
	final public static C	Cntrl					= new Predefined("Cntrl", true);
	final public static C	XDigit					= new Predefined("XDigit", true);
	final public static C	Space					= new Predefined("Space", true);
	final public static C	letter					= new Special("\\w");
	final public static C	dollar					= new Special("\\$");
	final public static C	euro					= new Special("€");
	final public static Q	word					= some(letter);
	final public static C	digit					= new Special("\\d");
	final public static C	nonDigit				= digit.not();
	final public static C	lineEnd					= new Special("\\b");
	final public static C	dot						= new Special("\\.");
	final public static C	comma					= new Special(",");
	final public static C	semicolon				= new Special(";");
	final public static C	colon					= new Special(":");
	final public static RE	nl						= new Boundary("\\R");
	final public static C	cr						= new Special("\r");
	final public static C	lf						= new Special("\n");
	final public static C	ff						= new Special("\f");
	final public static C	alarm					= new Special("\\a");
	final public static C	escape					= new Special("\\e");
	final public static RE	eof						= new Boundary("$");
	final public static RE	eol						= or(nl, eof);
	final public static C	parOpen					= new Special("\\(");
	final public static C	parClose				= new Special("\\)");
	final public static RE	empty					= new REImpl("");
	final public static C	tab						= new Special("\t");
	final public static RE	number					= some(digit);
	final public static C	minus					= new CharacterClass("-");
	final public static C	dquote					= new CharacterClass("\"");
	final public static C	squote					= new CharacterClass("'");
	final public static C	backQuote				= new CharacterClass("`");
	final public static C	underscore				= new CharacterClass("_");
	final public static Q	qualifier				= some(or(Alpha, Digit, underscore, minus));
	final public static RE	version					=															//
		g(number, opt(g(dot, number, opt(g(dot, number, opt(g(dot, qualifier)))))));
	final public static C	javaLowerCase			= new Predefined("javaLowerCase", true);
	final public static C	javaUpperCase			= new Predefined("javaUpperCase", true);
	final public static C	javaWhitespace			= new Predefined("javaWhitespace", true);
	final public static C	javaMirrored			= new Predefined("javaMirrored", true);
	final public static C	javaJavaIdentifierStart	= new Predefined("javaJavaIdentifierStart", true);
	final public static C	javaJavaIdentifierPart	= new Predefined("javaJavaIdentifierPart", true);
	final public static RE	javaId					= seq(javaJavaIdentifierStart, set(javaJavaIdentifierPart));
	final public static RE	startOfLine				= new Boundary("^");
	final public static RE	endOfLine				= new Boundary("$");
	final public static RE	wordBoundary			= new Boundary("\\b");
	final public static RE	beginInput				= new Boundary("\\A");
	final public static RE	endOfPreviousMatch		= new Boundary("\\G");
	final public static RE	endOfInputForFinal		= new Boundary("\\Z");
	final public static RE	endOfInput				= new Boundary("\\z");
	final public static C	isLatin					= new Predefined("IsLatin", true);
	final public static C	inGreek					= new Predefined("InGreek", true);
	final public static C	isAlphabetic			= new Predefined("isAlphabetic", true);
	final public static C	sc						= new Predefined("Sc", true);

	static class REImpl implements RE {
		final String		literal;
		final Set<String>	groups;
		Pattern				pattern;
		int					flags;

		REImpl(String literal) {
			this.literal = literal;
			groups = null;
		}

		REImpl(String literal, String... names) {
			this.literal = literal;
			this.groups = names.length > 0 ? new LinkedHashSet<>() : null;
			for (String name : names)
				this.groups.add(name);
		}

		@Override
		public RE not() {
			StringBuilder sb = new StringBuilder();
			sb.append("(?!");
			sb.append(literal);
			sb.append(")");
			return new REImpl(sb.toString());
		}

		@Override
		public String toString() {
			return literal;
		}

		@Override
		public Pattern pattern() {
			if (pattern == null || flags != 0) {
				pattern = Pattern.compile(toString());
			}
			return pattern;
		}

		@Override
		public Pattern pattern(RE.F.Flag... type) {
			if (type == null || type.length == 0)
				return Pattern.compile(toString());

			int options = 0;
			for (Flag flag : type) {
				options |= flag.option;
			}
			if (pattern == null || flags != options) {
				pattern = Pattern.compile(toString(), options);
				this.flags = options;
			}
			return pattern;
		}

		@Override
		public boolean isMatch(String string) {
			return pattern().matcher(string)
				.matches();
		}

		@Override
		public Optional<Match> matches(String string) {
			if (string == null)
				return Optional.empty();
			return matches0(string, Matcher::matches);
		}

		Optional<Match> matches0(String string, Predicate<Matcher> m) {
			if (string == null)
				return Optional.empty();
			Matcher matcher = pattern().matcher(string);
			return matches1(string, m, matcher);

		}

		private Optional<Match> matches1(String string, Predicate<Matcher> m, Matcher matcher) {
			if (m.test(matcher)) {
				abstract class Base {
					public int length() {
						return value().length();
					}

					public char charAt(int index) {
						return value().charAt(index);
					}

					public CharSequence subSequence(int start, int end) {
						return value().subSequence(start, end);
					}

					public Matcher getMatcher() {
						return matcher;
					}

					public abstract String value();

					@Override
					public String toString() {
						return value();
					}
				}

				class MatchGroupImpl extends Base implements MatchGroup {
					final String	name;
					String			value;
					int				start	= -1;
					int				end		= -1;

					MatchGroupImpl(String name, String value) {
						this.name = name;
						this.value = value;
					}

					@Override
					public String name() {
						return name;
					}

					@Override
					public String value() {
						return value == null ? value : matcher.group(name);
					}

					@Override
					public int start() {
						return start < 0 ? start = matcher.start(name) : start;
					}

					@Override
					public int end() {
						return end < 0 ? end = matcher.start(name) : end;
					}
				}
				class MatchImpl extends Base implements Match {
					Map<String, MatchGroup>	matchGroups;
					Map<String, String>		matchValues;

					@Override
					public String name() {
						return "";
					}

					@Override
					public String value() {
						return matcher.group();
					}

					@Override
					public int start() {
						return matcher.start();
					}

					@Override
					public int end() {
						return matcher.end();
					}

					@Override
					public Map<String, MatchGroup> getGroups() {
						if (matchGroups == null) {
							if (groups == null)
								matchGroups = Collections.emptyMap();
							Map<String, MatchGroup> result = new TreeMap<>();
							for (String name : groups) {
								String value = matcher.group(name);
								if (value != null) {
									MatchGroupImpl mg = new MatchGroupImpl(name, value);
									result.put(name, mg);
								}
							}
							matchGroups = Collections.unmodifiableMap(result);
						}
						return matchGroups;
					}

					@Override
					public Map<String, String> getGroupValues() {
						if (matchValues == null) {
							Map<String, String> result = new LinkedHashMap<>();
							getGroups().forEach((k, v) -> result.put(k, v.value()));
							matchValues = Collections.unmodifiableMap(result);
						}
						return matchValues;
					}

					@Override
					public Optional<MatchGroup> group(String name) {
						if (groups == null)
							throw new IllegalArgumentException("no groups defined");

						if (!groups.contains(name)) {
							throw new IllegalArgumentException("no group name defined: " + name + " in " + groups);
						}
						String value = matcher.group(name);
						if (value == null) {
							Optional.empty();
						}
						return Optional.of(new MatchGroupImpl(name, value));
					}
				}
				return Optional.of(new MatchImpl());
			} else
				return Optional.empty();
		}

		@Override
		public Optional<Match> lookingAt(String string) {
			return matches0(string, Matcher::lookingAt);
		}

		private Predicate<String> predicate(Predicate<Matcher> subPredicate) {
			Pattern pattern = pattern();
			return s -> subPredicate.test(pattern.matcher(s));
		}

		@Override
		public Optional<Match> find(String string) {
			return matches0(string, Matcher::find);
		}

		@Override
		public Stream<Match> findAll(String string) {
			return stream(string);
		}

		@Override
		public Predicate<String> asMatchPredicate() {
			return predicate(Matcher::matches);
		}

		@Override
		public Predicate<String> asFindPredicate() {
			return predicate(Matcher::find);
		}

		@Override
		public Predicate<String> asLookingAtPredicate() {
			return predicate(Matcher::lookingAt);
		}

		Stream<Match> stream(String string) {
			Spliterator<Match> spliterator = spliterator(string, Spliterator.ORDERED | Spliterator.NONNULL);
			return StreamSupport.stream(spliterator, false);
		}

		Spliterator<Match> spliterator(String string, int options) {
			Iterator<Match> iterator = iterator(string);
			return Spliterators.spliteratorUnknownSize(iterator, options);
		}

		Iterator<Match> iterator(String string) {
			Matcher matcher = pattern().matcher(string);
			return new Iterator<Match>() {
				Optional<Match> match;

				@Override
				public boolean hasNext() {
					match = matches1(string, Matcher::find, matcher);
					return match.isPresent();
				}

				@Override
				public Match next() {
					return match.get();
				}
			};
		}

		@Override
		public Matcher getMatcher(String string) {
			return pattern().matcher(string);
		}

		@Override
		public boolean isSingle() {
			return literal.length() == 1;
		}

		@Override
		public Optional<RE> merge(RE re) {
			return Optional.empty();
		}

		@Override
		public Set<String> getGroupNames() {
			return groups == null ? new HashSet<>() : new HashSet<>(groups);
		}

		@Override
		public void append(StringBuilder sb, String string, Function<Match, String> replacement) {
			AtomicInteger begin = new AtomicInteger(0);
			stream(string).forEach(match -> {
				sb.append(string.subSequence(begin.getAndSet(match.end()), match.start()));
				String r = replacement.apply(match);
				if (r != null)
					sb.append(r);
			});
			sb.append(string.substring(begin.get()));
		}

	}

	static class Group extends REImpl implements G {
		final Type		type;
		final String	name;

		Group(Type type, RE... res) {
			this(null, toGroupedString(false, res), type, names(res));
		}

		Group(Type type, String literal) {
			this(null, literal, type);
		}

		Group(String name, RE... res) {
			this(name, toGroupedString(false, res), name == null ? Group.Type.NONCAPTURING : Group.Type.NAMED,
				names(res));
		}

		private Group(String name, String literal, Type type, String... names) {
			super(literal, name == null ? names : combine(name, names));
			this.type = type;
			this.name = name;
		}

		@Override
		public Group not() {
			Type type = switch (this.type) {
				case AHEAD -> Type.NOT_AHEAD;
				case BEHIND -> Type.NOT_BEHIND;
				case NOT_AHEAD -> Type.AHEAD;
				case NOT_BEHIND -> Type.BEHIND;
				default -> null;
			};
			if (type == null || type == this.type)
				return this;

			return new Group(null, literal, type);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(type.prefix);
			if (type == Type.NAMED) {
				sb.append(name);
				sb.append(">");
			}
			sb.append(literal);
			sb.append(type.suffix);
			return sb.toString();
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public Type groupType() {
			return type;
		}
	}

	static class Predefined extends CharacterClass {

		Predefined(String literal, boolean positive) {
			super(literal, positive, null);
		}

		Predefined(String literal) {
			this(literal, true);
		}

		@Override
		public boolean isSingle(String literal) {
			return true;
		}

		@Override
		public String asSetContent() {
			return toString();
		}

		@Override
		public Predefined not() {
			return new Predefined(literal, !positive);
		}

		@Override
		public String toString() {
			if (positive)
				return "\\p{" + literal + "}";
			else
				return "\\P{" + literal + "}";
		}
	}

	static class Special extends CharacterClass {

		Special(String literal, boolean positive) {
			super(literal, positive, null);
		}

		Special(String literal) {
			this(literal, true);
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public Special not() {
			return new Special(literal, !positive);
		}

		@Override
		public String asSetContent() {
			return toString();
		}

		@Override
		public String toString() {
			if (positive)
				return literal;
			else {
				String s = switch (literal) {
					case "\\s" -> "\\S";
					case "\\S" -> "\\s";
					case "\\w" -> "\\W";
					case "\\W" -> "\\w";
					case "\\d" -> "\\D";
					case "\\D" -> "\\d";
					case "\\h" -> "\\H";
					case "\\H" -> "\\h";
					case "\\v" -> "\\V";
					case "\\V" -> "\\v";
					default -> {
						StringBuilder sb = new StringBuilder();
						sb.append("[^");
						sb.append(literal);
						sb.append("]");
						yield sb.toString();
					}
				};
				return s;
			}
		}
	}

	static class Quantified extends REImpl implements Q {
		final Type	type;
		final int	minimum;
		final int	maximum;

		Quantified(int minimum, int maximum, Type type, RE... res) {
			this(toGroupedString(true, res), minimum, maximum, type, names(res));
		}

		public Quantified(String grouped, int minimum, int maximum, Type type, String... names) {
			super(grouped, names);
			assert minimum >= 0;
			assert maximum > 0;
			this.minimum = minimum;
			this.maximum = maximum;
			this.type = type;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();

			if (minimum == 1 && maximum == 1 && type == Type.greedy)
				return literal;

			sb.append(literal);

			if (minimum == 0 && maximum == 1) {
				sb.append("?");
			} else if (minimum == 0 && maximum == Integer.MAX_VALUE) {
				sb.append("*");
			} else if (minimum == 1 && maximum == Integer.MAX_VALUE) {
				sb.append("+");
			} else if (minimum == maximum) {
				sb.append("{")
					.append(minimum)
					.append("}");
			} else if (maximum == Integer.MAX_VALUE) {
				sb.append("{")
					.append(minimum)
					.append(",")
					.append("}");
			} else {
				sb.append("{")
					.append(minimum)
					.append(",")
					.append(maximum)
					.append("}");
			}
			switch (type) {
				case greedy :
					break;
				case possesive :
					sb.append("+");
					break;
				case reluctant :
					sb.append("?");
					break;
			}
			return sb.toString();
		}

		@Override
		public RE reluctant() {
			return new Quantified(literal, minimum, maximum, Type.reluctant);
		}

		@Override
		public RE greedy() {
			return new Quantified(literal, minimum, maximum, Type.reluctant);
		}

		@Override
		public RE possesive() {
			return new Quantified(literal, minimum, maximum, Type.possesive);
		}
	}

	static class Boundary extends REImpl {

		Boundary(String literal) {
			super(literal);
		}

		@Override
		public RE not() {
			return switch (literal) {
				case "^" -> Catalog.startOfLine;
				case "$" -> Catalog.endOfLine;
				case "\\b" -> new Boundary("\\B");
				case "\\A" -> Catalog.endOfInput;
				case "\\z" -> Catalog.beginInput;
				default -> super.not();
			};
		}

	}

	static class Option extends REImpl implements F {
		private static final EnumSet<Flag>	NONE_OF	= EnumSet.noneOf(Flag.class);

		final EnumSet<Flag>					positive;
		final EnumSet<Flag>					negative;

		Option(EnumSet<Flag> positive, EnumSet<Flag> negative, RE... res) {
			this(toGroupedString(false, res), positive, negative);
		}

		Option(String ungrouped, EnumSet<Flag> p, EnumSet<Flag> n) {
			super(ungrouped);
			this.positive = p == null ? NONE_OF : p;
			this.negative = n == null ? NONE_OF : n;
		}

		@Override
		public String toString() {
			EnumSet<Flag> p = EnumSet.copyOf(positive);
			p.removeAll(negative);
			EnumSet<Flag> n = EnumSet.copyOf(negative);
			n.removeAll(positive);

			if (p.isEmpty() && n.isEmpty())
				return super.toString();

			StringBuilder sb = new StringBuilder();
			sb.append("(?");
			for (Flag f : p) {
				sb.append(f.flag);
			}
			if (!n.isEmpty()) {
				sb.append("-");
				for (Flag f : n) {
					sb.append(f.flag);
				}
			}
			if (!literal.isEmpty()) {
				sb.append(":");
				sb.append(literal);
			}
			sb.append(")");
			return sb.toString();
		}

		@Override
		public Optional<RE> merge(RE re) {
			if (re instanceof Option op) {
				EnumSet<Flag> p = EnumSet.copyOf(positive);
				EnumSet<Flag> n = EnumSet.copyOf(negative);
				p.addAll(op.positive);
				n.addAll(op.negative);
				return Optional.of(new Option(literal, p, n));
			} else
				return Optional.empty();
		}

		@Override
		public Set<Flag> positive() {
			return EnumSet.copyOf(positive);
		}

		@Override
		public Set<Flag> negative() {
			return EnumSet.copyOf(negative);
		}

	}

	static class CharacterClass extends REImpl implements C {
		final boolean			positive;
		final CharacterClass[]	conjunction;

		CharacterClass(String literal, boolean positive, CharacterClass[] conjunction) {
			super(literal);
			this.positive = positive;
			this.conjunction = conjunction == null ? new CharacterClass[0] : conjunction;
		}

		public CharacterClass(String string) {
			this(string, true, new CharacterClass[0]);
		}

		@Override
		public CharacterClass not() {
			return new CharacterClass(literal, !positive, conjunction);
		}

		@Override
		public Optional<RE> merge(RE other) {
			if (other instanceof CharacterClass cc) {
				return Optional.of(new CharacterClass(literal + cc.literal));
			}
			return Optional.empty();
		}

		/**
		 * https://www.regular-expressions.info/charclassintersect.html
		 */
		@Override
		public String toString() {
			if (isSingle() && positive) {
				return asSetContent();
			}
			StringBuilder sb = new StringBuilder("[");

			if (!positive)
				sb.append("^");

			sb.append(asSetContent());
			for (CharacterClass c : conjunction) {
				sb.append("&&");
				if (c.isSingle()) {
					sb.append(c.asSetContent());
				} else {
					sb.append(c);
				}
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		public String asSetContent() {
			return literal;
		}

		boolean isSingle(String literal) {
			return literal.length() == 1 || (literal.length() == 2 && literal.charAt(0) == '\\');
		}

		@Override
		public boolean isSingle() {
			return positive && isSingle(literal);
		}

		@Override
		public C and(C and) {
			CharacterClass[] copyOf = Arrays.copyOf(conjunction, conjunction.length + 1);
			copyOf[conjunction.length] = (CharacterClass) and;
			return new CharacterClass(asSetContent(), positive, copyOf);
		}

		@Override
		public C or(C or) {
			return new CharacterClass(asSetContent() + or.asSetContent(), positive, conjunction);
		}

	}

	static RE[] merge(RE[] res) {
		if (res.length < 2)
			return res;

		List<RE> result = new ArrayList<>();
		RE last = null;
		for (RE re : res) {
			if (last == null)
				last = re;
			else {
				RE merged = last.merge(re)
					.orElse(null);
				if (merged != null) {
					last = merged;
				} else {
					result.add(last);
					last = re;
				}
			}
		}
		result.add(last);
		return result.toArray(RE[]::new);
	}

	static String[] names(RE... res) {
		return Stream.of(res)
			.map(RE::getGroupNames)
			.filter(Objects::nonNull)
			.flatMap(Collection::stream)
			.toArray(String[]::new);
	}

	static String[] combine(String name, String... names) {
		if (names == null || names.length == 0)
			return new String[] {
				name
			};

		String[] result = Arrays.copyOf(names, names.length + 1);
		result[names.length] = name;
		return result;
	}

	static String toGroupedString(boolean force, RE... res) {
		res = merge(res);
		if (res.length == 0)
			return "";

		if (res.length == 1 && res[0].isSingle()) {
			return res[0].toString();
		}

		StringBuilder sb = new StringBuilder();
		if (force) {
			sb.append("(?:");
		}
		for (RE re : res) {
			sb.append(re);
		}
		if (force) {
			sb.append(")");
		}
		return sb.toString();
	}

	static boolean isWhiteSpace(RE re) {
		if (re == ws || re == setWs || re == someWs)
			return true;

		String s = re.toString();

		return switch (s) {
			case "\\s", " ", "\t", "\\s*", "\\s+", "(\\s)*", "(\\s)+", " *", " +", "( )*", "( )+" -> true;
			default -> false;
		};
	}

}
