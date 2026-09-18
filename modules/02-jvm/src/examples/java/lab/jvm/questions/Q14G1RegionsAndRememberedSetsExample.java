package lab.jvm.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q14G1RegionsAndRememberedSetsExample {
    private Q14G1RegionsAndRememberedSetsExample() {}

    public static void main(String[] args) {
        // G1 divides heap into uniform regions (1MB - 32MB).
        // Remembered Sets (RSets) track Old-to-Young references so Eden regions can be collected
        // independently.
        List<String> oldGenList = new ArrayList<>();
        oldGenList.add("persisted-record"); // Old generation reference tracked in Card Table / RSet
        int size = oldGenList.size(); // 1
    }
}
