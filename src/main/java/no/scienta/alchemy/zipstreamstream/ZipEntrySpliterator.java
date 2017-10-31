package no.scienta.alchemy.zipstreamstream;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ZipEntrySpliterator implements Spliterator<ZipEntry> {

    private final ZipInputStream zipInputStream;

    private final AtomicReference<ZipEntry> current = new AtomicReference<>();

    ZipEntrySpliterator(ZipInputStream zipInputStream) {
        this.zipInputStream = Objects.requireNonNull(zipInputStream, "zipInputStream");
    }

    @Override
    public boolean tryAdvance(Consumer<? super ZipEntry> action) {
        return nextEntry()
                .map(processEntry(action))
                .orElseGet(finishStream());
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
        return Spliterator.IMMUTABLE;
    }

    private Optional<ZipEntry> nextEntry() {
        return Optional.ofNullable(current.updateAndGet(entry -> {
            if (entry != null) {
                closeCurrent();
            }
            return openNext();
        }));
    }

    private Function<ZipEntry, Boolean> processEntry(Consumer<? super ZipEntry> action) {
        return entry -> {
            action.accept(entry);
            return true;
        };
    }

    private Supplier<Boolean> finishStream() {
        return () -> {
            try {
                zipInputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return false;
        };
    }

    private ZipEntry openNext() {
        try {
            return zipInputStream.getNextEntry();
        } catch (IOException e) {
            throw new IllegalStateException
                    (this + ": Failed to open next entry of zip stream! " +
                            "The underlying stream may have been closed", e);
        }
    }

    private void closeCurrent() {
        try {
            zipInputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException
                    (this + ": Failed to close current entry of zip stream! " +
                            "The underlying stream may have been closed", e);
        }
    }
}
