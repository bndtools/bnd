package aQute.lib.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Foo {

	public static void main(String[] args) throws IOException {
		System.out.println("Hooray!");
		File f = new File("test");
		FileOutputStream out = new FileOutputStream(f);
		out.write("foo".getBytes());
		out.close();
	}

}
