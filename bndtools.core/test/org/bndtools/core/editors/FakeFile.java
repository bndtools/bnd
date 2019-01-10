package org.bndtools.core.editors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import aQute.lib.io.ByteBufferOutputStream;

import static org.bndtools.core.editors.ImportPackageQuickFixProcessorTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FakeFile {

    static class FileInfo {
        private byte[] fContents;
        private String fCharset;

        public FileInfo(byte[] contents, String charset) {
            fContents = contents;
            fCharset = charset;
        }

        public InputStream getContentStream() throws CoreException {
            if (!exists()) {
                throw new CoreException(new Status(IStatus.ERROR, "fakefile", "File does not exist"));
            }
            return new ByteArrayInputStream(fContents);
        }

        public byte[] getRawContents() {
            return fContents;
        }

        public String getContents() {
            try {
                return new String(fContents, fCharset);
            } catch (UnsupportedEncodingException e) {
                fail("Unsupported encoding: " + fCharset, e);
            }
            return null;

        }

        public void setContents(byte[] contents) {
            fContents = contents;
        }

        public void setContents(String contents) {
            try {
                fContents = contents == null ? null : contents.getBytes(fCharset);
            } catch (UnsupportedEncodingException e) {
                fail("Unsupported encoding: " + fCharset, e);
            }
        }

        public void setContents(InputStream source) throws CoreException {
            BufferedInputStream buf = new BufferedInputStream(source);
            try (ByteBufferOutputStream out = new ByteBufferOutputStream(4096)) {
                int b;
                while ((b = buf.read()) > -1) {
                    out.write(b);
                }
                setContents(out.toByteArray());
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, "fakefile", "Error reading from source", e));
            }
        }

        public String getCharset() {
            return fCharset;
        }

        public void setCharset(String charset) {
            fCharset = charset;
        }

        public boolean exists() {
            return fContents != null;
        }
    }

    protected static Map<IFile, FileInfo> fakeInfo = new WeakHashMap<>();

    public static void setContents(IFile fakeFile, byte[] contents) {
        FileInfo info = safeGetFileInfo(fakeFile);
        info.setContents(contents);
    }

    public static void setContents(IFile fakeFile, String contents) {
        safeGetFileInfo(fakeFile).setContents(contents);
    }

    public static byte[] rawContentsOf(IFile fakeFile) {
        return safeGetFileInfo(fakeFile).getRawContents();
    }

    public static String contentsOf(IFile fakeFile) {
        return safeGetFileInfo(fakeFile).getContents();
    }

    protected static FileInfo safeGetFileInfo(IFile fakeFile) {
        FileInfo info = fakeInfo.get(fakeFile);
        if (info == null) {
            throw new AssertionError("Supplied file is not a faked file");
        }
        return info;
    }

    public static String charsetOf(IFile fakeFile) {
        return safeGetFileInfo(fakeFile).getCharset();
    }

    public static IFile fakeFile() {
        return fakeFile(null);
    }

    public static IFile fakeFile(final byte[] contents) {
        return fakeFile(contents, "utf-8");
    }

    @SuppressWarnings("deprecation")
    public static IFile fakeFile(final byte[] contents, final String charset) {
        final IFile retval = mock(IFile.class, DO_NOT_CALL);
        final FileInfo info = new FileInfo(contents, charset);
        fakeInfo.put(retval, info);

        try {
            doAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) {
                    return info.getContents();
                }
            }).when(retval)
                .toString();

            doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {
                    return info.exists();
                }
            }).when(retval)
                .exists();

            doAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    return retval.getCharset(true);
                }
            }).when(retval)
                .getCharset();

            doAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    return info.exists() ? info.getCharset() : null;
                }
            }).when(retval)
                .getCharset(anyBoolean());

            doAnswer(new Answer<InputStream>() {
                @Override
                public InputStream answer(InvocationOnMock invocation) throws Throwable {
                    return info.getContentStream();
                }
            }).when(retval)
                .getContents();

            doAnswer(new Answer<InputStream>() {
                @Override
                public InputStream answer(InvocationOnMock invocation) throws Throwable {
                    return info.getContentStream();
                }
            }).when(retval)
                .getContents(anyBoolean());

            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    fail("Shouldn't use deprecated API");
                    return null;
                }
            }).when(retval)
                .setCharset(any());

            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    String charset = invocation.getArgument(0);
                    info.setCharset(charset);
                    return null;
                }
            }).when(retval)
                .setCharset(any(), any());

            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    // public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws
                    // CoreException {
                    final InputStream source = invocation.getArgument(0);
                    info.setContents(source);
                    return null;
                }

            }).when(retval)
                .setContents(any(InputStream.class), anyInt(), any());

            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    // public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor
                    // monitor) throws CoreException {
                    final InputStream source = invocation.getArgument(0);
                    info.setContents(source);
                    return null;
                }

            }).when(retval)
                .setContents(any(InputStream.class), anyBoolean(), anyBoolean(), any());

        } catch (CoreException e) {
            fail("Couldn't set up mock", e);
        }

        return retval;
    }
}
