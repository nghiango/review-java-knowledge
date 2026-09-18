package lab.jvm.listenerregistry;

public interface Registration extends AutoCloseable {
    @Override
    void close();
}
