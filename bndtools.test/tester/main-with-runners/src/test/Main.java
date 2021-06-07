package test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		final String loc = "file:\\C:\\workspace\\bnd\\test\\generated\\test.jar";
		System.err.println("loc: " + loc);
		
		String rep = loc.replaceFirst("file:[\\\\/]", "");
		System.err.println("rep: " + rep);
		Path p = Paths.get(rep);
		System.err.println("path: " + p);
		System.err.println("file: "+  p.toFile());
	}

}
