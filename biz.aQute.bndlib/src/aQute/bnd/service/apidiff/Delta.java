package aQute.bnd.service.apidiff;

public enum Delta {
	UNCHANGED, CHANGED, MICRO, MINOR, MAJOR, ADDED, REMOVED;
	
	public Delta aggregate(Delta childDelta) {
		if ( childDelta == UNCHANGED || childDelta == this)
			return this;
		
		if ( childDelta == REMOVED ) {
			return MAJOR;
		}
		if ( childDelta == ADDED ) {
			return MINOR;
		}
		
		int compareTo = this.compareTo(childDelta);
		if ( compareTo > 0)
			return this;
		else
			return childDelta;
	}
}
