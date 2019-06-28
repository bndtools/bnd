package aQute.lib.dtoformatter;

public class StringCell implements Cell {

	public final String[]	value;
	public final int		width;
	final Object			original;

	public StringCell(String label, Object original) {
		this.original = original;
		if (label.length() > 80) {
			label = label.replaceAll("([,])", "$1\n");
		}
		this.value = label.trim()
			.split("\\s*\r?\n\\s*");
		int w = 0;
		for (String l : value) {
			if (l.length() > w)
				w = l.length();
		}
		this.width = w;
	}

	@Override
	public int width() {
		return width + 2;
	}

	@Override
	public int height() {
		return value.length + 2;
	}

	@Override
	public String toString() {
		return String.join("\n", value);
	}

	@Override
	public Object original() {
		return original;
	}

}
