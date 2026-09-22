package lab.java25boot4.whatsnew.questions;

import java.util.Map;

/** Q2: what are unnamed variables ({@code _}) for? */
public class Q02UnnamedVariablesExample {

    public static void main(String[] args) {
        Map<String, Integer> counts = Map.of("a", 1, "b", 2);

        int seen = 0;
        for (Map.Entry<String, Integer> _ : counts.entrySet()) { // loop variable is required but unused
            seen++;
        }

        try {
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException _) { // catch parameter is required but unused
            seen++;
        }

        System.out.println(seen); // 3 — two entries plus the caught exception
    }
}
