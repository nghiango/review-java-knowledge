package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Q23: Microkernel (Plugin) Architecture. Demonstrates a core microkernel engine coordinating
 * plugins via a common SPI contract.
 */
public class Q23MicrokernelPluginArchitectureExample {

    public interface FormatterPlugin {
        String format(String input);
    }

    public static class UpperCasePlugin implements FormatterPlugin {
        @Override
        public String format(String input) {
            return input.toUpperCase(Locale.ROOT);
        }
    }

    // Microkernel core engine holding registry
    public static class MicrokernelCore {
        private final List<FormatterPlugin> plugins = new ArrayList<>();

        public void registerPlugin(FormatterPlugin plugin) {
            this.plugins.add(plugin);
        }

        public String process(String input) {
            String current = input;
            for (FormatterPlugin plugin : plugins) {
                current = plugin.format(current);
            }
            return current;
        }
    }

    public static void main(String[] args) {
        MicrokernelCore core = new MicrokernelCore();
        core.registerPlugin(new UpperCasePlugin());

        String formatted = core.process("architecture"); // "ARCHITECTURE"
        boolean kernelExtendedWithoutCodeChange = formatted.equals("ARCHITECTURE"); // true

        System.out.println(
                "Q25 result: " + formatted + ", extended: " + kernelExtendedWithoutCodeChange);
    }
}
