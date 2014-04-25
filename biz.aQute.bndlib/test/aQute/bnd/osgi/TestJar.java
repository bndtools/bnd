package aQute.bnd.osgi;

import java.io.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;

public class TestJar extends TestCase {
	private File	tmpDir						= new File("testresources/tmp");

	File			emptyJarFile				= new File("testresources/jars/empty.jar");
	File			emptyNoManifestJarFile		= new File("testresources/jars/empty_no_manifest.jar");
	File			emptyCleanManifestJarFile	= new File("testresources/jars/empty_clean_manifest.jar");

	private Jar setupJar(String... headers) throws Exception {
		File tmpJarFile = File.createTempFile("jarfile", ".jar", tmpDir);
		tmpJarFile.deleteOnExit();
		IO.copy(emptyJarFile, tmpJarFile);
		Jar tmpJar = new Jar(tmpJarFile);
		try {
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

			tmpJar.setManifest(manifest);
			tmpJar.write(tmpJarFile);
		}
		finally {
			tmpJar.close();
		}

		return new Jar(tmpJarFile);
	}

	public void testGetBsnManifestNull() throws Exception {
		Jar jar = new Jar(emptyNoManifestJarFile);

		try {
			String strictOrg = jar.getBsn();
			String strict = jar.getBsn(true);
			String nonStrict = jar.getBsn(false);

			assertNull("org strict ", strictOrg);
			assertNull("    strict ", strict);
			assertNull("non-strict ", nonStrict);
		}
		finally {
			jar.close();
		}
	}

	public void testGetBsnManifestEmpty() throws Exception {
		Jar jar = new Jar(emptyCleanManifestJarFile);

		try {
			String strictOrg = jar.getBsn();
			String strict = jar.getBsn(true);
			String nonStrict = jar.getBsn(false);

			assertNull("org strict ", strictOrg);
			assertNull("    strict ", strict);
			assertNull("non-strict ", nonStrict);
		}
		finally {
			jar.close();
		}
	}

	public void testGetBsnBoolean() throws Exception {
		
		String[][] HEADERS = {
			/* header                       value    strict   nonstrict */
			{Constants.BUNDLE_SYMBOLICNAME, "a.b.c", "a.b.c", "a.b.c"},
			{"Implementation-Title"       , "a.b.c",    null, "a.b.c"},
			{"Implementation-Vendor"      , "a.b.c",    null, "a.b.c"},
			{"Specification-Title"        , "a.b.c",    null, "a.b.c"},
			{"Specification-Vendor"       , "a.b.c",    null, "a.b.c"},
			{"Implementation-Vendor-Id"   , "a.b.c",    null, "a.b.c"},
			{"Name"                       , "a.b.c",    null, "a.b.c"},
			{"BogusHeader"                , "a.b.c",    null,    null},

			{Constants.BUNDLE_SYMBOLICNAME, "#.b.c",    null,    null},
			{"Implementation-Title"       , "#.b.c",    null,    null},
			{"Implementation-Vendor"      , "#.b.c",    null,    null},
			{"Specification-Title"        , "#.b.c",    null,    null},
			{"Specification-Vendor"       , "#.b.c",    null,    null},
			{"Implementation-Vendor-Id"   , "#.b.c",    null,    null},
			{"Name"                       , "#.b.c",    null,    null},
			{"BogusHeader"                , "#.b.c",    null,    null}
		};

		for (String[] entry : HEADERS) {
			String header = entry[0];
			String headerValue = entry[1];
			String expectedStrict = entry[2];
			String expectedNonStrict = entry[3];

			Jar jar = setupJar(header, headerValue);
			try {
				String strictOrg = jar.getBsn();
				String strict = jar.getBsn(true);
				String nonStrict = jar.getBsn(false);

				String title = header + "=" + headerValue;
				assertEquals("org strict " + title, expectedStrict, strictOrg);
				assertEquals("    strict " + title, expectedStrict, strict);
				assertEquals("non-strict " + title, expectedNonStrict, nonStrict);
			}
			finally {
				jar.close();
			}
		}
	}
	
	public void testGetBsnFromFileName() throws Exception {
		String highest = Version.HIGHEST.toString();
		String[][] FILENAMES = {
				/* fileName                               strict           nonstrict */
				{null                             ,            null,            null},
                                                                           
				{tmpDir.getAbsolutePath()         ,            null,            null},
                                                                           
				{"somefile.notajar"               ,            null,            null},

				{"a/dir/biz.aQute.bnd-1.2.3.4.jar", "biz.aQute.bnd", "biz.aQute.bnd"},
				{"a/dir/ iz.aQute.bnd-1.2.3.4.jar",  "iz.aQute.bnd",  "iz.aQute.bnd"},
				{"a/dir/biz.aQute.bnd-1.2.3.x.jar", "biz.aQute.bnd", "biz.aQute.bnd"},
				{"a/dir/biz.aQute.bnd-1.2.x.jar"  ,            null, "biz.aQute.bnd"},
				{"a/dir/biz.aQute.bnd-1.2-x.jar"  ,            null, "biz.aQute.bnd"},
				{"a/dir/biz.aQute.bnd.jar"        ,            null, "biz.aQute.bnd"},
				{"a/dir/#iz.aQute.bnd.jar"        ,            null,            null}
			};

		for (String[] entry : FILENAMES) {
			String header = entry[0];
			String expStrict = entry[1];
			String expNonStrict = entry[2];

			String strict = Jar.getBsnFromFileName(header, true);
			String nonstrict = Jar.getBsnFromFileName(header, false);

			assertEquals("    strict " + header, expStrict, strict);
			assertEquals("non-strict " + header, expNonStrict, nonstrict);
		}
	}
}