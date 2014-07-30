package aQute.lib.zip;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * This class provides utilities to work with zip files.
 * 
 * http://www.opensource.apple.com/source/zip/zip-6/unzip/unzip/proginfo/extra.fld
 */
public class ZipUtil {
	static TimeZone tz = TimeZone.getDefault();
	
	public static long getModifiedTime(ZipEntry entry) throws IOException {
		long time = entry.getTime();
		time += tz.getOffset(time);
		return Math.min(time, System.currentTimeMillis()-1);
	}
////	
//		
//		byte[] extra = entry.getExtra();
//		if ( extra != null) try {
//			DataInputStream din = new DataInputStream( new ByteArrayInputStream(extra));
//
//			while (din.available() >= 0) {
//				int type = din.readShort();
//				int length = din.readShort();
//				
//				switch(type) {
//					case 0x5455: // timestamp
//						int flags = din.readByte();
//						long modtime = -1L;
//						long acctime = -1L;
//						long crtime = -1L;
//						if ( (flags & 1) != 0) {
//							modtime = din.readInt();
//							modtime *= 1000;
//						}
//						if ( (flags & 2) != 0) {
//							acctime = din.readInt();
//							acctime *= 1000;
//						}
//						if ( (flags & 4) != 0) {
//							crtime = din.readInt();
//							crtime *= 1000;
//						}
//
//						return modtime;
//						
//					default:
//						din.skipBytes(length);
//						break;
//				}
//			}
//		} catch( Exception e) {
//			// ignore
//		}
//		
//		return entry.getTime();
//	}

	public static void setModifiedTime(ZipEntry entry, long utc) throws IOException {
//		byte[] extra = entry.getExtra();
//		ByteArrayOutputStream bout = new ByteArrayOutputStream();
//		DataOutputStream dout = new DataOutputStream(bout);
//		
//		dout.writeShort(0x5455);
//		dout.writeShort(5);
//		dout.writeByte(1);
//		dout.writeInt((int) (utc/1000));
//		
//		if ( extra != null) try {
//			DataInputStream din = new DataInputStream( new ByteArrayInputStream(extra));
//
//			while (din.available() >= 0) {
//				int type = din.readShort();
//				int length = din.readShort();
//				
//				switch(type) {
//					case 0x5455: // timestamp
//						din.skipBytes(length);
//						break;
//						
//					default:
//						dout.writeShort(type);
//						dout.writeShort(length);
//						while(length > 0) {
//							dout.writeByte( din.readByte());
//						}
//						break;
//				}
//			}
//			
//		} catch( Exception e) {
//			// ignore
//		}
//		dout.flush();
//		entry.setExtra(bout.toByteArray());

		utc -= tz.getOffset(utc);
		entry.setTime(utc);
	}

}
