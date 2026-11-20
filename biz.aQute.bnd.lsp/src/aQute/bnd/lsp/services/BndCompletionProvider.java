package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.EE;
import aQute.bnd.help.Syntax;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.properties.BadLocationException;
import aQute.bnd.properties.IRegion;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class BndCompletionProvider {
	private static final Logger			logger	= LoggerFactory.getLogger(BndCompletionProvider.class);
	private static volatile Map<String, String>	builderMacros;

	private final BndWorkspaceManager	workspaceManager;

	public BndCompletionProvider(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public Either<List<CompletionItem>, CompletionList> provideCompletions(BndDocumentModel model,
		CompletionParams params) {
		Position pos = params.getPosition();
		int line = pos.getLine();
		int character = pos.getCharacter();

		List<CompletionItem> items = new ArrayList<>();

		String lineText = "";
		try {
			IRegion lineInfo = model.getLineInformation(line);
			lineText = model.getText()
				.substring(lineInfo.getOffset(), lineInfo.getOffset() + lineInfo.getLength());
		} catch (BadLocationException e) {
			// ignore
		}

		String prefix = character <= lineText.length() ? lineText.substring(0, character) : lineText;

		// 1. Macro completion if typing "${"
		int lastMacroStart = prefix.lastIndexOf("${");
		if (lastMacroStart != -1 && !prefix.substring(lastMacroStart)
			.contains("}")) {
			addMacroCompletions(items, getFileFromUri(model.getUri()));
			return Either.forRight(new CompletionList(false, items));
		}

		// Check if we are typing a header key or a value
		int sepIndex = -1;
		for (int i = 0; i < prefix.length(); i++) {
			char c = prefix.charAt(i);
			if (c == ':' || c == '=') {
				sepIndex = i;
				break;
			}
		}

		File targetFile = getFileFromUri(model.getUri());

		if (sepIndex == -1) {
			// Completing instruction / header name
			addHeaderKeyCompletions(items);
		} else {
			// Completing value / bundle / directive
			String key = prefix.substring(0, sepIndex)
				.trim();
			String valuePrefix = prefix.substring(sepIndex + 1)
				.trim();

			Syntax syntax = syntaxFor(key);
			String clause = lastClausePart(valuePrefix);
			if (syntax != null && clause != null) {
				// after ';' in a clause: attributes, directives and their values
				addParameterCompletions(items, syntax, clause);
				if (!clause.contains("=") && isBundlePathHeader(key)) {
					addVersionCompletions(items, targetFile, valuePrefix.split(";")[0].trim());
				}
			} else if (isBundlePathHeader(key)) {
				addBundleCompletions(items, targetFile, valuePrefix);
			} else if (key.equals("javac.source") || key.equals("javac.target") || key.equals("javac.release")) {
				addJavaVersionCompletions(items);
			} else if (key.equals("-runee")) {
				addRuneeCompletions(items);
			} else if (key.equals("javac.debug")) {
				addBooleanCompletions(items);
			} else {
				addValuePatternCompletions(items, key);
			}
		}

		return Either.forRight(new CompletionList(false, items));
	}

	private void addHeaderKeyCompletions(List<CompletionItem> items) {
		for (Map.Entry<String, Syntax> entry : Syntax.HELP.entrySet()) {
			String key = entry.getKey();
			Syntax s = entry.getValue();

			CompletionItem item = new CompletionItem();
			item.setLabel(key);
			item.setKind(key.startsWith("-") ? CompletionItemKind.Property : CompletionItemKind.Keyword);
			item.setDetail(s.getLead());
			if (s.getExample() != null) {
				item.setDocumentation(s.getExample());
			}
			item.setInsertText(key + ": ");
			items.add(item);
		}
	}

	private void addMacroCompletions(List<CompletionItem> items, File targetFile) {
		Map<String, String> macros = new TreeMap<>(builderMacros());
		Processor processor = targetFile == null ? null : workspaceManager.getProjectForFile(targetFile);
		if (processor == null && targetFile != null) {
			processor = workspaceManager.getWorkspaceForFile(targetFile);
		}
		if (processor != null) {
			try {
				processor.getReplacer()
					.getCommands()
					.forEach(macros::putIfAbsent);
			} catch (Exception e) {
				logger.debug("Cannot list the macros of {}", processor, e);
			}
		}
		macros.forEach((macroName, help) -> {
			CompletionItem item = new CompletionItem();
			item.setLabel("${" + macroName + "}");
			item.setKind(CompletionItemKind.Function);
			item.setDetail("bnd macro");
			if (help != null && !help.isBlank()) {
				item.setDocumentation(help);
			}
			item.setInsertText(macroName);
			items.add(item);
		});
	}

	/**
	 * The macros of bndlib's {@link Builder}, which include those of
	 * {@code Analyzer}, {@code Processor} and {@code Macro}.
	 */
	static Map<String, String> builderMacros() {
		Map<String, String> macros = builderMacros;
		if (macros == null) {
			try (Builder builder = new Builder()) {
				macros = Collections.unmodifiableMap(builder.getReplacer()
					.getCommands());
			} catch (Exception e) {
				logger.debug("Cannot list the bnd macros", e);
				macros = Collections.emptyMap();
			}
			builderMacros = macros;
		}
		return macros;
	}

	/**
	 * The text after the last ';' of the current clause, or null when the
	 * cursor is still in the clause's first part.
	 */
	static String lastClausePart(String valuePrefix) {
		int clauseStart = valuePrefix.lastIndexOf(',') + 1;
		String clause = valuePrefix.substring(clauseStart);
		int semicolon = clause.lastIndexOf(';');
		return semicolon < 0 ? null
			: clause.substring(semicolon + 1)
				.trim();
	}

	private static Syntax syntaxFor(String key) {
		Syntax syntax = Syntax.HELP.get(key);
		if (syntax == null && key.indexOf('.', 1) > 0) {
			syntax = Syntax.HELP.get(key.substring(0, key.indexOf('.', 1)));
		}
		return syntax;
	}

	private static void addParameterCompletions(List<CompletionItem> items, Syntax syntax, String clause) {
		Syntax[] children = syntax.getChildren();
		if (children == null) {
			return;
		}
		int equals = clause.indexOf('=');
		if (equals >= 0) {
			String name = clause.substring(0, equals)
				.trim();
			if (name.endsWith(":")) {
				name = name.substring(0, name.length() - 1);
			}
			for (Syntax child : children) {
				if (parameterName(child).equals(name) && child.getValues() != null) {
					for (String value : child.getValues()
						.split(",")) {
						String trimmed = value.trim();
						if (!trimmed.isEmpty()) {
							CompletionItem item = new CompletionItem(trimmed);
							item.setKind(CompletionItemKind.Value);
							item.setInsertText(trimmed);
							items.add(item);
						}
					}
				}
			}
			return;
		}
		for (Syntax child : children) {
			String header = child.getHeader();
			if (header == null || header.isEmpty()) {
				continue;
			}
			boolean directive = header.endsWith(":");
			String name = parameterName(child);
			CompletionItem item = new CompletionItem(directive ? name + ":=" : name + "=");
			item.setKind(directive ? CompletionItemKind.Keyword : CompletionItemKind.Field);
			item.setDetail(child.getLead());
			if (child.getExample() != null) {
				item.setDocumentation(child.getExample());
			}
			item.setInsertText(directive ? name + ":=" : name + "=");
			items.add(item);
		}
	}

	private static String parameterName(Syntax child) {
		String header = child.getHeader();
		return header.endsWith(":") ? header.substring(0, header.length() - 1) : header;
	}

	private void addBundleCompletions(List<CompletionItem> items, File targetFile, String valuePrefix) {
		Set<String> seen = new HashSet<>();

		// Project dependencies
		if (targetFile != null) {
			Collection<Project> projects = workspaceManager.getAllProjects(targetFile);
			for (Project p : projects) {
				String bsn = p.getName();
				if (seen.add(bsn)) {
					CompletionItem item = new CompletionItem();
					item.setLabel(bsn);
					item.setKind(CompletionItemKind.Module);
					item.setDetail("Workspace project: " + bsn);
					item.setInsertText(bsn);
					items.add(item);
				}
			}

			// Repositories
			List<RepositoryPlugin> repos = workspaceManager.getRepositories(targetFile);
			for (RepositoryPlugin repo : repos) {
				try {
					List<String> bsns = repo.list(null);
					if (bsns != null) {
						for (String bsn : bsns) {
							if (seen.add(bsn)) {
								CompletionItem item = new CompletionItem();
								item.setLabel(bsn);
								item.setKind(CompletionItemKind.Module);
								item.setDetail("Repo (" + repo.getName() + ")");
								item.setInsertText(bsn);
								items.add(item);
							}
						}
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}

		// If value contains a bsn and typing ";version=", suggest versions
		if (valuePrefix.contains(";")) {
			String[] parts = valuePrefix.split(";");
			String lastBsn = parts[0].trim();
			addVersionCompletions(items, targetFile, lastBsn);
		}
	}

	private void addVersionCompletions(List<CompletionItem> items, File targetFile, String bsn) {
		if (targetFile == null || bsn == null || bsn.isEmpty())
			return;

		List<RepositoryPlugin> repos = workspaceManager.getRepositories(targetFile);
		for (RepositoryPlugin repo : repos) {
			try {
				SortedSet<Version> versions = repo.versions(bsn);
				if (versions != null) {
					for (Version v : versions) {
						CompletionItem item = new CompletionItem();
						item.setLabel("version=" + v.toString());
						item.setKind(CompletionItemKind.Value);
						item.setDetail("Exact version from " + repo.getName());
						item.setInsertText("version='" + v.toString() + "'");
						items.add(item);

						// Range snippet: [v, nextMajor)
						CompletionItem rangeItem = new CompletionItem();
						String nextMajor = (v.getMajor() + 1) + ".0.0";
						String range = "[" + v.toString() + "," + nextMajor + ")";
						rangeItem.setLabel("version=\"" + range + "\"");
						rangeItem.setKind(CompletionItemKind.Snippet);
						rangeItem.setDetail("Major version range");
						rangeItem.setInsertText("version='[" + v.toString() + "," + nextMajor + ")'");
						items.add(rangeItem);
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private void addJavaVersionCompletions(List<CompletionItem> items) {
		String[] versions = {
			"1.8", "8", "11", "17", "21", "25"
		};
		for (String v : versions) {
			CompletionItem item = new CompletionItem();
			item.setLabel(v);
			item.setKind(CompletionItemKind.Value);
			item.setInsertText(v);
			items.add(item);
		}
	}

	private void addRuneeCompletions(List<CompletionItem> items) {
		int current = Runtime.version()
			.feature();
		for (EE ee : EE.values()) {
			// Java SE 8 and later up to the running Java version
			if (!"JavaSE".equals(ee.getCapabilityName()) || ee.getRelease() < 8 || ee.getRelease() > current) {
				continue;
			}
			String name = ee.getEEName();
			CompletionItem item = new CompletionItem();
			item.setLabel(name);
			item.setKind(CompletionItemKind.Value);
			item.setInsertText(name);
			items.add(item);
		}
	}

	private void addBooleanCompletions(List<CompletionItem> items) {
		for (String val : new String[] {
			"on", "off", "true", "false"
		}) {
			CompletionItem item = new CompletionItem();
			item.setLabel(val);
			item.setKind(CompletionItemKind.Value);
			item.setInsertText(val);
			items.add(item);
		}
	}

	private void addValuePatternCompletions(List<CompletionItem> items, String key) {
		Syntax s = Syntax.HELP.get(key);
		if (s != null && s.getValues() != null) {
			for (String val : s.getValues()
				.split(",")) {
				String trimmed = val.trim();
				if (!trimmed.isEmpty()) {
					CompletionItem item = new CompletionItem();
					item.setLabel(trimmed);
					item.setKind(CompletionItemKind.Value);
					item.setInsertText(trimmed);
					items.add(item);
				}
			}
		}
	}

	private boolean isBundlePathHeader(String key) {
		String baseKey = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
		return baseKey.equals("-buildpath") || baseKey.equals("-testpath") || baseKey.equals("-runbundles")
			|| baseKey.equals("-runpath") || baseKey.equals("-runblacklist") || baseKey.equals("-runrequires")
			|| baseKey.equals("Require-Bundle");
	}

	private static File getFileFromUri(String uriStr) {
		return BndWorkspaceManager.getFileFromUri(uriStr);
	}
}
