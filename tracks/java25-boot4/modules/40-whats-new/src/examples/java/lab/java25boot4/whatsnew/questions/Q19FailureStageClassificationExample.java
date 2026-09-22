package lab.java25boot4.whatsnew.questions;

/** Q19: which upgrade changes fail at compile time, at startup, and only under production traffic? */
public class Q19FailureStageClassificationExample {

    public static void main(String[] args) {
        System.out.println(classify("removed Security 7 and() method")); // compile time
        System.out.println(classify("renamed @ConfigurationProperties key")); // startup (with @Validated)
        System.out.println(classify("silently changed matcher default")); // production traffic only
    }

    static String classify(String change) {
        return switch (change) {
            case "removed Security 7 and() method" -> "compile time";
            case "renamed @ConfigurationProperties key" -> "startup (with @Validated)";
            case "silently changed matcher default" -> "production traffic only";
            default -> "unknown";
        };
    }
}
