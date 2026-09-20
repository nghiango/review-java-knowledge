package lab.webflux.questions;

public class Q19ByteBufLeakDetectionAndOperatorDebugExample {

    record DiagnosticTool(String name, String productionSafeLevel, String diagnosticPurpose) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In Reactor Netty, pooled direct ByteBufs must be released or reference counting will leak
        // native memory.
        // - ResourceLeakDetector: Netty leak detector (-Dio.netty.leakDetection.level=ADVANCED or
        // SIMPLE).
        // - Hooks.onOperatorDebug(): Captures assembly stack traces but carries high runtime
        // overhead (never run in prod!).
        // - Reactor Debug Agent: Uses ByteBuddy at load time without runtime overhead (production
        // safe).
        DiagnosticTool nettyLeak =
                new DiagnosticTool(
                        "Netty ResourceLeakDetector",
                        "SIMPLE",
                        "Detect unreleased ByteBuf reference counts in native memory");
        DiagnosticTool operatorDebug =
                new DiagnosticTool(
                        "Hooks.onOperatorDebug",
                        "DISABLED_IN_PROD",
                        "Captures stack trace during stream assembly graph creation");

        boolean operatorDebugUnsafeForProd =
                operatorDebug.productionSafeLevel().equals("DISABLED_IN_PROD"); // true
        boolean nettyLeakTracksDirectBuffers =
                nettyLeak.diagnosticPurpose().contains("ByteBuf"); // true

        System.out.println("Operator debug carries high overhead: " + operatorDebugUnsafeForProd);
        System.out.println(
                "Netty leak detector checks native ByteBuf: " + nettyLeakTracksDirectBuffers);
    }
}
