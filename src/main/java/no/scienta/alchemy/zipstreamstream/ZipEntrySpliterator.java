package no.scienta.alchemy.zipstreamstream;

import java.io.IOException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ZipEntrySpliterator implements Spliterator<ZipEntry> {

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
