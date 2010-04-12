package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.jardiff.*;
import aQute.lib.osgi.*;

public class DiffTest extends TestCase {

    public void testSimple() throws IOException {
        aQute.lib.jardiff.Diff diff = new Diff();
        Map<String,Object> map = diff.diff(new Jar(new File("jar/ds.jar")), new Jar(new File("jar/asm.jar")), false);
        
        diff.print(System.out, map, 0);
    }
}
