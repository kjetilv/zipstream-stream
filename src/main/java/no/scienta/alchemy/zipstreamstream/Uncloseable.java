package no.scienta.alchemy.zipstreamstream;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Wraps a stream and averts {@link #close() close calls} to it.  All other calls are forward to the wrapped stream.
 */
final class Uncloseable extends InputStream {

    private final InputStream inputStream;

    Uncloseable(InputStream inputStream) {
        this.inputStream = requireNonNull(inputStream, "inputStream");
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
