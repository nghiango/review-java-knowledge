package lab.corejava.examples;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class StreamExamples {
    private StreamExamples() {}

    public static List<String> normalizedNames(List<String> names) {
        return names.stream().map(String::strip).filter(name -> !name.isEmpty()).toList();
    }

    public static List<String> sortedNames(List<String> names) {
        return names.stream().map(String::strip).sorted().toList();
    }

    public static <T, R> List<R> mapInInputOrder(
            List<T> values, Function<? super T, R> mapper, Executor executor) {
        if (values.size() > 4) {
            throw new IllegalArgumentException("example batch must contain at most four values");
        }
        var futures =
                values.stream()
                        .map(
                                value ->
                                        CompletableFuture.supplyAsync(
                                                () -> mapper.apply(value), executor))
                        .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }
}
