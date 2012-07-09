package bndtools.release;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import bndtools.Plugin;

public class FileUtils {
	public static IDocument readFully(IFile file) throws CoreException, IOException {
		if(file.exists()) {
			InputStream stream = file.getContents();
			byte[] bytes = readFully(stream);

			String string = new String(bytes, file.getCharset());
			return new Document(string);
		}
		return null;
	}

	public static byte[] readFully(InputStream stream) throws IOException {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			final byte[] buffer = new byte[1024];
			while(true) {
				int read = stream.read(buffer, 0, 1024);
				if(read == -1)
					break;
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		} finally {
			stream.close();
		}
	}

    public static void writeFully(IDocument document, IFile file, boolean createIfAbsent) throws CoreException {
        writeFully(document.get(), file, createIfAbsent);
    }

    public static void writeFully(String text, IFile file, boolean createIfAbsent) throws CoreException {
        ByteArrayInputStream inputStream;
        try {
            inputStream = new ByteArrayInputStream(text.getBytes(file.getCharset(true)));
        } catch (UnsupportedEncodingException e) {
            return;
        }
        if (file.exists()) {
            file.setContents(inputStream, false, true, null);
        } else {
            if (createIfAbsent)
                file.create(inputStream, false, null);
            else
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "File does not exist: " + file.getFullPath().toString(), null));
        }
    }
}