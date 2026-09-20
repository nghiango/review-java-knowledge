package lab.springsecurity.questions;

import java.util.Map;

public class Q19OwaspTop10ApiSecurityMapping {

    public static void main(String[] args) {
        Map<String, String> owaspApiTop10 =
                Map.of(
                        "API1:2023", "Broken Object Level Authorization (IDOR/BOLA)",
                        "API2:2023", "Broken Authentication (Weak JWT, missing signature)",
                        "API3:2023", "Broken Object Property Level Authorization (Mass Assignment)",
                        "API4:2023", "Unrestricted Resource Consumption (Rate Limiting/OOM)",
                        "API5:2023",
                                "Broken Function Level Authorization (Overly broad permitAll)");

        boolean idorIsTop1 =
                owaspApiTop10.get("API1:2023").contains("Object Level Authorization"); // true
        boolean authIsTop2 =
                owaspApiTop10.get("API2:2023").contains("Broken Authentication"); // true

        System.out.println(
                "API1:2023 corresponds to IDOR/BOLA: "
                        + idorIsTop1); // API1:2023 corresponds to IDOR/BOLA: true
        System.out.println(
                "API2:2023 corresponds to Broken Authentication: "
                        + authIsTop2); // API2:2023 corresponds to Broken Authentication: true
        System.out.println(
                "Mapped categories count: " + owaspApiTop10.size()); // Mapped categories count: 5
    }
}
