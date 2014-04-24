package aQute.bnd.version;

import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class TestVersion extends TestCase {
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private Manifest setupManifest(String... headers) {
		Manifest manifest = new Manifest();
		Attributes attrs = manifest.getMainAttributes();
		int i = 0;
		String headerName = null;
		for (String value : headers) {
			i++;
			switch (i) {
				default :
				case 1 :
					headerName = value;
					break;

				case 2 :
					attrs.putValue(headerName, value);
					headerName = null;
					i = 0;
					break;
			}
		}

		return manifest;
	}

	public void testFromManifestNull() throws Exception {
		Version v = Version.fromManifest(null, true);
		assertNull(v);

		v = Version.fromManifest(null, false);
		assertNull(v);
	}

	public void testFromManifestEmpty() throws Exception {
		Manifest manifest = setupManifest();
		Version v = Version.fromManifest(manifest, true);
		assertNull(v);

		v = Version.fromManifest(manifest, false);
		assertNull(v);
	}

	public void testFromManifest() throws Exception {
		String[][] VERSION_HEADERS = {
			/* header                  value      strict     nonstrict */
			{Constants.BUNDLE_VERSION, "1.2.3.4", "1.2.3.4", "1.2.3.4"},
			{"Implementation-Version", "1.2.3.4",      null, "1.2.3.4"},
			{"Specification-Version" , "1.2.3.4",      null, "1.2.3.4"},
			{"Version"               , "1.2.3.4",      null, "1.2.3.4"},
			{"BogusHeader"           , "1.2.3.4",      null,      null},

			{Constants.BUNDLE_VERSION, "x.2.3.4",      null,      null},
			{"Implementation-Version", "x.2.3.4",      null,      null},
			{"Specification-Version" , "x.2.3.4",      null,      null},
			{"Version"               , "x.2.3.4",      null,      null},
			{"BogusHeader"           , "x.2.3.4",      null,      null},

			{Constants.BUNDLE_VERSION, "1.2.3-4",      null,      null},
			{"Implementation-Version", "1.2.3-4",      null, "1.2.3.4"},
			{"Specification-Version" , "1.2.3-4",      null, "1.2.3.4"},
			{"Version"               , "1.2.3-4",      null, "1.2.3.4"},
			{"BogusHeader"           , "1.2.3-4",      null,      null},

			{Constants.BUNDLE_VERSION, "1.2.3"  ,   "1.2.3",   "1.2.3"},
			{"Implementation-Version", "1.2.3"  ,      null,   "1.2.3"},
			{"Specification-Version" , "1.2.3"  ,      null,   "1.2.3"},
			{"Version"               , "1.2.3"  ,      null,   "1.2.3"},
			{"BogusHeader"           , "1.2.3"  ,      null,      null},

			{Constants.BUNDLE_VERSION, "1.2-3"  ,      null,      null},
			{"Implementation-Version", "1.2-3"  ,      null, "1.2.0.3"},
			{"Specification-Version" , "1.2-3"  ,      null, "1.2.0.3"},
			{"Version"               , "1.2-3"  ,      null, "1.2.0.3"},
			{"BogusHeader"           , "1.2-3"  ,      null,      null},

			{Constants.BUNDLE_VERSION, "1.2"    ,     "1.2",   "1.2.0"},
			{"Implementation-Version", "1.2"    ,      null,   "1.2.0"},
			{"Specification-Version" , "1.2"    ,      null,   "1.2.0"},
			{"Version"               , "1.2"    ,      null,   "1.2.0"},
			{"BogusHeader"           , "1.2"    ,      null,      null},

			{Constants.BUNDLE_VERSION, "1-2"    ,      null,      null},
			{"Implementation-Version", "1-2"    ,      null, "1.0.0.2"},
			{"Specification-Version" , "1-2"    ,      null, "1.0.0.2"},
			{"Version"               , "1-2"    ,      null, "1.0.0.2"},
			{"BogusHeader"           , "1-2"    ,      null,      null},

			{Constants.BUNDLE_VERSION, "1"      ,       "1",   "1.0.0"},
			{"Implementation-Version", "1"      ,      null,   "1.0.0"},
			{"Specification-Version" , "1"      ,      null,   "1.0.0"},
			{"Version"               , "1"      ,      null,   "1.0.0"},
			{"BogusHeader"           , "1"      ,      null,      null}
		};

		for (String[] entry : VERSION_HEADERS) {
			String header = entry[0];
			String headerValue = entry[1];
			Version expectedStrict = entry[2] == null ? null : new Version(entry[2]);
			Version expectedNonStrict = entry[3] == null ? null : new Version(entry[3]);

			Manifest manifest = setupManifest(header, headerValue);

			Version strict = Version.fromManifest(manifest, true);
			Version nonStrict = Version.fromManifest(manifest, false);

			String title = header + "=" + headerValue;
			assertEquals("    strict " + title, expectedStrict, strict);
			assertEquals("non-strict " + title, expectedNonStrict, nonStrict);
		}
	}

	public void testFromFileName() {
		String highest = Version.HIGHEST.toString();
		String[][] FILENAMES = {
				/* fileName                         strict     nonstrict */
				{null                             ,      null,      null},

				{"a/dir/biz.aQute.bnd-1.2.3.4.jar", "1.2.3.4", "1.2.3.4"},
				{"a/dir/biz.aQute.bnd-1.2.3.jar"  ,   "1.2.3",   "1.2.3"},
				{"a/dir/biz.aQute.bnd-1.2.jar"    ,   "1.2.0",   "1.2.0"},
				{"a/dir/biz.aQute.bnd-1.jar"      ,   "1.0.0",   "1.0.0"},

				{"a/dir/biz.aQute.bnd-1.2.3.x.jar", "1.2.3.x", "1.2.3.x"},
				{"a/dir/biz.aQute.bnd-1.2.3-x.jar",      null, "1.2.3.x"},

				{"a/dir/biz.aQute.bnd-x.2.3.x.jar",      null,      null},
				{"a/dir/biz.aQute.bnd-x.2.3-x.jar",      null,      null},

				{"a/dir/biz.aQute.bnd-1.2.x.jar"  ,      null, "1.2.0.x"},
				{"a/dir/biz.aQute.bnd-1.2-x.jar"  ,      null, "1.2.0.x"},

				{"a/dir/biz.aQute.bnd-1.x.jar"    ,      null, "1.0.0.x"},
				{"a/dir/biz.aQute.bnd-1-x.jar"    ,      null, "1.0.0.x"},

				{"a/dir/biz.aQute.bnd-latest.jar" ,   highest,   highest}
			};

		for (String[] entry : FILENAMES) {
			String header = entry[0];
			String expStrict = entry[1];
			String expNonStrict = entry[2];
			Version expStrictVersion = expStrict == null ? null : highest.equals(expStrict) ? Version.HIGHEST
					: new Version(expStrict);
			Version expNonStrictVersion = expNonStrict == null ? null : highest.equals(expNonStrict) ? Version.HIGHEST
					: new Version(expNonStrict);

			Version strict = Version.fromFileName(header, true);
			Version nonstrict = Version.fromFileName(header, false);

			assertEquals("    strict " + header, expStrictVersion, strict);
			assertEquals("non-strict " + header, expNonStrictVersion, nonstrict);
		}
	}
}