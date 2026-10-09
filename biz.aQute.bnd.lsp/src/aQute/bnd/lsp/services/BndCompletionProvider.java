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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import aQute.bnd.build.Project;
import aQute.bnd.help.Syntax;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.properties.BadLocationException;
import aQute.bnd.properties.IRegion;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class BndCompletionProvider {
	private static final String[]		COMMON_MACROS	= {
		"bsn", "version", "range", "filter", "tstamp", "def", "if", "list", "replace", "join", "sort", "uniq", "repo",
		"classes", "packages", "exports", "imports", "dir", "basename", "isfile", "isdir", "pathseparator"
	};

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
			addMacroCompletions(items);
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

			if (isBundlePathHeader(key)) {
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

	private void addMacroCompletions(List<CompletionItem> items) {
		for (String macroName : COMMON_MACROS) {
			CompletionItem item = new CompletionItem();
			item.setLabel("${" + macroName + "}");
			item.setKind(CompletionItemKind.Function);
			item.setDetail("bnd macro");
			item.setInsertText(macroName);
			items.add(item);
		}
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
		String[] ees = {
			"JavaSE-1.8", "JavaSE-11", "JavaSE-17", "JavaSE-21"
		};
		for (String ee : ees) {
			CompletionItem item = new CompletionItem();
			item.setLabel(ee);
			item.setKind(CompletionItemKind.Value);
			item.setInsertText(ee);
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
