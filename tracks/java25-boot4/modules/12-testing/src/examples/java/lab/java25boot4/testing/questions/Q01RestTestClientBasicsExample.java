package lab.java25boot4.testing.questions;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Q01: How does modern Spring testing unify REST endpoint integration assertions? */
public class Q01RestTestClientBasicsExample {

    public static void main(String[] args) {
        boolean isMockMvcAvailable = MockMvc.class.isInterface() || !MockMvc.class.isPrimitive();
        boolean canBuildStandalone = MockMvcBuilders.standaloneSetup().build() != null;

        System.out.println("MockMvc available: " + isMockMvcAvailable); // MockMvc available: true
        System.out.println(
                "Standalone builder functional: "
                        + canBuildStandalone); // Standalone builder functional: true
    }
}
