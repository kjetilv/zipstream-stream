package no.scienta.alchemy.zipstreamstream;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;

final class ZipStreamStreamImpl implements ZipStreamStream {

    private final ZipInputStream zipInputStream;

    private final InputStream inputStream;

    private final AtomicBoolean opened = new AtomicBoolean();

    ZipStreamStreamImpl(ZipInputStream zipInputStream) {
        this.zipInputStream = requireNonNull(zipInputStream, "zipInputStream");
        this.inputStream = new Uncloseable(this.zipInputStream);
    }

    @Override
    public <T> Stream<T> map(Function<InputStream, T> fun) {
        return entries(zipInputStream).map(entry -> fun.apply(inputStream));
    }

    @Override
    public <T> Stream<T> map(BiFunction<String, InputStream, T> fun) {
        return entries(zipInputStream).map(entry -> fun.apply(entry.getName(), inputStream));
    }

    @Override
    public <T> Stream<T> mapEntries(BiFunction<ZipEntry, InputStream, T> fun) {
        return entries(zipInputStream).map(entry -> fun.apply(entry, inputStream));
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

    @Override
    public void forEach(Consumer<InputStream> action) {
        entries(zipInputStream).forEach(zipEntry -> action.accept(inputStream));
    }

    @Override
    public void forEach(BiConsumer<String, InputStream> action) {
        entries(zipInputStream).forEach(zipEntry -> action.accept(zipEntry.getName(), inputStream));
    }

    @Override
    public void forEachEntry(BiConsumer<ZipEntry, InputStream> action) {
        entries(zipInputStream).forEach(zipEntry -> action.accept(zipEntry, inputStream));
    }

    private Stream<ZipEntry> entries(ZipInputStream zis) {
        if (opened.compareAndSet(false, true)) {
            return StreamSupport.stream(new ZipEntrySpliterator(zis), false);
        }
        throw new IllegalStateException(this + " already opened");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[[" + zipInputStream + " " + (opened.get() ? "ready" : "completed") + "]";
    }
}
