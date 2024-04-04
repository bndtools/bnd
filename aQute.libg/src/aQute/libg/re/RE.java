package aQute.libg.re;

import static aQute.libg.re.Catalog.cc;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import aQute.libg.re.RE.F.Flag;

/**
 * A library to make regular expressions with {@link Pattern} a bit easier to
 * use. The Pattern class is extremely powerfull and as far as I know high
 * performance. However,regular expressions quickly become unwieldy.
 * <p>
 * This class provides a more modern interface using lambdas and options and the
 * accompanying {@link Catalog} class provides a comprehensive set of constants
 * and static methods to create complex regular expressions.
 */
public interface RE {

	/**
	 * Represents a Character Class in a regular expression. This is an
	 * additional type since character classes have some special rules.
	 */
	interface C extends RE {
		/**
		 * Intersect two character classes. This uses the `&&` operator. I.e.
		 * `[%abc@]` and `\p{Alnum}` will intersect to only `abc` and will be
		 * represented as `[abc&&\p{Alnum}`.
		 *
		 * @param and the second character class
		 * @return a new character class
		 */
		C and(C and);

		/**
		 * Make the union of two character classes. This concatenates the set if
		 * possible. I.e. `[%abc@]` and `\p{Alnum}` will union to
		 * `[%abc@\p{Alnum}]`.
		 *
		 * @param or the second character class
		 * @return a new character class
		 */
		C or(C or);

		/**
		 * Make the union of two character classes. This concatenates the set if
		 * possible. I.e. `[%abc@]` and `\p{Alnum}` will union to
		 * `[%abc@\p{Alnum}]`.
		 *
		 * @param or the second character class
		 * @return a new character class
		 */
		default C or(String or) {
			return or(cc(or));
		}

		/**
		 * Return just the content of the set without the square brackets.
		 */
		String asSetContent();

		/**
		 * Some character sets have a reverse name. For example the `\s` has
		 * `\S`. A set with square brackets can be reversed by adding/removing a
		 * `^` as first character. This overrides the RE version but returns a C
		 * so this can be repeated.
		 */
		@Override
		C not();
	}

	/**
	 * Represents a flag. A flag can be specified during compilation or in an
	 * expression. It can work for the remainder of the expression or it can
	 * only be effective in a group.
	 */
	interface F extends RE {

		/**
		 * The supported flags
		 */
		public enum Flag {
			/**
			 * Match case insensitive, see {@link Pattern#CASE_INSENSITIVE}
			 */
			CASE_INSENSITIVE('i', Pattern.CASE_INSENSITIVE),
			/**
			 * Ignore comments, see {@link Pattern#COMMENTS}
			 */
			COMMENTS('x', Pattern.COMMENTS),
			/**
			 * The any ('.') matcher also matches the cr and lf, it normally
			 * doesn't.
			 */
			DOTALL('s', Pattern.DOTALL),
			/**
			 * The `$` and `^` normally match the begin and end of the input. In
			 * multiline mode they the beginning and ending of a line. See
			 * {@link Pattern#MULTILINE}
			 */
			MULTILINE('m', Pattern.MULTILINE),
			/**
			 * Use the Unicode rules to case fold, see
			 * {@link Pattern#UNICODE_CASE}
			 */
			UNICODE_CASE('u', Pattern.UNICODE_CASE),
			/**
			 * See {@link Pattern#UNICODE_CHARACTER_CLASS}
			 */
			UNICODE_CHARACTER_CLASS('U', Pattern.UNICODE_CHARACTER_CLASS),
			/**
			 * Only line separator recognized is \n. See
			 * {@link Pattern#UNIX_LINES}
			 */
			UNIX_LINES('d', Pattern.UNIX_LINES);

			/**
			 * The char that represents this flag. For example 'i' is the
			 * {@link Pattern#CASE_INSENSITIVE}.
			 */
			public final char	flag;

			/**
			 * The Pattern option
			 */
			public final int	option;

			Flag(char flag, int option) {
				this.flag = flag;
				this.option = option;
			}
		}

		/**
		 * Return the flags to turn off.
		 */
		Set<Flag> negative();

		/**
		 * Return the flags to turn on.
		 */
		Set<Flag> positive();
	}

