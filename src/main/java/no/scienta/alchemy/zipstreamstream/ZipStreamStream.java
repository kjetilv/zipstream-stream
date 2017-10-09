package no.scienta.alchemy.zipstreamstream;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiFunction;
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
                return entryStream(zipInputStream).map(entry -> fun.apply(inputStream));
            }
            @Override
            public <T> Stream<T> map(BiFunction<String, InputStream, T> fun) {
                return entryStream(zipInputStream).map(entry -> fun.apply(entry.getName(), inputStream));
            }

            @Override
            public <T> Stream<T> flatMap(Function<InputStream, Stream<T>> fun) {
                return entryStream(zipInputStream).flatMap(entry -> fun.apply(inputStream));
            }

            @Override
            public <T> Stream<T> flatMapEntries(BiFunction<ZipEntry, InputStream, Stream<T>> fun) {
                return entryStream(zipInputStream).flatMap(entry -> fun.apply(entry, inputStream));
            }

            @Override
            public <T> Stream<T> flatMap(BiFunction<String, InputStream, Stream<T>> fun) {
                return entryStream(zipInputStream).flatMap(entry -> fun.apply(entry.getName(), inputStream));
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

    private static Stream<ZipEntry> entryStream(ZipInputStream zis) {
        return StreamSupport.stream(new ZipEntrySpliterator(zis), false);
    }
}
