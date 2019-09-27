package test.classforname;

public class ClassForName {

	public static void main(String[] args) throws ClassNotFoundException {
		Class<?> c = Class.forName("javax.swing.Box");
		System.err.println(c);
	}
}
