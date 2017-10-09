package no.scienta.alchemy.zipstreamstream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("ALL")
public class ZipStreamStream {

    public static <T> Processor stream(InputStream inputStream) {
        return stream(inputStream, null);
    }

    public static <T> Processor stream(InputStream zis, Charset charset) {
        return stream(new ZipInputStream(zis, charset == null ? StandardCharsets.UTF_8 : charset));
    }

    public static Processor stream(ZipInputStream zipInputStream) {
        UncloseableInputStream inputStream = seal(Objects.requireNonNull(zipInputStream, "zip input stream"));
        return new Processor() {
            @Override
            public <T> Stream<T> map(Function<InputStream, T> fun) {
                return entries(zipInputStream).map(entry -> fun.apply(inputStream));
            }
            @Override
            public <T> Stream<T> map(BiFunction<String, InputStream, T> fun) {
                return entries(zipInputStream).map(entry -> fun.apply(entry.getName(), inputStream));
            }

            @Override
            public <T> Stream<T> flatMap(Function<InputStream, Stream<T>> fun) {
                return entries(zipInputStream).flatMap(entry -> fun.apply(inputStream));
            }

            @Override
            public <T> Stream<T> flatMapEntries(BiFunction<ZipEntry, InputStream, Stream<T>> fun) {
                return entries(zipInputStream).flatMap(entry -> fun.apply(entry, inputStream));
            }

            @Override
            public <T> Stream<T> flatMap(BiFunction<String, InputStream, Stream<T>> fun) {
                return entries(zipInputStream).flatMap(entry -> fun.apply(entry.getName(), inputStream));
            }
        };
    }

    interface Processor  {

        <T> Stream<T> map(Function<InputStream, T> fun);

        <T> Stream<T> map(BiFunction<String, InputStream, T> fun);

        <T> Stream<T> flatMap(Function<InputStream, Stream<T>> fun);

        <T> Stream<T> flatMap(BiFunction<String, InputStream, Stream<T>> fun);

        <T> Stream<T> flatMapEntries(BiFunction<ZipEntry, InputStream, Stream<T>> fun);
    }

    private static UncloseableInputStream seal(ZipInputStream zis) {
        return new UncloseableInputStream(zis);
    }

    private static Stream<ZipEntry> entries(ZipInputStream zis) {
        return StreamSupport.stream(new ZipEntrySpliterator(zis), false);
    }

    private static class ZipEntrySpliterator implements Spliterator<ZipEntry> {

        private final AtomicReference<ZipEntry> current;

        private final ZipInputStream zis;

        ZipEntrySpliterator(ZipInputStream zis) {
            this.zis = zis;
            current = new AtomicReference<>();
        }

        @Override
        public boolean tryAdvance(Consumer<? super ZipEntry> action) {
            closePreviousEntry();
            ZipEntry nextEntry = nextEntry();
            return nextEntry == null
                    ? finishStream()
                    : processNext(action, nextEntry);
        }

        @Override
        public Spliterator<ZipEntry> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.NONNULL & Spliterator.IMMUTABLE;
        }

        private boolean processNext(Consumer<? super ZipEntry> action, ZipEntry nextEntry) {
            try {
                action.accept(nextEntry);
            } finally {
                current.set(nextEntry);
            }
            return true;
        }

        private boolean finishStream() {
            try {
                zis.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return false;
        }

        private void closePreviousEntry() {
            Optional.ofNullable(current.get()).ifPresent(entry -> {
                try {
                    zis.closeEntry();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        private ZipEntry nextEntry() {
            ZipEntry nextEntry;
            try {
                nextEntry = zis.getNextEntry();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return nextEntry;
        }
    }

    private static final class UncloseableInputStream extends InputStream {

        private final InputStream inputStream;

        UncloseableInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void close() throws IOException {
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
}
