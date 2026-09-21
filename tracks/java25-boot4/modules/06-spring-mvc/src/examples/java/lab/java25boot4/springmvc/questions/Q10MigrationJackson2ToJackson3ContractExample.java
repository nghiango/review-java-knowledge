package lab.java25boot4.springmvc.questions;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Q10MigrationJackson2ToJackson3ContractExample {

    public record AccountPayload(String accountId, long balanceCents) {}

    public static void main(String[] args) throws Exception {
        // Jackson 2 vs Jackson 3 package names:
        // Jackson 2: com.fasterxml.jackson.*
        // Jackson 3: tools.jackson.*
        // Records serialize cleanly without extra annotations in modern Spring Boot
        ObjectMapper mapper = new ObjectMapper();
        AccountPayload payload = new AccountPayload("ACC-900", 25000L);
        String json = mapper.writeValueAsString(payload);

        System.out.println(json.contains("ACC-900")); // true
        System.out.println(json.contains("25000")); // true
    }
}
