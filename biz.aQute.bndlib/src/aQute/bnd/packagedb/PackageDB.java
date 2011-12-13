package aQute.bnd.packagedb;

import java.util.*;

import aQute.lib.deployer.*;
import aQute.lib.osgi.*;

public class PackageDB extends Processor {
	FileRepo		repository;
	
	public boolean checkPackageVersions(Jar source) {
		for ( String dir : source.getDirectories().keySet()) {
			if ( isPackage(dir))
				checkPackageVersion( dir, source );
		}
		//TODO
		return false;
	}
	
	private void checkPackageVersion(String packageName, Jar source) {
		
	}


	public void update(Jar jar) {
	}
	
	private boolean isPackage(String packageName) {
		return packageName.length() > 0 && Character.isLowerCase(packageName.charAt(0));
	}
	

}
