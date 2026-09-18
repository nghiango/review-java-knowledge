package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q02BytecodeExecutionExample {
    private Q02BytecodeExecutionExample() {}

    public static int calculate(int a, int b) {
        // Bytecode pushes operands onto operand stack and executes iadd:
        // iload_0 (push a) -> iload_1 (push b) -> iadd (pop both, add, push result) -> ireturn
        return a + b;
    }

    public static void main(String[] args) {
        int result = calculate(10, 25); // 35
    }
}
