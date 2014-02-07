package aQute.bnd.service.phases;

import java.util.*;

import aQute.service.reporter.*;

public interface PhasesPlugin {
	Comparator<PhasesPlugin> COMPARATOR = new Comparator<PhasesPlugin>() {
		
		public int compare(PhasesPlugin a, PhasesPlugin b) {
			return  a.ranking() > b.ranking() ? 1 : (a.ranking() == b.ranking() ? 0 : -1);
		}
	};
	public enum Options {
		NOCOMPILE, TRACE; // when run in IDE that compiles
	}
	Phases getPhases(Reporter reporter, Phases master, Phases next, EnumSet<Options> options);
	int ranking();
}
