package aQute.bnd.signatures;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;

@SuppressWarnings("unused")
public class TypeUser1<LR extends LogReaderService, SR extends ServiceReference<? extends LR>> {

	LR										flr;
	SR										fsr;
	LR[]									flra1;
	SR[]									fsra1;
	LR[][]									flra2;
	SR[][]									fsra2;
	int[]									inta1;
	long[][]								longa2;
	LogReaderService[]						lra1;

	List<LR>								lrlist;
	Collection<? extends LogReaderService>	lrcoll;
	Collection<? super LogReaderService>	lrcoll2;

	public void bindLR(LR lr) {}

	public void bindSR(SR sr) {}

	public <SO extends ServiceObjects<? extends LR>, M extends Map<String, Object>> M bindSO(SO cso) {
		return null;
	}

}
