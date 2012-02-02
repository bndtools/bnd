package test;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Collections;

import org.osgi.service.bindex.impl.ResourceIndexerImpl;

public class Main {

	public static void main(String[] args) throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		OutputStreamWriter writer = new OutputStreamWriter(System.out);
		indexer.index(Collections.singleton(new File("testdata/03-export.jar")), writer, null);
		writer.close();
	}

}
