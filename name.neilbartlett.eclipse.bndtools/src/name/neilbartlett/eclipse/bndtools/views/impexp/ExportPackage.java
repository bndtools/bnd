package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import aQute.lib.osgi.Constants;

public class ExportPackage extends HeaderClause {

	public ExportPackage(String name, Map<String, String> attribs) {
		super(name, attribs);
	}
	
	public List<String> getUses() {
		String usesStr = attribs.get(Constants.USES_DIRECTIVE);
		if(usesStr == null)
			return null;
		
		List<String> result = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(usesStr,",");
		while(tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken().trim());
		}
		return result;
	}

}
