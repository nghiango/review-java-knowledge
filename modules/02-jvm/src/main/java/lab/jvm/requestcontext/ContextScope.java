package lab.jvm.requestcontext;

public interface ContextScope extends AutoCloseable {
    @Override
    void close();
}
