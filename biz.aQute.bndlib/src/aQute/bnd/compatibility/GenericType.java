package aQute.bnd.compatibility;

public class GenericType {
	public GenericType(@SuppressWarnings("unused") Class<Object> class1) {
		// TODO Auto-generated constructor stub
	}

	final static GenericType	EMPTY[]	= new GenericType[0];
	Scope						reference;
	GenericType[]				a;
	GenericType[]				b;
	int							array;

	Scope						scope;

	static public class GenericWildcard extends GenericType {

		public GenericWildcard(Class<Object> class1) {
			super(class1);
			// TODO Auto-generated constructor stub
		}

	}

	static public class GenericArray extends GenericType {

		public GenericArray(Class<Object> class1) {
			super(class1);
			// TODO Auto-generated constructor stub
		}

	}

}
