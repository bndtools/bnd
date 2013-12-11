package biz.aQute.bndoc.lib;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.lib.env.*;
import aQute.lib.io.*;

public class Generator extends Base {
	List<File> clean;
	
	
	public Generator(Env main) {
		super(main);
	}

	public Generator() {
	}

	@Override
	public void close() throws IOException {

	}

	protected boolean prepare() throws Exception {
		System.out.printf("prepare %s %s\n", getBase(), getParent());
		if (!super.prepare()) {

			File f = getFile("bndoc.bndoc");
			trace("base file %s", f);
			if (f.isFile()) {
				setProperties(f);
				System.out.println("Read props " + getProperties());
			}

			return false;
		} else
			return true;
	}

	public void generate() throws Exception {
		prepare();
			 
		Header h = new Header(getProperty(LANGUAGES, DEFAULT_LANGUAGE));
		for (Entry<String,Props> language : h.entrySet()) {
			Base env = new Base(this);
			env.putAll(language.getValue());
			env.setProperty(LANGUAGE, language.getKey());

			Header doit = env.getHeader(DO);

			if (doit.isEmpty())
				error("No -do specified");

			for (Entry<String,Props> format : doit.entrySet()) {
				DocumentBuilder db = new DocumentBuilder(env);
				db.putAll(format.getValue());
				db.setProperty(OUTPUT, format.getKey());
				
				db.generate();

				getInfo(db);
			}
		}
	}

	List<File> getClean() {
		if ( clean == null) {
			clean = new ArrayList<File>();
			for ( Entry<String,Props> entry : getHeader(CLEAN).entrySet()) {
				File file = getFile(entry.getKey());
				clean.add(file);
			}
		}
		return clean;
	}
	
	public void clean() {
		for ( File f : getClean()) {
			if ( f.exists()) {
				IO.delete(f);
				if ( f.exists())
					error("Could not delete %s", f);
			}
		}
	}
}
