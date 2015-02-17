package aQute.jpm.remote.lib;

import java.io.*;

import aQute.libg.remote.*;

public interface Master extends Source, Closeable {

	Slave getSlave();

}
