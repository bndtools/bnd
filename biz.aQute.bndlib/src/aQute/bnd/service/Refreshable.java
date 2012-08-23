package aQute.bnd.service;

import java.io.*;

public interface Refreshable {
	boolean refresh() throws Exception;

	File getRoot();
}
