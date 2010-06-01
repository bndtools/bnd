package bndtools.repository.orbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OrbitGetMapParser {

    private static final String PREFIX_PLUGIN = "plugin@";
    static final String PROP_BSN = "bsn";
    static final String PROP_VERSION = "version";
    static final String PROP_URL = "url";

    private final InputStream stream;

    public OrbitGetMapParser(InputStream stream) {
        this.stream = stream;
    }

    public List<Map<String, String>> parse() throws IOException {
        List<Map<String, String>> result = new ArrayList<Map<String,String>>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            while(true) {
                String line = reader.readLine();
                if(line == null) break;
                if(line.startsWith(PREFIX_PLUGIN)) {
                    HashMap<String, String> entry = new HashMap<String, String>();
                    int equalIndex = line.indexOf("=");
                    if(equalIndex == -1)
                        throw new IllegalArgumentException("Invalid format");

                    String idPart = line.substring(PREFIX_PLUGIN.length(), equalIndex);
                    String locationPart = line.substring(equalIndex + 1);

                    String[] idSplit = idPart.split(",");
                    if(idSplit.length != 2)
                        throw new IllegalArgumentException("Invalid format");
                    entry.put(PROP_BSN, idSplit[0]);
                    entry.put(PROP_VERSION, idSplit[1]);

                    String[] locationSplit = locationPart.split(",");
                    if(locationSplit.length < 2)
                        throw new IllegalArgumentException("Invalid format");

                    // Only GET method is supported
                    if(!"GET".equals(locationSplit[0]))
                        continue;

                    // Bundles requiring unpacking are not supported
                    if(locationSplit.length == 3 && locationSplit[2].indexOf("unpack=true") != -1)
                        continue;

                    entry.put(PROP_URL, locationSplit[1]);

                    result.add(entry);
                }
            }
            return result;
        } finally {
            reader.close();
        }
    }

}
