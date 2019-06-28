package aQute.lib.fileset;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import aQute.libg.glob.Glob;

/**
 * Implements a FileSet a la Ant/Gradle. A file set is a specification of a set
 * of files. A file set specification contains a number of '/' separated
 * segments. The last segment is Glob expression and the preceding segments
 * specify either a directory, a wildcard directory ('*'), or a set of wildcard
 * directories ('**').
 *
 * <pre>
 * filesets ::= fileset ( ',' fileset )*
 * fileset  ::= ( segment '/' )* filematch
 * segment  ::= any | glob
 * glob     ::= <glob expression>
 * any      ::= '**'
 * filematch::= allfile | anyfile | glob
 * anyfile  ::= '**' glob
 * allfile  ::= '**'
 * </pre>
 */
public class FileSet {
	private final File	base;
	private DFA			dfa;
	private String		source;

	public FileSet(File base, String filesetSpec) {
		this.source = filesetSpec;
		this.dfa = compile(filesetSpec);
		this.base = base;
	}

	public FileSet(File base, String filematch, Collection<String> allsourcepath) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (String f : allsourcepath) {
			if (f.endsWith("/"))
				f = f.substring(0, f.length() - 1);

			sb.append(del)
				.append(f)
				.append("/**/" + filematch);
			del = ",";
		}
		this.source = sb.toString();
		this.dfa = compile(this.source);
		this.base = base;
	}

	/*
	 * Compile the file set specification to a DFA
	 */
	private static DFA compile(String filesetSpec) {

		String parts[] = filesetSpec.trim()
			.split("\\s*,\\s*");
		DFA result = null;

		for (String part : parts) {

			if (part.startsWith("/"))
				throw new IllegalArgumentException("FileSet must not start with a /");

			// if we end in allfile (**), turn it in to any/all

			if (part.endsWith("**"))
				part = part + "/*";

			String[] segments = part.split("/");
			String lastSegment = segments[segments.length - 1];

			// anyfile (**.ts)

			DFA prev;
			if (lastSegment.startsWith("**")) {
				prev = new AnyDir(new FileMatch(lastSegment.substring(1)));
			} else
				prev = new FileMatch(lastSegment);

			for (int i = segments.length - 2; i >= 0; i--) {
				String segment = segments[i];
				if (segment.equals("**"))
					prev = new AnyDir(prev);
				else
					prev = new DirMatch(prev, segment);
			}

			if (result == null)
				result = prev;
			else {
				result = new OrDFA(result, prev);
			}
		}
		return result;
	}

	public Set<File> getFiles() {
		Set<File> files = new HashSet<>();
		if (base.isDirectory()) {
			for (File sub : base.listFiles()) {
				dfa.match(files, sub);
			}
		}
		return files;
	}

	public boolean isIncluded(File file) {
		URI target = file.toURI();
		URI source = base.toURI();
		URI relative = source.relativize(target);
		if (relative.equals(target) || relative.equals(source))
			return false;

		String[] segments = relative.getPath()
			.split("/");
		if (dfa.isIncluded(segments, 0))
			return true;

		return false;
	}

	public boolean isIncluded(String relativePath) {
		if (relativePath.startsWith("/"))
			throw new IllegalArgumentException("FileSet must not start with a /");

		String[] segments = relativePath.split("/");
		if (dfa.isIncluded(segments, 0))
			return true;

		return false;
	}

	public boolean hasOverlap(Collection<File> files) {
		for (File f : files) {
			if (isIncluded(f))
				return true;
		}
		return false;
	}

	public File findFirst(String file) {
		for (File f : getFiles()) {
			if (f.getName()
				.equals(file))
				return f;
		}
		return null;
	}

	/*
	 * Deterministic Finite Automata
	 */
	static abstract class DFA {
		abstract void match(Collection<File> files, File input);

		abstract boolean isIncluded(String segments[], int n);
	}

	/*
	 * Implements an Or state, both paths are executed in parallel
	 */
	static class OrDFA extends DFA {

		private DFA	a;
		private DFA	b;

		public OrDFA(DFA a, DFA b) {
			this.a = a;
			this.b = b;
		}

		@Override
		void match(Collection<File> files, File input) {
			a.match(files, input);
			b.match(files, input);
		}

		@Override
		boolean isIncluded(String[] segments, int n) {
			return a.isIncluded(segments, n) || b.isIncluded(segments, n);
		}

	}

	/*
	 * Must match a directory with a glob expression
	 */
	static class DirMatch extends DFA {
		final Glob	glob;
		final DFA	next;

		DirMatch(DFA next, String segment) {
			this.next = next;
			this.glob = new Glob(segment);
		}

		@Override
		void match(Collection<File> files, File input) {
			if (input.isDirectory()) {
				if (glob.matcher(input.getName())
					.matches()) {
					for (File sub : input.listFiles()) {
						next.match(files, sub);
					}
				}
			}
		}

		@Override
		boolean isIncluded(String segments[], int n) {
			if (n >= segments.length - 1)
				return false;

			if (!glob.matcher(segments[n])
				.matches())
				return false;

			return next.isIncluded(segments, n + 1);
		}

	}

	/*
	 * Matches ANY depth of directories, including zero.
	 */
	static class AnyDir extends DFA {
		final DFA next;

		AnyDir(DFA next) {
			this.next = next;
		}

		@Override
		void match(Collection<File> files, File input) {
			if (input.isDirectory()) {
				for (File sub : input.listFiles()) {
					next.match(files, sub);
					this.match(files, sub);
				}
			} else
				next.match(files, input);
		}

		@Override
		boolean isIncluded(String segments[], int n) {
			if (n >= segments.length - 1)
				return false;

			return next.isIncluded(segments, n + 1) || this.isIncluded(segments, n + 1);
		}
	}

	/*
	 * Match a file name with a glob
	 */
	static class FileMatch extends DFA {
		final Glob glob;

		FileMatch(String string) {
			this.glob = new Glob(string);
		}

		@Override
		void match(Collection<File> files, File input) {
			if (input.isFile()) {
				if (glob.matcher(input.getName())
					.matches())
					files.add(input);
			}
		}

		@Override
		boolean isIncluded(String segments[], int n) {
			if (n != segments.length - 1)
				return false;

			return glob.matcher(segments[n])
				.matches();
		}
	}

	@Override
	public String toString() {
		return source;
	}
}
