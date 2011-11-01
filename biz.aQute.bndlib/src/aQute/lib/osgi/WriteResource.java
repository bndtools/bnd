package aQute.lib.osgi;

import java.io.*;

public abstract class WriteResource implements Resource {
	long 	lastModified;
	String	extra;

	public InputStream openInputStream() throws Exception {
	    PipedInputStream pin = new PipedInputStream();
	    final PipedOutputStream pout = new PipedOutputStream(pin);
	    Thread t = new Thread() {
	        public void run() {
	            try {
                    write(pout);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        pout.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
	        }
	    };
	    t.start();
	    return pin;
	}

	public abstract void write(OutputStream out) throws IOException, Exception;
	
	public abstract long lastModified();

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
