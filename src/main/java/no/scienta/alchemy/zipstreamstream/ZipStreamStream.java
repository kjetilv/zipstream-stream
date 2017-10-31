package no.scienta.alchemy.zipstreamstream;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;

/**
 * A functional way to process zip files.  By using the streaming paradigm, we avoid loading the whole thing into
 * memory.  As the resulting stream is processed, data gets pulled from the zip file on-demand.
 * <p/>
 * The {@link ZipStreamStream} takes responsibility for closing the zip stream. This needs to happen when
 * the stream is exhausted.  Since we read from the zip stream on-demand as the stream is processed, this is at
 * some undefined point in the future.  In other words, closing it just after creating the {@link ZipStreamStream}
 * is too early.
 */
@SuppressWarnings("unused")
public interface ZipStreamStream {

    /**
     * Whenever you want zips<br>
     * All you have to do is stream<br>
     * ZipStreamStream.stream<br>
     *
     * @param inputStream An input stream which can be unzipped. If this value is a {@link ZipInputStream} already,
     *                    it will be used as-is
     * @return Stream which handles closing of the inputstream was passed as an argument
     */
    static ZipStreamStream stream(InputStream inputStream) {
        return stream(requireNonNull(inputStream, "inputStream"), null);
    }

    /**
     * Whenever you want zips<br>
     * All you have to do is stream<br>
     * ZipStreamStream.stream<br>
     *
     * @param inputStream An input stream which can be unzipped.  If this value is a {@link ZipInputStream} already,
     *                    it will be used as-is, ignoring the charset argument.
     * @param charset     Charset for reading the inputstream as a zip stream
     * @return Stream which handles closing of the inputstream that was passed as an argument
     */
    static ZipStreamStream stream(InputStream inputStream, Charset charset) {
        boolean alreadyZipped = requireNonNull(inputStream, "inputStream") instanceof ZipStreamStream;
        return stream(alreadyZipped ? (ZipInputStream) inputStream
                : charset == null ? new ZipInputStream(inputStream)
                : new ZipInputStream(inputStream, charset));
    }

    /**
     * Whenever you want zips<br>
     * All you have to do is stream<br>
     * ZipStreamStream.stream<br>
     *
     * @param zipInputStream A ready-to-go zip input stream
     * @return Stream which handles closing of the zip inputstream that was passed as an argument
     */
    static ZipStreamStream stream(ZipInputStream zipInputStream) {
        return new ZipStreamStreamImpl(requireNonNull(zipInputStream, "zipInputStream"));
    }

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A stream of target type instances, one per zip entry
     */
    <T> Stream<T> map(Function<InputStream, T> fun);

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A stream of target type instances, one per named zip entry
     */
    <T> Stream<T> map(BiFunction<String, InputStream, T> fun);

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A stream of target type instances, one per zip entry
     */
    <T> Stream<T> mapEntries(BiFunction<ZipEntry, InputStream, T> fun);

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A flattened stream of target type instances, produced from zip entries
     */
    <T> Stream<T> flatMap(Function<InputStream, Stream<T>> fun);

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A flattened stream of target type instances, produced from named zip entries
     */
    <T> Stream<T> flatMap(BiFunction<String, InputStream, Stream<T>> fun);

    /**
     * @param fun Mapping function
     * @param <T> Target type
     * @return A flattened stream of target type instances, produced from zip entries
     */
    <T> Stream<T> flatMapEntries(BiFunction<ZipEntry, InputStream, Stream<T>> fun);

    /**
     * @param action Action to run on each zip entry
     */
    void forEach(Consumer<InputStream> action);

    /**
     * @param action Action to run on each named zip entry
     */
    void forEach(BiConsumer<String, InputStream> action);

    /**
     * @param action Action to run on each zip entry
     */
    void forEachEntry(BiConsumer<ZipEntry, InputStream> action);
}
