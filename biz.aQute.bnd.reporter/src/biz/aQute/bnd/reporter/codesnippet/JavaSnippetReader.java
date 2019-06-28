package biz.aQute.bnd.reporter.codesnippet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetGroupDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetProgramDTO;

/**
 * A snippet readers for the Java language.
 */
class JavaSnippetReader extends SnippetReader {

	private static final Pattern				snippetPattern	= Pattern.compile("(?:\\$\\{\\s*snippet)(.*?)(?:\\})",
		Pattern.DOTALL);

	private final JavaParser					_parser;
	private final PrettyPrinterConfiguration	_printConf;

	JavaSnippetReader() {
		super("java");

		final ParserConfiguration configuration = new ParserConfiguration();
		configuration.setLexicalPreservationEnabled(false); // should be true,
															// waiting
															// JavaParser to
															// export
															// LexicalPreservingPrinter
		_parser = new JavaParser(configuration);

		_printConf = new PrettyPrinterConfiguration().setIndentSize(2)
			.setEndOfLineCharacter("\n");
	}

	@Override
	public List<Snippet> read(final File file) throws FileNotFoundException {
		final List<Snippet> snippets = new LinkedList<>();

		final ParseResult<CompilationUnit> r = _parser.parse(file);

		if (!r.isSuccessful()) {
			throw new RuntimeException("Failed to parser file " + file.getPath() + ". " + r.getProblems()
				.stream()
				.map(Problem::toString)
				.reduce("", (p, a) -> p + ", " + a));
		}

		r.ifSuccessful(cu -> {
			cu.removePackageDeclaration();

			cu.getTypes()
				.forEach(typeDeclaration -> {
					typeDeclaration.getComment()
						.map(c -> {
							final List<CodeSnippetConfig> result = parseSnippetConfig(c);
							if (c.getContent()
								.isEmpty()) {
								typeDeclaration.removeComment();
							}
							return result;
						})
						.orElse(new ArrayList<>())
						.forEach(typeConfig -> snippets
							.add(createSnippet(typeDeclaration.getNameAsString(), typeConfig, () -> formatCode(cu,
								typeDeclaration, typeConfig.includeImports, typeConfig.includeDeclaration))));

					typeDeclaration.getMethods()
						.forEach(methodDeclaration -> methodDeclaration.getComment()
							.map(c -> {
								final List<CodeSnippetConfig> result = parseSnippetConfig(c);
								if (c.getContent()
									.isEmpty()) {
									methodDeclaration.removeComment();
								}
								return result;
							})
							.orElse(new ArrayList<>())
							.forEach(methodConfig -> snippets.add(createSnippet(methodDeclaration.getNameAsString(),
								methodConfig, () -> formatCode(cu, methodDeclaration, methodConfig.includeImports,
									methodConfig.includeDeclaration)))));
				});
		});

		return snippets;
	}

	private String formatCode(final CompilationUnit cu, final TypeDeclaration<?> node, final boolean showImport,
		final boolean showDeclaration) {
		if (showImport && showDeclaration) {
			final StringBuilder sb = new StringBuilder();

			formatImports(sb, cu);

			return sb.append(node.toString(_printConf))
				.toString();
		} else if (showImport) {
			final StringBuilder sb = new StringBuilder();

			formatImports(sb, cu);

			formatTypeNoDeclaration(sb, node);

			return sb.toString();
		} else if (showDeclaration) {
			return node.toString(_printConf);
		} else {
			final StringBuilder sb = new StringBuilder();

			formatTypeNoDeclaration(sb, node);

			return sb.toString();
		}
	}

	private void formatTypeNoDeclaration(final StringBuilder sb, final TypeDeclaration<?> node) {
		if (node.getMembers() != null && !node.getMembers()
			.isEmpty()) {
			node.getMember(0)
				.addMarkerAnnotation("$$$");

			String body = node.toString(_printConf);
			body = Pattern.compile(".*?\\$\\$\\$\n", Pattern.DOTALL)
				.matcher(body)
				.replaceFirst("")
				.replaceAll("\n[^\\S\\r\\n]{2}", "\n");

			sb.append(body.substring(2, body.length() - 2));
		}
	}