	/**
	 * A group is a regular expression that groups a set of REs. A capturing
	 * group is a simple parenthesis open. Other groups start with `(?` and are
	 * then following by a unique identification.
	 */
	interface G extends RE {
		/**
		 * Variation of different group types
		 */
		enum Type {
			/**
			 * Matches _if_ its members can match ahead of the current position.
			 * It will not consume anything from the input. See
			 * https://www.regular-expressions.info/lookaround.html
			 */
			AHEAD("(?="),
			/**
			 * An atomic group is a group that, when the regex engine exits from
			 * it, automatically throws away all backtracking positions
			 * remembered by any tokens inside the group.The regular expression
			 * `a(bc|b)c` matches `abcc` and `abc`. The regex `a(?>bc|b)c`
			 * (atomic group) matches `abcc` but not `abc`.
			 */
			ATOMIC("(?>"),
			/**
			 * Matches _if_ its members can match behind the current position.
			 * It will not consume anything from the input. See
			 * https://www.regular-expressions.info/lookaround.html
			 */
			BEHIND("(?<="),
			/**
			 * Basic most simple group. It is advised not to use these since
			 * they need to be counted and that is really tricky. Using named
			 * groups is much easier and recommended.
			 */
			CAPTURING("("),
			/**
			 * If this group is matched, the value of this group specifically
			 * can be retrieved by its group name.
			 */
			NAMED("(?<"),
			/**
			 * Groups but will not capture a match.
			 */
			NONCAPTURING("(?:"),
			/**
			 * Will not provide grouping parenthesis.
			 */
			NONE("", ""),
			/**
			 * Will match if its members do not match ahead
			 */
			NOT_AHEAD("(?!"),
			/**
			 * Will match if its members do not match before
			 */
			NOT_BEHIND("(?<!"),

			/**
			 * Negated
			 */
			NOT("(?!");

			/**
			 * The prefix to start the grouping.Notice that the NAMED group is
			 * special, it will have to be followed by the name and ended with a
			 * `>`.
			 */
			final String	prefix;
			/**
			 * The suffix to end the grouping.
			 */
			final String	suffix;

			Type(String prefix) {
				this(prefix, ")");
			}

			Type(String prefix, String suffix) {
				this.prefix = prefix;
				this.suffix = suffix;
			}
		}

		/**
		 * Get the type of this group
		 */
		Type groupType();
	}

	/**
	 * The result of a matched group after a successful find, matches, or
	 * lookingAt operation.
	 */
	interface Match extends MatchGroup {

		/**
		 * Get the matching groups. This will only return the groups that were
		 * captured.
		 */
		Map<String, MatchGroup> getGroups();

		/**
		 * Get the matching groups with their value. This will only return the
		 * values that were actually captured.
		 */
		Map<String, String> getGroupValues();

		/**
		 * Get a group by name. This will throw an exception if the group was
		 * not defined in this regular expression. It will return an
		 * Optional.empty() when the group wasn't captured.
		 *
		 * @param name the name of the group
		 */
		Optional<MatchGroup> group(String name);

		/**
		 * This Match has a rover in its the matching region. This method
		 * requires the expected to match against the current position or it
		 * will throw an exception. It will move the rover forward to after the
		 * match. It will skip any whitespace before it matches.
		 *
		 * @param expected the expected match
		 * @return the value of the match
		 */
		default String take(RE expected) {
			skip(Catalog.setWs);
			String result = tryMatch(expected);

			if (result == null)
				throw new IllegalArgumentException("take: no match for " + expected + " on " + this);

			return result;
		}

		/**
		 * This Match has a rover in its the matching region. This method
		 * requires the skip to match against the current position or it will
		 * throw an exception. It will move the rover forward to after the
		 * match.
		 *
		 * @param skip the RE to skip
		 */
		default void skip(RE skip) {
			if (tryMatch(skip) == null)
				throw new IllegalArgumentException("skip: no match for " + skip + " on " + this);
		}

		/**
		 * This Match has a rover its the matching region. This method will see
		 * if the current position matches the RE. If it does, the rover will be
		 * moved forward. Otherwise it stays where it is. It will skip any
		 * whitespace before it matches.
		 *
		 * @param expected the expected value
		 * @return true if there was a match and the match was consumed
		 */
		default boolean check(RE expected) {
			skip(Catalog.setWs);
			return tryMatch(expected) != null;
		}

		/**
		 * This Match has a rover in its the matching region. This method tries
		 * too see if the string from this rover to the end of this match,
		 * matches the match parameter. If so, it returns the value and moves
		 * the rover forward.
		 *
		 * @param match the RE to match return a string when matched or null
		 */
		String tryMatch(RE match);

		Optional<MatchGroup> group(int group);

