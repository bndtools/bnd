package aQute.bnd.lsp.models;

public class BndMacroRef {
	private final String	name;
	private final String	args;
	private final int		offset;
	private final int		length;
	private final int		line;
	private final int		character;

	public BndMacroRef(String name, String args, int offset, int length, int line, int character) {
		this.name = name;
		this.args = args;
		this.offset = offset;
		this.length = length;
		this.line = line;
		this.character = character;
	}

	public String getName() {
		return name;
	}

	public String getArgs() {
		return args;
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

	public boolean contains(int targetLine, int targetChar) {
		if (targetLine != line)
			return false;
		return targetChar >= character && targetChar <= (character + length);
	}
}
