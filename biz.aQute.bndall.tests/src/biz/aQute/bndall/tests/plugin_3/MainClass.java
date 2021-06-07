package biz.aQute.bndall.tests.plugin_3;

import java.io.File;

public class MainClass {

	public static void main(String... args) throws Exception {
		while (System.in.available() > 0) {
			int b = System.in.read();
			if (b < 0)
				break;

			System.out.write(b);
		}

		System.out.println(new File("").getAbsolutePath());

	}

}
