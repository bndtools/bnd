package aQute.bnd.lsp.models;

public class BndToken {
	private final int			offset;
	private final int			length;
	private final int			line;
	private final int			character;
	private final BndTokenType	type;
	private final String		text;

	public BndToken(int offset, int length, int line, int character, BndTokenType type, String text) {
		this.offset = offset;
		this.length = length;
		this.line = line;
		this.character = character;
		this.type = type;
		this.text = text;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public int getLine() {
		return line;
	}

	public int getCharacter() {
		return character;
	}

	public BndTokenType getType() {
		return type;
	}

	public String getText() {
		return text;
	}

	public boolean contains(int targetLine, int targetChar) {
		if (targetLine != line)
			return false;
		return targetChar >= character && targetChar <= (character + length);
	}
}
