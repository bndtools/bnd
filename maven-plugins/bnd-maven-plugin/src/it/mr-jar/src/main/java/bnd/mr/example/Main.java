package bnd.mr.example;

import java.io.IOException;
import java.net.URL;

public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("Please specify at laest one file to fetch!");
			System.exit(1);
		}
		HttpClient client = new HttpClient();
		for (String arg : args) {
			byte[] bytes = client.fetchBytes(new URL(arg));
			System.out.println("URL " + arg + " has provided " + bytes.length + " bytes!");
		}
	}
}
