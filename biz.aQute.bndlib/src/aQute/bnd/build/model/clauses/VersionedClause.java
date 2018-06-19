package aQute.bnd.build.model.clauses;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.version.VersionRange;

public class VersionedClause extends HeaderClause implements Cloneable {

	public static final Comparator<VersionedClause> COMPARATOR = new Comparator<VersionedClause>() {

		@Override
		public int compare(VersionedClause o1, VersionedClause o2) {
			int n = o1.getName().compareTo(o2.getName());
			if (n != 0)
				return n;

			try {
				if (Objects.equals(o1.getVersionRange(), o2.getVersionRange()))
					return 0;

				if ("latest".equals(o1.getVersionRange())) {
					return 1;
				}
				if ("latest".equals(o2.getVersionRange())) {
					return -1;
				}

				VersionRange v1 = new VersionRange(o1.getVersionRange());
				VersionRange v2 = new VersionRange(o2.getVersionRange());
				return v1.getLow().compareTo(v2.getLow());
			} catch (Exception e) {
				return o1.getVersionRange().compareTo(o2.getVersionRange());
			}
		}
	};

	public VersionedClause(String name, Attrs attribs) {
		super(name, attribs);
	}

	public String getVersionRange() {
		return attribs.get(Constants.VERSION_ATTRIBUTE);
	}

	public void setVersionRange(String versionRangeString) {
		attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeString);
	}

	@Override
	public VersionedClause clone() {
		VersionedClause clone = (VersionedClause) super.clone();
		clone.name = this.name;
		clone.attribs = new Attrs(this.attribs);
		return clone;
	}

	public static VersionedClause error(String msg) {
		Attrs a = new Attrs();
		a.put("PARSE ERROR", msg);
		return new VersionedClause("ERROR", a);
	}

	/**
	 * <pre>
	 * pred replaceOrAdd[ 
	 * 	next : VersionedClause, path, path' : set VersionedClause ] {
	 * 		let overlap = { p: path | p.name = next.name } | { 
	 * 			some overlap 
	 * 			implies { 
	 * 					next.version > overlap.version 
	 * 					implies path' = path-t+next }
	 * 					else 	path' = path
	 *  			} else 
	 *				path' = path + next 
	 *		}
	 * }
	 * </pre>
	 * 
	 * @param path A path of Version Clauses
	 * @param next the one to add to the path
	 * @return true if this was replaced or added, false if ignored because a
	 *         higher version was available
	 */
	public static boolean replaceOrAdd(List<VersionedClause> path, VersionedClause next) {
		for (int i = 0; i < path.size(); i++) {
			VersionedClause previous = path.get(i);
			if (previous.getName().equals(next.getName())) {
				try {
					if ("latest".equals(previous.getVersionRange())) {
						return false;
					}
					if ("latest".equals(next.getVersionRange())) {
						path.set(i, next);
						return true;
					}
					VersionRange previousVersionRange = VersionRange.parseVersionRange(previous.getVersionRange());
					VersionRange nextVersionRange = VersionRange.parseVersionRange(previous.getVersionRange());
					aQute.bnd.version.Version previousFloor = previousVersionRange.getLow();
					aQute.bnd.version.Version nextFloor = nextVersionRange.getLow();
					if (nextFloor.compareTo(previousFloor) > 0) {
						path.set(i, next);
						return true;
					}
				} catch (Exception e) {
					// ignore, use older one. We can't fix it all ...
				}
				return false;
			}
		}
		path.add(next);
		return true;
	}

	/**
	 * Remove version clauses from path when they have the given name
	 * 
	 * <pre>
	 * 	pred removeFromPath[ name : String, path, path' : set VersionedClause ] {
	 * 		path' = { vc : path | vc.name != name }
	 *  }
	 * </pre>
	 */

	public static int removeFromPath(List<VersionedClause> path, String nm) {
		int n = 0;
		for (Iterator<VersionedClause> it = path.iterator(); it.hasNext();) {
			VersionedClause vc = it.next();

			if (nm.equals(vc.getName())) {
				it.remove();
				n++;
			}
		}
		return n;
	}

	public static List<VersionedClause> from(Parameters bp) {
		ArrayList<VersionedClause> list = new ArrayList<>();
		for (Map.Entry<String,Attrs> e : bp.entrySet()) {
			VersionedClause vc = new VersionedClause(e.getKey(), e.getValue());
			list.add(vc);
		}
		return list;
	}

}
