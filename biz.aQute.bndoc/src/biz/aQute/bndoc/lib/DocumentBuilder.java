package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.lib.env.*;

public class DocumentBuilder extends Base {
	Map<URI, Props>		sources;
	
	interface BaseOptions {
		Locale locale();
		String output();
		String template();
		String images();
		Collection<String> sources();
	}
	
	interface SingleOptions {
		
	}
	String					template	= null;

	DocumentBuilder(Base g) {
		super(g);
	}

	protected void prepare() throws Exception {
		if ( isPrepared())
			return;
		
		super.prepare();
	}

	
	
	
	
	@Override
	public void close() throws IOException {}


}