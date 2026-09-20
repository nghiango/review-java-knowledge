package lab.springboot.questions;

import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;

public class Q13ProfileActivationMultiDocYamlExample {

    public static void main(String[] args) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod", "cloud");

        boolean isProdActive = environment.acceptsProfiles(Profiles.of("prod")); // true
        boolean isDevActive = environment.acceptsProfiles(Profiles.of("dev")); // false
        boolean isProdAndCloud = environment.acceptsProfiles(Profiles.of("prod & cloud")); // true
        boolean isDevOrProd = environment.acceptsProfiles(Profiles.of("dev | prod")); // true

        System.out.println(
                "Prod: "
                        + isProdActive
                        + ", Dev: "
                        + isDevActive
                        + ", Complex: "
                        + isProdAndCloud
                        + ", Or: "
                        + isDevOrProd);
    }
}
