package name.neilbartlett.eclipse.bndtools.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

public class FileUtils {
	public static IDocument readFully(IFile file) throws CoreException, IOException {
		if(file.exists()) {
			InputStream stream = file.getContents();
			try {
				Reader reader = new InputStreamReader(stream, file.getCharset());
				StringWriter writer = new StringWriter();
				
				char[] buffer = new char[1024];
				while(true) {
					int charsRead = reader.read(buffer, 0, 1024);
					if(charsRead == -1)
						break;
					writer.write(buffer, 0, charsRead);
				}
				return new Document(writer.toString());
			} finally {
				stream.close();
			}
		}
		return null;
	}
	public static void writeFully(IDocument document, IFile file, boolean createIfAbsent) throws CoreException, FileNotFoundException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(document.get().getBytes());
		if(file.exists()) {
			file.setContents(inputStream, false, true, null);
		} else {
			if(createIfAbsent)
				file.create(inputStream, false, null);
			else
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "File does not exist: " + file.getFullPath().toString(), null));
		}
	}
}
