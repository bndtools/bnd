package aQute.jpm.lib;

import java.util.*;

import aQute.struct.*;

public class ArtifactData extends struct {
	public byte[]		sha;
	public long			time			= System.currentTimeMillis();
	public CommandData	command;
	public ServiceData	service;
	public String		error;

	public String		name;
	public String		mainClass;
	public String		description;
	public List<String>	dependencies	= new ArrayList<String>();		// shas

	boolean				busy			= false;
	public String		file;
	public String	coordinates;

	synchronized void done() {
		busy = false;
		notifyAll();
	}

	public synchronized void sync() throws InterruptedException {
		while(busy)
			wait();
	}

}
