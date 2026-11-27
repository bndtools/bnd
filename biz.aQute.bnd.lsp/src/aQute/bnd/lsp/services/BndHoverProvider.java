package aQute.bnd.lsp.services;

import java.io.File;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.lsp.models.BndMacroRef;
import aQute.bnd.lsp.models.BndToken;
import aQute.bnd.lsp.models.BndTokenType;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.properties.BadLocationException;
import aQute.lib.io.IO;

public class BndHoverProvider {
	private final BndWorkspaceManager workspaceManager;

	public BndHoverProvider(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public Hover provideHover(BndDocumentModel model, HoverParams params) {
		Position pos = params.getPosition();
		int line = pos.getLine();
		int character = pos.getCharacter();
		BndHeaderEntry entry = model.getEntryAt(line);
		String includes = entry != null && Constants.INCLUDE.equals(entry.getKey()) ? includedFiles(model, entry) : null;

		// Check if hover is on a macro
		BndMacroRef macro = model.getMacroAt(line, character);
		if (macro != null) {
			return provideMacroHover(model, macro, includes);
		}

		// Check token at position
		BndToken token = model.getTokenAt(line, character);
		if (token != null) {
			if (token.getType() == BndTokenType.INSTRUCTION || token.getType() == BndTokenType.HEADER) {
				return provideHeaderHover(token.getText(), token.getLine(), token.getCharacter(), token.getLength(),
					includes);
			}
		}

		// Check if position is on an entry key
		if (entry != null && line == entry.getStartLine()) {
			if (character <= entry.getKeyLength()) {
				return provideHeaderHover(entry.getKey(), line, 0, entry.getKeyLength(), includes);
			}
		}

		if (includes != null) {
			return new Hover(new MarkupContent(MarkupKind.MARKDOWN, includes), lineRange(model, line));
		}
		return null;
	}

	/**
	 * Lists the files of an {@code -include} instruction as links. The paths
	 * resolve like {@code Processor.doIncludes}: macros expand in the context
	 * of the document, {@code -} and {@code ~} prefixes are dropped, and
	 * relative paths resolve against the directory of the document.
	 */
	String includedFiles(BndDocumentModel model, BndHeaderEntry entry) {
		File file = BndWorkspaceManager.getFileFromUri(model.getUri());
		if (file == null || entry.getValue()
			.isBlank()) {
			return null;
		}
		String value = workspaceManager.expandMacro(entry.getValue(), file);
		if (value == null) {
			value = entry.getValue();
		}
		StringBuilder md = new StringBuilder("**Included files:**\n\n");
		for (String clause : new Parameters(value).keySet()) {
			String path = Processor.removeDuplicateMarker(clause);
			boolean optional = false;
			while (path.startsWith("-") || path.startsWith("~")) {
				optional |= path.startsWith("-");
				path = path.substring(1)
					.trim();
			}
			if (path.isEmpty()) {
				continue;
			}
			File included = IO.getFile(file.getAbsoluteFile()
				.getParentFile(), path)
				.getAbsoluteFile();
			if (included.isFile()) {
				md.append("- [`")
					.append(included.getPath())
					.append("`](")
					.append(included.toPath()
						.toUri())
					.append(")\n");
			} else if (path.matches("(?i)(https?|file):.*")) {
				md.append("- [`")
					.append(path)
					.append("`](")
					.append(path.replace(" ", "%20"))
					.append(")\n");
			} else {
				md.append("- `")
					.append(included.getPath())
					.append("` (")
					.append(optional ? "optional, " : "")
					.append("does not exist)\n");
			}
		}
		return md.toString();
	}

	private static Range lineRange(BndDocumentModel model, int line) {
		int length = 0;
		try {
			length = model.getLineInformation(line)
				.getLength();
		} catch (BadLocationException e) {
			// keep an empty range
		}
		return new Range(new Position(line, 0), new Position(line, length));
	}

	private Hover provideHeaderHover(String headerName, int line, int character, int length, String includes) {
		Syntax syntax = Syntax.HELP.get(headerName);
		String mergedSuffix = null;

		if (syntax == null && headerName.startsWith("-")) {
			syntax = Syntax.HELP.get(headerName.substring(1));
		}

		// Check for merged instruction property (e.g. -runblacklist.win32 or -runbundles.extra)
		if (syntax == null && headerName.contains(".")) {
			int dot = headerName.indexOf('.');
			String baseHeader = headerName.substring(0, dot);
			mergedSuffix = headerName.substring(dot + 1);

			syntax = Syntax.HELP.get(baseHeader);
			if (syntax == null && baseHeader.startsWith("-")) {
				syntax = Syntax.HELP.get(baseHeader.substring(1));
			}
		}

		if (syntax == null) {
			return includes == null ? null
				: new Hover(new MarkupContent(MarkupKind.MARKDOWN, includes),
					new Range(new Position(line, character), new Position(line, character + length)));
		}

		StringBuilder md = new StringBuilder();
		if (mergedSuffix != null) {
			md.append("### `")
				.append(syntax.getHeader())
				.append(".")
				.append(mergedSuffix)
				.append("` *(Merged Instruction Property)*\n\n")
				.append("Merged sub-clause for **`")
				.append(syntax.getHeader())
				.append("`** with qualifier `")
				.append(mergedSuffix)
				.append("`.\n\n");
		} else {
			md.append("### `")
				.append(syntax.getHeader())
				.append("`\n\n");
		}

		if (syntax.getLead() != null && !syntax.getLead()
			.isEmpty()) {
			md.append(syntax.getLead())
				.append("\n\n");
		}

		if (syntax.getExample() != null && !syntax.getExample()
			.isEmpty()) {
			md.append("**Example:**\n```properties\n")
				.append(syntax.getExample())
				.append("\n```\n\n");
		}

		if (syntax.getValues() != null && !syntax.getValues()
			.isEmpty()) {
			md.append("**Allowed Values:** `")
				.append(syntax.getValues())
				.append("`\n\n");
		}

		if (syntax.getHelpUrl() != null && !syntax.getHelpUrl()
			.isEmpty()) {
			md.append("[Documentation](")
				.append(syntax.getHelpUrl())
				.append(")\n");
		}

		if (includes != null) {
			md.append("\n")
				.append(includes);
		}

		MarkupContent markup = new MarkupContent(MarkupKind.MARKDOWN, md.toString());
		Range range = new Range(new Position(line, character), new Position(line, character + length));
		return new Hover(markup, range);
	}

	private Hover provideMacroHover(BndDocumentModel model, BndMacroRef macro, String includes) {
		StringBuilder md = new StringBuilder();
		md.append("### Macro `${")
			.append(macro.getName())
			.append("}`\n\n");

		// Live evaluation
		File targetFile = BndWorkspaceManager.getFileFromUri(model.getUri());

		String expression = "${" + macro.getName() + (macro.getArgs() != null ? ";" + macro.getArgs() : "") + "}";
		String evaluated = workspaceManager.expandMacro(expression, targetFile);

		if (evaluated != null && !evaluated.equals(expression)) {
			md.append("**Live Evaluation:**\n```\n")
				.append(evaluated)
				.append("\n```\n\n");
		}

		if (includes != null) {
			md.append(includes);
		}

		MarkupContent markup = new MarkupContent(MarkupKind.MARKDOWN, md.toString());
		Range range = new Range(new Position(macro.getLine(), macro.getCharacter()),
			new Position(macro.getLine(), macro.getCharacter() + macro.getLength()));
		return new Hover(markup, range);
	}
}
