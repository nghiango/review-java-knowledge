package lab.java25boot4.corejava.questions;

import java.util.List;

public class Q03UnnamedVariablesAndPatternsExample {

    record Point3D(int x, int y, int z) {}

    public static void main(String[] args) {
        Point3D p = new Point3D(10, 20, 30);

        // Pattern matching with unnamed pattern variable _
        if (p instanceof Point3D(int x, int y, _)) {
            System.out.println(x + y); // 30
        }

        List<String> items = List.of("A", "B", "C");
        int count = 0;
        for (String _ : items) {
            count++;
        }
        System.out.println(count); // 3
    }
}
