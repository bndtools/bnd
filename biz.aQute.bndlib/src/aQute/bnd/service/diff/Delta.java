package aQute.bnd.service.diff;

public enum Delta {
	
	// ORDER IS IMPORTANT FOR TRANSITIONS TABLE!
	
	IGNORED,                                  // for all
	UNCHANGED, CHANGED, MICRO, MINOR, MAJOR,  // content 
	REMOVED, ADD_MINOR, ADD_MAJOR;            // structural
	

	public boolean isStructural() {
		return this.ordinal() >= REMOVED.ordinal();
	}
	
}
