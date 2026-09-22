package lab.java25boot4.whatsnew.questions;

/** Q8: what is a compact source file / instance {@code main}? */
public class Q08CompactSourceMainExample {

    // JEP 512: a class may declare an instance main method instead of `public static void main(String[])`.
    void main() {
        System.out.println("launched with an instance main method"); // no static, no String[] args
    }
}
