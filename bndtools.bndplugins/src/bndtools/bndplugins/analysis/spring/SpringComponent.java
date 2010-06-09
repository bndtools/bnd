package bndtools.bndplugins.analysis.spring;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Resource;
import aQute.libg.reporter.Reporter;

/**
 * This component is called when we find a resource in the META-INF/*.xml
 * pattern. We parse the resource and and the imports to the builder.
 *
 * Parsing is done with XSLT (first time I see the use of having XML for the
 * Spring configuration files!).
 *
 * @author aqute
 *
 */
public class SpringComponent implements AnalyzerPlugin, Plugin {
    static final String PROPNAME_PREFIX_PATTERN = "pattern";
	static final Pattern[] DEFAULT_SPRING_PATTERNS = new Pattern[] { Pattern.compile("META-INF/spring/.*\\.xml") };
	static final Pattern QN = Pattern.compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");

	static Transformer transformer;

    private Reporter reporter;
    private Map<String, String> properties;

	public static Set<CharSequence> analyze(InputStream in) throws Exception {
		if (transformer == null) {
			TransformerFactory tf = TransformerFactory.newInstance();
			Source source = new StreamSource(SpringComponent.class.getResourceAsStream("extract.xsl"));
			transformer = tf.newTransformer(source);
		}

		Set<CharSequence> refers = new HashSet<CharSequence>();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Result r = new StreamResult(bout);
		Source s = new StreamSource(in);
		transformer.transform(s, r);

		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		bout.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(bin));

		String line = br.readLine();
		while (line != null) {
			line = line.trim();
			if (line.length() > 0) {
				String parts[] = line.split("\\s*,\\s*");
				for (int i = 0; i < parts.length; i++) {
					int n = parts[i].lastIndexOf('.');
					if (n > 0) {
						refers.add(parts[i].subSequence(0, n));
					}
				}
			}
			line = br.readLine();
		}
		br.close();
		return refers;
	}

	public void setProperties(Map<String, String> map) {
        this.properties = map;
	}

	public void setReporter(Reporter reporter) {
        this.reporter = reporter;
	}

	private Pattern[] getSourcePatterns(Reporter reporter) {
	    Pattern[] result = DEFAULT_SPRING_PATTERNS;

	    if(properties != null) {
	        List<Pattern> patterns = new ArrayList<Pattern>(properties.size());

	        for (Entry<String, String> propertyEntry : properties.entrySet()) {
                if(propertyEntry.getKey().startsWith(PROPNAME_PREFIX_PATTERN)) {
                    String patternStr = propertyEntry.getValue();
                    try {
                        Pattern pattern = Pattern.compile(patternStr);
                        patterns.add(pattern);
                    } catch (PatternSyntaxException e) {
                        if(reporter != null)
                            reporter.error("Invalid regular expression: %s", patternStr);
                    }
                }
            }

	        result = patterns.toArray(new Pattern[patterns.size()]);
	    }

	    return result;
	}

    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        Jar jar = analyzer.getJar();
        Map<String, Resource> resources = jar.getResources();

        Pattern[] patterns = getSourcePatterns(analyzer);

        for (Entry<String, Resource> entry : resources.entrySet()) {
            String path = entry.getKey();
            Resource resource = entry.getValue();

            boolean match = false;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(path).matches()) {
                    match = true;
                    break;
                }
            }

            if (match) {
                try {
                    InputStream in = resource.openInputStream();
                    Set<CharSequence> set = analyze(in);
                    in.close();
                    for (Iterator<CharSequence> r = set.iterator(); r.hasNext();) {
                        String pack = (String) r.next();
                        if (!QN.matcher(pack).matches())
                            analyzer.warning("Package does not seem a package in spring resource (" + path + "): " + pack);
                        if (!analyzer.getReferred().containsKey(pack))
                            analyzer.getReferred().put(pack, new LinkedHashMap<String, String>());
                    }
                } catch (Exception e) {
                    analyzer.error("Unexpected exception in processing spring resources(" + path + "): " + e);
                }
            }
        }
        return false;
    }
}