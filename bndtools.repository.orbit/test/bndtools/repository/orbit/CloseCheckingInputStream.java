package bndtools.repository.orbit;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloseCheckingInputStream extends InputStream {

    private final InputStream delegate;
    private AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        delegate.close();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    public CloseCheckingInputStream(InputStream delegate) {
        this.delegate = delegate;
    }
}
