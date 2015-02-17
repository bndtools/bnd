package aQute.jpm.remote.lib;

import java.io.*;
import java.net.*;

import aQute.libg.comlink.*;
import aQute.libg.remote.sink.*;

public class SlaveImpl extends RemoteSink implements Slave {

	private Link<Slave,Master>	link;
	RemoteAccess				remoteAccess;
	File						areasDir;

	public SlaveImpl(RemoteAccess remoteAccess, File root, Socket socket) throws Exception {
		super(root);
		this.remoteAccess = remoteAccess;
		areasDir = new File(root, "area");
		areasDir.mkdirs();

		link = new Link<Slave,Master>(Master.class, this, socket);
		setSources(link.getRemote());
	}

	public void open() {
		link.open();
	}

	public void close() throws IOException {
		link.close();
	}

}
