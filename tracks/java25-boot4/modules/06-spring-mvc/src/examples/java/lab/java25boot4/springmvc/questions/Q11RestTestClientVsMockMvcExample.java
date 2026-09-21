package lab.java25boot4.springmvc.questions;

public class Q11RestTestClientVsMockMvcExample {

    public static void main(String[] args) {
        // RestTestClient in Spring Framework 7 provides a unified client testing experience
        // combining MockMvc-like slice ergonomics with WebTestClient fluent assertions.
        boolean unifiesTestingPatterns = true;
        System.out.println(
                "RestTestClient unifies web tests: "
                        + unifiesTestingPatterns); // RestTestClient unifies web tests: true
    }
}
