package aQute.libg.cryptography;

import java.io.*;
import java.security.*;

import aQute.lib.io.*;

public abstract class Digester<T extends Digest> extends OutputStream {
	protected MessageDigest	md;
	
	public Digester(MessageDigest instance){
		md = instance;
	}
	@Override
	public void write( byte[] buffer, int offset, int length) throws IOException{
		md.update(buffer,offset,length);
	}
	@Override
	public void write( int b) throws IOException{
		md.update((byte) b);
	}
	
	public MessageDigest getMessageDigest() throws Exception {
		return md;
	}
	
	public T from(InputStream in) throws Exception {
		IO.copy(in,this);
		return digest();
	}
		
	public abstract T digest() throws Exception;
	public abstract T digest( byte [] bytes) throws Exception;
	public abstract String getAlgorithm();
}
