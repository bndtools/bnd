package eclipse_1_1;

public class ClassRef {
	class Inner {
	};
	static {
		System.out.println(Inner.class);
	}
	
	public static void main() {
		int a= 3;
		a++;
		switch( a ) {
		case 1: System.out.println(javax.swing.event.ChangeEvent.class); break;
		case 2: System.out.println(javax.swing.event.ChangeEvent.class); break;
		default:
		}
		System.out.println(javax.swing.Box.class);
	}
}