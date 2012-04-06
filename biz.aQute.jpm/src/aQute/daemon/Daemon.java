package aQute.daemon;

public class Daemon {

	public static void  main(String args[]) throws Exception {
		int n = 1;
		while ( true ) {
			System.out.println("count " + n++);
			Thread.sleep(3000);
		}
	}
}
