package aQute.jpm.remote.lib;

import java.io.*;
import java.net.*;

import aQute.libg.comlink.*;
import aQute.libg.remote.source.*;

public class MasterImpl extends RemoteSource implements Master, Closeable {

	RemoteAccess				remoteAccess;
	private Link<Master,Slave>	link;
	private File				cwd;
	private String				areaId;

	public MasterImpl(RemoteAccess remoteAccess, Socket socket, File cwd, String areaId) throws IOException {
		this.remoteAccess = remoteAccess;
		this.cwd = cwd;
		this.areaId = areaId;
		link = new Link<Master,Slave>(Slave.class, this, socket);
	}

	public void open() {
		link.start();
		super.open(link.getRemote(), cwd, areaId == null ? "test" : areaId);
	}

	public Slave getSlave() {
		return link.getRemote();
	}

	public void close() throws IOException {
		super.close();
		link.close();
	}

}