	private String formatCode(final CompilationUnit cu, final MethodDeclaration node, final boolean showImport,
		final boolean showDeclaration) {
		if (showImport && showDeclaration) {
			final StringBuilder sb = new StringBuilder();

			formatImports(sb, cu);

			return sb.append(node.toString(_printConf))
				.toString();
		} else if (showImport) {
			final StringBuilder sb = new StringBuilder();

			formatImports(sb, cu);

			formatMethodNoDeclaration(sb, node);

			return sb.toString();
		} else if (showDeclaration) {
			return node.toString(_printConf);
		} else {
			final StringBuilder sb = new StringBuilder();

			formatMethodNoDeclaration(sb, node);

			return sb.toString();
		}
	}

	private void formatMethodNoDeclaration(final StringBuilder sb, final MethodDeclaration node) {
		node.getBody()
			.ifPresent(b -> {
				final String body = b.toString(_printConf)
					.replaceAll("\n[^\\S\\r\\n]{2}", "\n");
				sb.append(body.substring(2, body.length() - 2));
			});
	}

	private void formatImports(final StringBuilder sb, final CompilationUnit cu) {
		if (cu.getImports() != null && !cu.getImports()
			.isEmpty()) {
			cu.getImports()
				.forEach(i -> sb.append(i.toString(_printConf)));
			sb.append("\n");
		}
	}

	private Snippet createSnippet(final String id, final CodeSnippetConfig config,
		final Supplier<String> formatedCode) {
		if (config.groupName != null) {
			final CodeSnippetGroupDTO dto = new CodeSnippetGroupDTO();
			dto.id = config.id == null ? generateId(id) : config.id;
			dto.title = config.title;
			dto.description = config.description;
			return new Snippet(config.groupName, dto);
		} else {
			final CodeSnippetProgramDTO dto = new CodeSnippetProgramDTO();
			dto.id = config.id == null ? generateId(id) : config.id;
			dto.title = config.title;
			dto.description = config.description;
			dto.programmingLanguage = "java";
			dto.codeSnippet = formatedCode.get();
			return new Snippet(config.parentGroup, dto);
		}
	}

	private List<CodeSnippetConfig> parseSnippetConfig(final Comment comment) {
		final List<CodeSnippetConfig> configs = new LinkedList<>();

		final StringBuilder commentContent = new StringBuilder(comment.getContent());

		Matcher matcher = snippetPattern.matcher(commentContent);

		while (matcher.find()) {
			String unparsedConfig = commentContent.substring(matcher.start(1), matcher.end(1));
			if (isLegalConfig(unparsedConfig)) {
				commentContent.replace(matcher.start(0), matcher.end(0), "");
				unparsedConfig = cleanupConfig(unparsedConfig);

				final Attrs attrs = OSGiHeader.parseProperties(unparsedConfig);

				final CodeSnippetConfig config = new CodeSnippetConfig();
				config.id = attrs.get("id", null);
				config.title = attrs.get("title", null);
				config.description = attrs.get("description", null);
				config.includeDeclaration = Boolean.parseBoolean(attrs.get("includeDeclaration", "true"));
				config.includeImports = Boolean.parseBoolean(attrs.get("includeImports", "true"));
				config.groupName = attrs.get("groupName", null);
				config.parentGroup = attrs.get("parentGroup", null);

				configs.add(config);

				matcher = snippetPattern.matcher(commentContent);
			} else {
				commentContent.replace(matcher.end(1), matcher.end(1) + 1, "$$");
				matcher = snippetPattern.matcher(commentContent);
			}
		}

		if (!configs.isEmpty()) {
			comment.setContent(formatComment(commentContent.toString()));
		}

		return configs;
	}

	private boolean isLegalConfig(final String unparsedConfig) {
		final Matcher matcher = Pattern.compile("(?<!\\\\)\"")
			.matcher(unparsedConfig);

		int count = 0;
		while (matcher.find()) {
			count++;
		}

		return count % 2 == 0;
	}

	private String cleanupConfig(final String unparsedConfig) {
		String reworked = unparsedConfig.replaceAll("\n\\s*\\*", "\n");
		reworked = reworked.replaceAll("\n(\\s*\n)+", "\n\n");
		reworked = reworked.replaceAll("\\$\\$", "}");
		return reworked;
	}

	private String formatComment(final String commentContent) {
		if (Pattern.matches("[\n\\s\\*]*", commentContent)) {
			return "";
		} else {
			return commentContent;
		}
	}

	static private class CodeSnippetConfig {
		public String	id;
		public String	title;
		public String	description;
		public boolean	includeImports;
		public boolean	includeDeclaration;
		public String	parentGroup;
		public String	groupName;

		CodeSnippetConfig() {}
	}
}
