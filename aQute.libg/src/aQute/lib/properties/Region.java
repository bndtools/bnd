package aQute.lib.properties;

public class Region implements IRegion {

	private final int	length;
	private final int	offset;

	public Region(int length, int offset) {
		this.length = length;
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}

	public int getOffset() {
		return offset;
	}
}
