package aQute.jpm.remote.lib;

import java.io.*;

import junit.framework.*;
import aQute.libg.comlink.*;

public class LinkTest extends TestCase {

	interface Remote {
		int foo();
	}

	interface Local {
		int bar();
	}

	public static class LocalImpl extends Link<Local,Remote> implements Local {

		public LocalImpl(Class<Remote> type, InputStream in, OutputStream out) {
			super(type, null, in, out);
		}

		public int bar() {
			return 42;
		}

	}

	public static class RemoteImpl extends Link<Remote,Local> implements Remote {

		public RemoteImpl(Class<Local> type, InputStream in, OutputStream out) {
			super(type, null, in, out);
		}

		public int foo() {
			return -42;
		}
	}

	public void testSimple() throws IOException {
		// PipedInputStream remoteIn = new PipedInputStream();
		// PipedOutputStream localOut = new PipedOutputStream(remoteIn);
		// PipedInputStream localIn = new PipedInputStream();
		// PipedOutputStream remoteOut = new PipedOutputStream(localIn);
		// LocalImpl localImpl = new LocalImpl(Remote.class, localIn, localOut);
		// RemoteImpl remoteImpl = new RemoteImpl(Local.class, remoteIn,
		// remoteOut);
		//
		// localImpl.open();
		// remoteImpl.open();
		//
		// assertEquals(-42, localImpl.getRemote().foo());
		// assertEquals(42, remoteImpl.getRemote().bar());
	}
}
