package bndtools.launch.api;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;

public class LaunchTest {

	@Test
	public void testArgumentRendering() throws Exception {
		File file = new File("bndtools.cocoa.macosx.x86_64.bndrun");
		try (Processor p = new Processor()) {
			p.setProperties(file);
			p.setProperty(
				"-runvm.test1", "-Dfoo=\"xyz abc\"");
			p.setProperty("-runvm.test2",
				"-Dbar=xyz abc, -Dlauncher.properties=C:\\Users\\bj hargrave\\git\\bnd\\bndtools.core\\generated\\launch7354374140259840283.properties");
			Parameters hdr = p.getMergedParameters(Constants.RUNVM);
			Collection<String> arguments = hdr.keyList();
			String[] argumentsArray = arguments
				.stream()
				// .flatMap(argument -> new QuotedTokenizer(argument, " \t",
				// false, true).stream()
				// .filter(
				// Strings::notEmpty))
				.toArray(String[]::new);
			String rendered = AbstractOSGiLaunchDelegate.renderArguments(argumentsArray);
			String[] roundTrip = parseArguments(rendered);
			System.out.printf("input:\n%s\nrendered:\n  «%s»\nroundTrip:\n%s\n\n", arguments.stream()
				.collect(joining("»,\n  «", "  «",
					"»")), //
				rendered, //
				Arrays.stream(roundTrip)
					.collect(joining("»,\n  «", "  «", "»")));
			assertThat(roundTrip).containsExactly(argumentsArray);
		}
	}

	// The following were copied from org.eclipse.debug.core.DebugPlugin for use
	// in this test

	public String[] parseArguments(String args) {
		if (args == null) {
			return new String[0];
		}

		if (IO.isWindows()) {
			return parseArgumentsWindows(args, false);
		}

		return parseArgumentsImpl(args, false);
	}

	private String[] parseArgumentsImpl(String args, boolean split) {
		// man sh, see topic QUOTING
		List<String> result = new ArrayList<>();

		final int DEFAULT = 0;
		final int ARG = 1;
		final int IN_DOUBLE_QUOTE = 2;
		final int IN_SINGLE_QUOTE = 3;

		int state = DEFAULT;
		StringBuilder buf = new StringBuilder();
		int len = args.length();
		for (int i = 0; i < len; i++) {
			char ch = args.charAt(i);
			if (Character.isWhitespace(ch)) {
				if (state == DEFAULT) {
					// skip
					continue;
				} else if (state == ARG) {
					state = DEFAULT;
					result.add(buf.toString());
					buf.setLength(0);
					continue;
				}
			}
			switch (state) {
				case DEFAULT :
				case ARG :
					if (ch == '"') {
						if (split) {
							buf.append(ch);
						}
						state = IN_DOUBLE_QUOTE;
					} else if (ch == '\'') {
						if (split) {
							buf.append(ch);
						}
						state = IN_SINGLE_QUOTE;
					} else if (ch == '\\' && i + 1 < len) {
						if (split) {
							buf.append(ch);
						}
						state = ARG;
						ch = args.charAt(++i);
						buf.append(ch);
					} else {
						state = ARG;
						buf.append(ch);
					}
					break;

				case IN_DOUBLE_QUOTE :
					if (ch == '"') {
						if (split) {
							buf.append(ch);
						}
						state = ARG;
					} else if (ch == '\\' && i + 1 < len && (args.charAt(i + 1) == '\\' || args.charAt(i + 1) == '"')) {
						if (split) {
							buf.append(ch);
						}
						ch = args.charAt(++i);
						buf.append(ch);
					} else {
						buf.append(ch);
					}
					break;

				case IN_SINGLE_QUOTE :
					if (ch == '\'') {
						if (split) {
							buf.append(ch);
						}
						state = ARG;
					} else {
						buf.append(ch);
					}
					break;

				default :
					throw new IllegalStateException();
			}
		}
		if (buf.length() > 0 || state != DEFAULT) {
			result.add(buf.toString());
		}

		return result.toArray(new String[result.size()]);
	}

	private String[] parseArgumentsWindows(String args, boolean split) {
		// see http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
		List<String> result = new ArrayList<>();

		final int DEFAULT = 0;
		final int ARG = 1;
		final int IN_DOUBLE_QUOTE = 2;

		int state = DEFAULT;
		int backslashes = 0;
		StringBuilder buf = new StringBuilder();
		int len = args.length();
		for (int i = 0; i < len; i++) {
			char ch = args.charAt(i);
			if (ch == '\\') {
				backslashes++;
				continue;
			} else if (backslashes != 0) {
				if (ch == '"') {
					for (; backslashes >= 2; backslashes -= 2) {
						buf.append('\\');
						if (split) {
							buf.append('\\');
						}
					}
					if (backslashes == 1) {
						if (state == DEFAULT) {
							state = ARG;
						}
						if (split) {
							buf.append('\\');
						}
						buf.append('"');
						backslashes = 0;
						continue;
					} // else fall through to switch
				} else {
					// false alarm, treat passed backslashes literally...
					if (state == DEFAULT) {
						state = ARG;
					}
					for (; backslashes > 0; backslashes--) {
						buf.append('\\');
					}
					// fall through to switch
				}
			}
			if (Character.isWhitespace(ch)) {
				if (state == DEFAULT) {
					// skip
					continue;
				} else if (state == ARG) {
					state = DEFAULT;
					result.add(buf.toString());
					buf.setLength(0);
					continue;
				}
			}
			switch (state) {
				case DEFAULT :
				case ARG :
					if (ch == '"') {
						state = IN_DOUBLE_QUOTE;
						if (split) {
							buf.append(ch);
						}
					} else {
						state = ARG;
						buf.append(ch);
					}
					break;

				case IN_DOUBLE_QUOTE :
					if (ch == '"') {
						if (i + 1 < len && args.charAt(i + 1) == '"') {
							/*
							 * Undocumented feature in Windows: Two consecutive
							 * double quotes inside a double-quoted argument are
							 * interpreted as a single double quote.
							 */
							buf.append('"');
							i++;
							if (split) {
								buf.append(ch);
							}
						} else if (buf.length() == 0) {
							// empty string on Windows platform. Account for bug
							// in constructor of JDK's java.lang.ProcessImpl.
							result.add("\"\""); //$NON-NLS-1$
							state = DEFAULT;
						} else {
							state = ARG;
							if (split) {
								buf.append(ch);
							}
						}
					} else {
						buf.append(ch);
					}
					break;

				default :
					throw new IllegalStateException();
			}
		}
		if (buf.length() > 0 || state != DEFAULT) {
			result.add(buf.toString());
		}

		return result.toArray(new String[result.size()]);
	}

}
