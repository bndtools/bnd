package bndtools.repository.orbit;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import aQute.libg.version.Version;

public class OrbitRepositoryTest extends TestCase {

    private OrbitRepository repo;

    @Override
    protected void setUp() throws Exception {
        URL resource = getClass().getResource("orbitbundles_small.map.txt");
        repo = new OrbitRepository();
        repo.setURL(resource);
        repo.initialise(null);
    }

    public void testQueryBSN() throws CoreException {
        Collection<String> bsns = repo.list(null);
        assertNotNull(bsns);
        assertEquals(2, bsns.size());

        Iterator<String> bsnIter = bsns.iterator();

        String bsn;
        Collection<Version> versions;
        Iterator<Version> versionsIter;

        bsn = bsnIter.next();
        assertEquals("ch.ethz.iks.slp", bsn);

        versions = repo.versions(bsn);
        assertNotNull(versions);
        assertEquals(1, versions.size());
        versionsIter = versions.iterator();
        assertEquals("1.0.0", versionsIter.next().toString());

        bsn = bsnIter.next();
        assertEquals("com.ibm.icu", bsn);

        versions = repo.versions(bsn);
        assertNotNull(versions);
        assertEquals(4, versions.size());
        versionsIter = versions.iterator();
        assertEquals("3.6.0", versionsIter.next().toString());
        assertEquals("3.6.1", versionsIter.next().toString());
        assertEquals("4.0.0", versionsIter.next().toString());
        assertEquals("4.0.1", versionsIter.next().toString());
    }

    public void testGetURLs() {
        URL[] urls = repo.get("com.ibm.icu", null);
        assertNotNull(urls);
        assertEquals(4, urls.length);
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.0.v20080530.jar", urls[0].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.1.v20080530.jar", urls[1].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", urls[2].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.1.v20090822.jar", urls[3].toString());
    }

    public void testGetUrlsWithRanges() {
        URL[] urls;

        urls = repo.get("com.ibm.icu", "4.0.0");
        assertNotNull(urls);
        assertEquals(2, urls.length);
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", urls[0].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.1.v20090822.jar", urls[1].toString());

        urls = repo.get("com.ibm.icu", "[3.6.1,4.0.1)");
        assertNotNull(urls);
        assertEquals(2, urls.length);
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.1.v20080530.jar", urls[0].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", urls[1].toString());

        urls = repo.get("com.ibm.icu", "[3.6.1,4.0.1]");
        assertNotNull(urls);
        assertEquals(3, urls.length);
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.1.v20080530.jar", urls[0].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", urls[1].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.1.v20090822.jar", urls[2].toString());

        urls = repo.get("com.ibm.icu", "(3.6.1,4.0.1]");
        assertNotNull(urls);
        assertEquals(2, urls.length);
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", urls[0].toString());
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.1.v20090822.jar", urls[1].toString());
    }

    public void testOrbitRepositoryRegexQuery() throws CoreException {
        Collection<String> bsns = repo.list("*ibm*");
        assertNotNull(bsns);
        assertEquals(1, bsns.size());
        assertEquals("com.ibm.icu", bsns.iterator().next());
    }
}
