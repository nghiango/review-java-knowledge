package lab.webflux.questions;

public class Q11BlockHoundInstrumentationExample {

    record BlockHoundConfig(boolean enabled, String detectionMethod, String exceptionThrown) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // BlockHound is a Java agent that instruments JDK and library classes via ByteBuddy.
        // It intercepts known blocking methods (e.g. Thread.sleep, SocketInputStream.read,
        // FileInputStream.read)
        // and throws BlockingOperationError whenever they are invoked from threads marked as
        // NonBlocking (such as Netty event loops).
        BlockHoundConfig config =
                new BlockHoundConfig(
                        true,
                        "Bytecode instrumentation of blocking JDK calls",
                        "reactor.blockhound.BlockingOperationError");

        boolean protectsEventLoops = config.enabled(); // true
        String thrownException =
                config.exceptionThrown(); // "reactor.blockhound.BlockingOperationError"

        System.out.println("BlockHound active: " + protectsEventLoops);
        System.out.println("Exception thrown on blocking call: " + thrownException);
    }
}
