package aQute.lib.remote;

import java.io.*;

public class Foo {

	public static void main(String[] args) throws IOException {
		System.out.println("Hooray!");
		File f = new File("test");
		FileOutputStream out = new FileOutputStream(f);
		out.write("foo".getBytes());
		out.close();
	}

}
