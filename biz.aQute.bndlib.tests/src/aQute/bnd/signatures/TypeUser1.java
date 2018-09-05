package aQute.bnd.signatures;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.log.LogReaderService;

@SuppressWarnings("unused")
public class TypeUser1<LR extends LogReaderService, SR extends ServiceReference<? extends LR>> {

	private LR										flr;
	private SR										fsr;
	private LR[]									flra1;
	private SR[]									fsra1;
	private LR[][]									flra2;
	private SR[][]									fsra2;
	private int[]									inta1;
	private long[][]								longa2;
	private LogReaderService[]						lra1;

	private List<LR>								lrlist;
	private Collection<? extends LogReaderService>	lrcoll;
	private Collection<? super LogReaderService>	lrcoll2;

	public void bindLR(LR lr) {}

	public void bindSR(SR sr) {}

	public <CSO extends ComponentServiceObjects<? extends LR>, M extends Map<String, Object>> M bindCSO(CSO cso) {
		return null;
	}

}
