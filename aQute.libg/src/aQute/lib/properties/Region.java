package aQute.lib.properties;

public class Region implements IRegion {

	private final int	offset;
	private final int	length;

	public Region(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	public int getOffset() {
		return offset;

	}

	public int getLength() {
		return length;
	}

}
