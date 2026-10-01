package aQute.bnd.lsp.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BndHeaderEntry {
	private final String				key;
	private final String				value;
	private final int					startLine;
	private final int					endLine;
	private final int					keyOffset;
	private final int					keyLength;
	private final int					valueOffset;
	private final int					valueLength;
	private final List<BndHeaderClause>	clauses;
	private final List<BndMacroRef>		macros;

	public BndHeaderEntry(String key, String value, int startLine, int endLine, int keyOffset, int keyLength,
		int valueOffset, int valueLength, List<BndHeaderClause> clauses, List<BndMacroRef> macros) {
		this.key = key;
		this.value = value;
		this.startLine = startLine;
		this.endLine = endLine;
		this.keyOffset = keyOffset;
		this.keyLength = keyLength;
		this.valueOffset = valueOffset;
		this.valueLength = valueLength;
		this.clauses = clauses != null ? Collections.unmodifiableList(new ArrayList<>(clauses))
			: Collections.emptyList();
		this.macros = macros != null ? Collections.unmodifiableList(new ArrayList<>(macros)) : Collections.emptyList();
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getKeyOffset() {
		return keyOffset;
	}

	public int getKeyLength() {
		return keyLength;
	}

	public int getValueOffset() {
		return valueOffset;
	}

	public int getValueLength() {
		return valueLength;
	}

	public List<BndHeaderClause> getClauses() {
		return clauses;
	}

	public List<BndMacroRef> getMacros() {
		return macros;
	}

	public boolean isInstruction() {
		return key != null && key.startsWith("-");
	}
}
