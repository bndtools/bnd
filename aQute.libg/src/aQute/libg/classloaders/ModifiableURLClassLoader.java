package aQute.libg.classloaders;

import java.net.URL;
import java.net.URLClassLoader;

public class ModifiableURLClassLoader extends URLClassLoader {

	public ModifiableURLClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

}
