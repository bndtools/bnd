package aQute.bnd.lsp.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import aQute.bnd.lsp.Constants;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.osgi.Processor;

/**
 * Builds quick fixes for diagnostics that carry a known code: property
 * conflicts, invalid bundle activators, and baseline bundle version errors.
 * Edits are computed against the current editor text, never the saved file.
 */
public class BndQuickFixProvider {
	private static final Pattern	VERSION_WITH_QUALIFIER	= Pattern
		.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.([-.${}\\w]+)");

	private final BndWorkspaceManager workspaceManager;

	public BndQuickFixProvider(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public List<CodeAction> provideQuickFixes(BndDocumentModel model, File file, List<Diagnostic> diagnostics) {
		List<CodeAction> actions = new ArrayList<>();
		if (diagnostics == null)
			return actions;
		for (Diagnostic diagnostic : diagnostics) {
			String code = diagnostic.getCode() == null ? null
				: diagnostic.getCode()
					.isLeft() ? diagnostic.getCode()
						.getLeft() : null;
			if (code == null)
				continue;
			JsonObject data = data(diagnostic.getData());
			switch (code) {
				case Constants.DIAGNOSTIC_CODE_PROPERTY_CONFLICT -> propertyConflictFixes(model, file, diagnostic, data,
					actions);
				case Constants.DIAGNOSTIC_CODE_BUNDLE_ACTIVATOR -> activatorFixes(model, diagnostic, actions);
				case Constants.DIAGNOSTIC_CODE_BUNDLE_VERSION -> versionFixes(model, diagnostic, data, actions);
				default -> {
				}
			}
		}
		return actions;
	}

	private void propertyConflictFixes(BndDocumentModel model, File file, Diagnostic diagnostic, JsonObject data,
		List<CodeAction> actions) {
		if (data == null || !data.has("key"))
			return;
		String key = data.get("key")
			.getAsString();
		boolean duplicate = "DUPLICATE".equals(string(data, "kind"));
		int start = data.has("start") ? data.get("start")
			.getAsInt() : -1;
		int end = data.has("end") ? data.get("end")
			.getAsInt() : -1;
		int line = start >= 0 ? model.offsetToPosition(start)
			.getLine()
			: diagnostic.getRange()
				.getStart()
				.getLine();
		BndHeaderEntry entry = model.getEntryAt(line);
		if (entry == null || !entry.getKey()
			.equals(key))
			return;

		if (data.has("mergeable") && data.get("mergeable")
			.getAsBoolean() && start >= 0 && end > start) {
			String replacement = uniqueMergedKey(key, file, usedKeys(model, file));
			Range keyRange = new Range(model.offsetToPosition(start), model.offsetToPosition(end));
			actions.add(action("Rename to `" + replacement + "` to merge values", model.getUri(),
				new TextEdit(keyRange, replacement), diagnostic));
		}

		String title = duplicate ? "Remove this definition of `" + key + "`"
			: "Remove shadowed definition of `" + key + "`";
		actions.add(action(title, model.getUri(), deleteEntry(model, entry), diagnostic));
	}

	private void activatorFixes(BndDocumentModel model, Diagnostic diagnostic, List<CodeAction> actions) {
		BndHeaderEntry entry = findEntry(model, aQute.bnd.osgi.Constants.BUNDLE_ACTIVATOR);
		if (entry == null)
			return;
		actions.add(action("Remove the " + aQute.bnd.osgi.Constants.BUNDLE_ACTIVATOR + " header", model.getUri(),
			deleteEntry(model, entry), diagnostic));
	}

	private void versionFixes(BndDocumentModel model, Diagnostic diagnostic, JsonObject data,
		List<CodeAction> actions) {
		String suggested = string(data, "suggestedVersion");
		if (suggested == null)
			return;
		BndHeaderEntry entry = findEntry(model, aQute.bnd.osgi.Constants.BUNDLE_VERSION);
		if (entry == null || entry.getStartLine() != entry.getEndLine())
			return;
		String current = entry.getValue()
			.trim();
		Matcher m = VERSION_WITH_QUALIFIER.matcher(current);
		if (m.matches()) {
			suggested = suggested + "." + m.group(4);
		}
		if (suggested.equals(current))
			return;
		int valueStart = entry.getValueOffset();
		int valueEnd;
		try {
			var line = model.getLineInformation(entry.getEndLine());
			valueEnd = line.getOffset() + line.getLength();
		} catch (Exception e) {
			return;
		}
		String text = model.getText();
		// value offset follows the separator; replace only the version literal
		while (valueStart < valueEnd && Character.isWhitespace(text.charAt(valueStart)))
			valueStart++;
		while (valueEnd > valueStart && Character.isWhitespace(text.charAt(valueEnd - 1)))
			valueEnd--;
		Range range = new Range(model.offsetToPosition(valueStart), model.offsetToPosition(valueEnd));
		actions.add(action("Change " + aQute.bnd.osgi.Constants.BUNDLE_VERSION + " to " + suggested,
			model.getUri(), new TextEdit(range, suggested), diagnostic));
	}

	/** Deletes the entry's lines including continuation lines and the line break. */
	static TextEdit deleteEntry(BndDocumentModel model, BndHeaderEntry entry) {
		Position start = new Position(entry.getStartLine(), 0);
		Position end;
		if (entry.getEndLine() + 1 < model.getLineCount()) {
			end = new Position(entry.getEndLine() + 1, 0);
		} else {
			int length = 0;
			try {
				length = model.getLineInformation(entry.getEndLine())
					.getLength();
			} catch (Exception e) {
				// keep zero
			}
			end = new Position(entry.getEndLine(), length);
		}
		return new TextEdit(new Range(start, end), "");
	}

	static String uniqueMergedKey(String key, File file, Set<String> used) {
		String base = suffixBase(file);
		String replacement = key + "." + base;
		int suffix = 2;
		while (used.contains(replacement))
			replacement = key + "." + base + "-" + suffix++;
		return replacement;
	}

	static String suffixBase(File file) {
		String name = file.getName()
			.replaceFirst("\\.bnd(?:run)?$", "")
			.replaceAll("[^A-Za-z0-9._-]", "-");
		return name.isEmpty() ? "local" : name;
	}

	private Set<String> usedKeys(BndDocumentModel model, File file) {
		Set<String> used = new HashSet<>();
		for (BndHeaderEntry entry : model.getHeaderEntries())
			used.add(entry.getKey());
		try {
			Processor processor = workspaceManager.getProcessorForFile(file);
			if (processor != null)
				used.addAll(processor.getProperties()
					.stringPropertyNames());
		} catch (Exception e) {
			// best effort: document keys are sufficient to avoid local clashes
		}
		return used;
	}

	private static BndHeaderEntry findEntry(BndDocumentModel model, String key) {
		for (BndHeaderEntry entry : model.getHeaderEntries()) {
			if (entry.getKey()
				.equals(key))
				return entry;
		}
		return null;
	}

	private static CodeAction action(String title, String uri, TextEdit edit, Diagnostic diagnostic) {
		CodeAction action = new CodeAction(title);
		action.setKind(CodeActionKind.QuickFix);
		action.setDiagnostics(Collections.singletonList(diagnostic));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit();
		workspaceEdit.setChanges(Map.of(uri, Collections.singletonList(edit)));
		action.setEdit(workspaceEdit);
		return action;
	}

	private static String string(JsonObject data, String name) {
		return data != null && data.has(name) && !data.get(name)
			.isJsonNull() ? data.get(name)
				.getAsString() : null;
	}

	/** Diagnostic data is a JsonObject locally and a JsonElement after a protocol round trip. */
	private static JsonObject data(Object data) {
		if (data instanceof JsonObject object)
			return object;
		if (data instanceof JsonElement element && element.isJsonObject())
			return element.getAsJsonObject();
		return null;
	}
}
