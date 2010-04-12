package aQute.bnd.maven;

import java.util.*;

import aQute.bnd.service.*;
import aQute.libg.reporter.*;

public class MavenGroup implements BsnToMavenPath, Plugin {
    String    groupId = "";

    public String[] getGroupAndArtifact(String bsn) {
        String[] result = new String[2];
        result[0] = groupId;
        result[1] = bsn;
        return result;
    }

    public void setProperties(Map<String, String> map) {
        if (map.containsKey("groupId")) {
            groupId = map.get("groupId");
        }
    }

    public void setReporter(Reporter processor) {
    }

}
