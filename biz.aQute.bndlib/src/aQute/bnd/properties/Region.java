package aQute.bnd.properties;

public class Region implements IRegion {

	private final int	offset;
	private final int	length;

	public Region(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	@Override
	public int getOffset() {
		return offset;

	}

	@Override
	public int getLength() {
		return length;
	}

}
