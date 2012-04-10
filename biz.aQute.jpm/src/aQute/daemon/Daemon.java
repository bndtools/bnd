package aQute.daemon;

public class Daemon {

	public static void  main(String args[]) throws Exception {
		int n = 1;
		int inc = 1;
		if ( args.length > 0)
			inc = Integer.parseInt(args[0]);
		if ( args.length > 1)
			n = Integer.parseInt(args[1]);
		
		while ( true ) {
			System.out.println("count " + n);
			n += inc;
			Thread.sleep(3000);
		}
	}
}