		/**
		 * This gets the value of a group but throws an exception of the group
		 * is not there.
		 *
		 * @param groupName the name of the group
		 */
		String presentGroup(String groupName);
	}

	/**
	 * Provides the details of a matching group. The Matching Group is also a
	 * CharSequence.
	 */
	interface MatchGroup extends CharSequence {
		/**
		 * The end index of this group in the original string. See
		 * {@link Matcher#end(String)}
		 */
		int end();

		/**
		 * The original matcher
		 */
		Matcher getMatcher();

		/**
		 * The name of the captured group
		 */
		String name();

		/**
		 * The start index of this group in the original string. See
		 * {@link Matcher#start(String)}
		 */
		int start();

		/**
		 * The value of the captured group.
		 */
		String value();
	}

	/**
	 * The `*`, `?`, `+` operators and the `{...}` suffix quantify the previous
	 * node. By default, these quantified nodes are _greedy_, they try to match
	 * as much as possible of the input. Quantified nodes can be further
	 * modified to be reluctant (first match) or possesive.
	 */
	interface Q extends RE {
		/**
		 * The types of modified quantification
		 */
		enum Type {
			/**
			 * Default, match as much as possible
			 */
			greedy,
			/**
			 * See documentation
			 */
			possesive,
			/**
			 * Stop after first match
			 */
			reluctant;
		}

		/**
		 * Set greedy
		 */
		RE greedy();

		/**
		 * Set possesive
		 */
		RE possesive();

		/**
		 * Set reluctant
		 */
		RE reluctant();
	}

	/**
	 * Return a predicate that checks if the pattern is found in the tested
	 * string.
	 */
	Predicate<String> asFindPredicate();

	/**
	 * Return a predicate that checks if the pattern is looking at in the tested
	 * string.
	 */
	Predicate<String> asLookingAtPredicate();

	/**
	 * Return a predicate that checks if the pattern is matched at in the tested
	 * string.
	 */
	Predicate<String> asMatchPredicate();

	/**
	 * Find the given pattern in the given string. If found, a Match is returned
	 * that can be used to continue.
	 *
	 * @param string the source string
	 * @return a matcher if found
	 */
	Optional<Match> findIn(String string);

	/**
	 * Return a stream with matches in the current string
	 *
	 * @param string the source string
	 */
	Stream<Match> findAllIn(String string);

	/**
	 * Return
	 *
	 * @param string the source string
	 * @return the replaced String
	 */
	default String append(String string, Function<Match, String> replacement) {
		StringBuilder sb = new StringBuilder(string.length() * 2);
		append(sb, string, replacement);
		return sb.toString();
	}

	/**
	 * Append the StringBuilder by finding all this matches in the given string,
	 * and using the replacement from the replacement function. For each match,
	 * this function is called with the Match. The function can then take the
	 * captured groups and calculate the replacement string. This is like a
	 * template function.
	 * <p>
	 * If the replacement function returns null, it will be ignored
	 *
	 * @param sb the builder
	 * @param string the source string
	 * @param replacement
	 */
	void append(StringBuilder sb, String string, Function<Match, String> replacement);

	/**
	 * Get a set of group names in the current RE. This includes any member REs
	 * recursively.
	 */
	Set<String> getGroupNames();

	/**
	 * Returns true if this RE matches the given string.
	 *
	 * @param string the source string
	 */
	boolean isMatch(String string);

	/**
	 * Returns true if this RE is a single node. That is a single letter, a
	 * character class, a group that is not NONE, etc.
	 */
	boolean isSingle();

	/**
	 * Match with lookingAt
	 *
	 * @param string the source string
	 */
	Optional<Match> lookingAt(String string);

	/**
	 * Get a new matcher activated with the given source string.
	 *
	 * @param string the source string
	 */
	Matcher getMatcher(CharSequence string);

	/**
	 * Matches the source string to this RE. If there is a match, it returns the
	 * Match
	 *
	 * @param string the source string
	 */
	Optional<Match> matches(String string);

	/**
	 * Merge another RE with this RE. This is not always possible.
	 *
	 * @param re the other RE
	 */
	Optional<RE> merge(RE re);

	/**
	 * Reverse the meaning of this RE. This depends on the different types. If
	 * it has no meaning, it will return the original.
	 */
	RE not();

	@Override
	String toString();

	/**
	 * Return the pattern compiled with the given flags. The pattern is cached
	 * for optimization but this method can be called concurrently.
	 *
	 * @param flags the flags
	 */
	Pattern pattern(Flag... flags);

}
