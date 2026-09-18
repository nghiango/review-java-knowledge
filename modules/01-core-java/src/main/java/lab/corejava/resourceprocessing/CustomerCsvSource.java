package lab.corejava.resourceprocessing;

import java.io.BufferedReader;
import java.io.IOException;

@FunctionalInterface
public interface CustomerCsvSource {
    BufferedReader open() throws IOException;
}
