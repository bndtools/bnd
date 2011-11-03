package aQute.lib.jardiff.manifest;

import java.util.*;
import java.util.jar.*;

import aQute.lib.jardiff.*;
import aQute.lib.jardiff.Diff.*;
import aQute.lib.osgi.*;

public class ManifestDiffPlugin implements DiffPlugin {

	public boolean canDiff(String name) {
		return name.endsWith("MANIFEST.MF");
	}

	public Collection<Diff> diff(Diff container, String name, Resource newResource,	Resource oldResource) throws  Exception {
		
		Manifest oldManifest = null;
		if (oldResource != null) {
			oldManifest = new Manifest(oldResource.openInputStream());
		}
		Manifest newManifest = null;
		if (newResource != null) {
			newManifest = new Manifest(newResource.openInputStream());
		}
		
		ManifestInfo manifestInfo = new ManifestInfo(container, name, newManifest, oldManifest);
		
        compareManifest(manifestInfo);
        
        return Arrays.asList((Diff) manifestInfo);
	}

    void compareManifest(ManifestInfo manifestInfo) {
    	
        if (manifestInfo.getNewManifest() == null || manifestInfo.getOldManifest() == null) {
        	if (manifestInfo.getNewManifest() != null) {
        		manifestInfo.setDelta(Delta.ADDED);
        	} else if (manifestInfo.getOldManifest() != null) {
        		manifestInfo.setDelta(Delta.REMOVED);
        	} else {
        		return;
        	}
        }
        if (manifestInfo.getDelta() != Delta.UNCHANGED) {
        	return;
        }

        Attributes newAttrs = manifestInfo.getNewManifest().getMainAttributes();
        Attributes oldAttrs = manifestInfo.getOldManifest().getMainAttributes();
        diff(manifestInfo, newAttrs, oldAttrs);
        for (Object element : newAttrs.keySet()) {
            Attributes.Name name = (Attributes.Name) element;
            String av = newAttrs.getValue(name);
            String bv = oldAttrs.getValue(name);
            if (bv != null) {
                if (!av.equals(bv)) {
                	ManifestEntry me = new ManifestEntry(manifestInfo, name.toString());
                	me.setNewValue(av);
                	me.setOldValue(bv);
                	me.setDelta(Delta.MODIFIED);
                	manifestInfo.addDiff(me);
                	manifestInfo.setDelta(Delta.MODIFIED);
                }
            }
        }
    }

    void diff(ManifestInfo manifestInfo, Attributes newAttrs, Attributes oldAttrs) {
    	
        Set<Object> onlyInNew = new HashSet<Object>(newAttrs.keySet());
        onlyInNew.removeAll(oldAttrs.keySet());
        Set<Object> onlyInOld = new HashSet<Object>(oldAttrs.keySet());
        onlyInOld.removeAll(newAttrs.keySet());

        for (Object element : onlyInNew) {
            Attributes.Name name = (Attributes.Name) element;
            String value = newAttrs.getValue(name);
        	ManifestEntry me = new ManifestEntry(manifestInfo, name.toString());
        	me.setDelta(Delta.ADDED);
        	me.setNewValue(value);
        	manifestInfo.addDiff(me);
        	manifestInfo.setDelta(Delta.MODIFIED);
        }
        for (Object element : onlyInOld) {
            Attributes.Name name = (Attributes.Name) element;
            String value = oldAttrs.getValue(name);
        	ManifestEntry me = new ManifestEntry(manifestInfo, name.toString());
        	me.setDelta(Delta.REMOVED);
        	me.setOldValue(value);
        	manifestInfo.addDiff(me);
        	manifestInfo.setDelta(Delta.MODIFIED);
        }
    }

	public String getName() {
		return "Manifest Changes";
	}
}
