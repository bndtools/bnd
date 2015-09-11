package aQute.bnd.service.diff;

public interface Tree extends Comparable<Tree> {

	public class Data {
		public String	name;
		public Type		type		= Type.METHOD;
		public Delta	add			= Delta.MINOR;
		public Delta	rem			= Delta.MAJOR;
		public Data[]	children	= null;
		public String	comment		= null;
	}

	Data serialize();

	Tree[] getChildren();

	String getName();

	Type getType();

	Delta ifAdded();

	Delta ifRemoved();

	Diff diff(Tree older);

	Tree get(String name);
}
