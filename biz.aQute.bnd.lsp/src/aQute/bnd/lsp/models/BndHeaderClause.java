package aQute.bnd.lsp.models;

import java.util.Collections;
import java.util.Map;

import aQute.bnd.header.Attrs;

public class BndHeaderClause {
	private final String				name;
	private final Attrs					attrs;
	private final int					offset;
	private final int					length;
	private final int					startLine;
	private final int					startChar;
	private final int					endLine;
	private final int					endChar;
	private final Map<String, Integer>	attrOffsets;

	public BndHeaderClause(String name, Attrs attrs, int offset, int length, int startLine, int startChar, int endLine,
		int endChar, Map<String, Integer> attrOffsets) {
		this.name = name;
		this.attrs = attrs != null ? attrs : new Attrs();
		this.offset = offset;
		this.length = length;
		this.startLine = startLine;
		this.startChar = startChar;
		this.endLine = endLine;
		this.endChar = endChar;
		this.attrOffsets = attrOffsets != null ? attrOffsets : Collections.emptyMap();
	}

	public String getName() {
		return name;
	}

	public Attrs getAttrs() {
		return attrs;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartChar() {
		return startChar;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getEndChar() {
		return endChar;
	}

	public Map<String, Integer> getAttrOffsets() {
		return attrOffsets;
	}
}
