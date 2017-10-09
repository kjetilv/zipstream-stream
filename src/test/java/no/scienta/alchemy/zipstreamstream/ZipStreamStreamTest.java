package no.scienta.alchemy.zipstreamstream;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("SameParameterValue")
public class ZipStreamStreamTest {

    @Test
    public void testJar() throws FileNotFoundException {
        File file = getFile("text.zip");

        Collection<String> texts = ZipStreamStream.stream(new FileInputStream(file))
                .flatMap(this::readFirstLine)
                .collect(Collectors.toSet());

        Assert.assertEquals(
                new HashSet<>(Arrays.asList("zip", "bar", "argh")),
                texts);
    }

    @Test
    public void testJar2() throws FileNotFoundException {
        File file = getFile("text.zip");

        Collection<String> texts = ZipStreamStream.stream(new FileInputStream(file))
                .flatMap(this::readLines)
                .collect(Collectors.toSet());

        Assert.assertEquals(
                new HashSet<>(Arrays.asList("zip", "bar", "grok", "argh")),
                texts);
    }

    private File getFile(String name) {
        URL testJar = Thread.currentThread().getContextClassLoader().getResource(name);
        Assert.assertNotNull(testJar);
        return new File(testJar.getFile());
    }

    private Stream<String> readFirstLine(InputStream inputStream) {
        try (LineNumberReader lnr = getLnr(inputStream)) {
            return Stream.of(lnr.readLine());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Stream<String> readLines(InputStream inputStream) {
        try (LineNumberReader lnr = getLnr(inputStream)) {
            Collection<String> lines = new ArrayList<>();
            do {
                String line = lnr.readLine();
                if (line == null) {
                    return lines.stream();
                }
                lines.add(line);
            } while (true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private LineNumberReader getLnr(InputStream inputStream) {
        return new LineNumberReader(new InputStreamReader(inputStream));
    }

}
