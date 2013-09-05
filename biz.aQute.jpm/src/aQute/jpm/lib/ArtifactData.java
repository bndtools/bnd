package aQute.jpm.lib;

import java.net.*;

import aQute.service.library.*;
import aQute.struct.*;

public class ArtifactData extends struct {
	public byte[]	sha;
	public long		time	= System.currentTimeMillis();
	public String	error;
	public URI		url;
	boolean			busy	= false;
	public String	file;
	public String	name;
	public boolean	local;
	public Coordinate	coordinate;

	synchronized void done() {
		busy = false;
		notifyAll();
	}

	public synchronized void sync() throws InterruptedException {
		while (busy)
			wait();
	}

}
