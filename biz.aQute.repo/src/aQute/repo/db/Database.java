package aQute.repo.db;

import java.io.*;

import aQute.repo.services.*;

public class Database {
	final File			root;
	final Platform	platform;

	public Database(Platform platform) {
		this.platform = platform;
		root = platform.getRoot();
	}
	
	

}
