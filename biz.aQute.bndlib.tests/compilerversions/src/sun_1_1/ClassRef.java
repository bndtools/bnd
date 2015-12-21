package sun_1_1;

public class ClassRef {
	class Inner {
	};
	static {
		System.out.println(Inner.class);
	}
	
	public static void main() {
		System.out.println(javax.swing.Box.class);
	}
}