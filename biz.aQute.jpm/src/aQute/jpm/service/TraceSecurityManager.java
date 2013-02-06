package aQute.jpm.service;

import java.security.*;
import java.util.*;

public class TraceSecurityManager extends SecurityManager {
	final HashSet<Permission>	had	= new HashSet<Permission>();

	public TraceSecurityManager() {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Set<Permission> hadx;
				synchronized (TraceSecurityManager.this) {
					 hadx = new HashSet<Permission>(TraceSecurityManager.this.had);
				}
				Set<Permission> implied = new HashSet<Permission>();

				for (Permission smaller : hadx) {
					for (Permission larger : hadx) {
						if (smaller != larger && larger.implies(smaller))
							implied.add(smaller);
					}
				}
				hadx.removeAll(implied);

				ArrayList<Permission> sorted = new ArrayList<Permission>(hadx);
				Collections.sort(sorted, new Comparator<Permission>() {

					public int compare(Permission a, Permission b) {
						if (a.getClass() == b.getClass()) {
							if (a.getName().equals(b.getName())) {
								return a.getActions().compareTo(b.getActions());
							} else
								return a.getName().compareTo(b.getName());
						} else
							return shorten(a.getClass().getName()).compareTo(shorten(b.getClass().getName()));
					}
				});
				for (Permission p : sorted) {
					System.err.println(shorten(p.getClass().getName()) + ":" + p.getName() + ":" + p.getActions());
				}
			}

			String shorten(String name) {
				int n = name.lastIndexOf('.');
				if (n < 0)
					return name;

				return name.substring(n + 1);
			}
		});
	}

	public synchronized void checkPermission(Permission perm) {
		if ( perm.getClass() == AllPermission.class)
			throw new SecurityException();
		
		if (had.contains(perm))
			return;

		had.add(perm);
	}

	public void checkPermission(Permission perm, Object o) {
		checkPermission(perm);
	}
}
