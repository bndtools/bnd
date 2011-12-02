package aQute.bnd.apidiff;

import java.util.*;

import aQute.bnd.service.apidiff.*;
import aQute.libg.version.*;

public class DiffImpl<T extends Def<T>> implements Diff {
	final T					older;
	final T					newer;
	final Collection<Diff>	children	= new ArrayList<Diff>();
	final String			path;
	final T					dominant;
	Delta delta;
	
	DiffImpl(String path, T newer, T older) {
		assert newer == null || older == null;

		this.path = path;
		this.older = older;
		this.newer = newer;
		this.dominant = newer != null ? newer : older;
		
		Map<String, ? extends Def> olderChildren = older.getChildren();
		Map<String, ? extends Def> newerChildren = older.getChildren();
		Set<String>  all = new HashSet<String>(newerChildren.keySet());
		all.addAll( olderChildren.keySet());
		
		for ( String key : all ) {
			@SuppressWarnings("unchecked") DiffImpl diff = new DiffImpl(key,newerChildren.get(key), olderChildren.get(key));
			children.add(diff);
		}
	}

	public Delta getDelta() {
		if ( delta != null)
			return delta;

		if ( newer == null) 
			return delta = Delta.REMOVED;
		
		if ( older == null) 
			return delta = Delta.ADDED;

		delta = newer.compare(older);
		if ( delta != null )
			return delta;


		delta = Delta.UNCHANGED;
		
		for ( Diff diff : children) {
			if ( diff.getDelta() == Delta.REMOVED ) 
				return delta=Delta.MAJOR;
			
			if ( diff.getDelta() == Delta.ADDED ) {
				if ( newer.isAddMajor())
					return delta=Delta.MAJOR;
				
				delta = Delta.MINOR;
			}
			
			if ( delta.compareTo(diff.getDelta()) > 0)
				delta = diff.getDelta();
		}
		return delta;
	}

	public Type getType() {
		return dominant.getType();
	}

	public String getName() {
		return dominant.getName();
	}

	public String getPath() {
		return path;
	}

	public Version getOlderVersion() {
		return older.getVersion();
	}

	public Version getNewerVersion() {
		return newer.getVersion();
	}

	public Collection<Diff> getChildren() {

		return children;
	}

}
