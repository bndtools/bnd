package aQute.bnd.wstemplates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZipSlipCreator {
	public static void createZipSlip1(File zipFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
			addEntry(zos, "../evil.txt", "This is evil!");
		}
	}

	public static void createZipSlip2(File zipFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
			addEntry(zos, "../../outside.txt", "Going outside!");
		}
	}

	public static void createZipSlip3(File zipFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
			addEntry(zos, "subdir/../../../traversal.txt", "Sneaky traversal!");
		}
	}

	public static void createZipSlip4(File zipFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
			addEntry(zos, "/abs/abspath.txt", "Absolute path file!");
		}
	}

	private static void addEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		zos.putNextEntry(entry);
		zos.write(content.getBytes());
		zos.closeEntry();
	}
}
