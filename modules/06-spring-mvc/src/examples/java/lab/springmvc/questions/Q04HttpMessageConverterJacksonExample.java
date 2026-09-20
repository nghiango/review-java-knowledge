package lab.springmvc.questions;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Q04HttpMessageConverterJacksonExample {

    record CustomerDto(String name, String email) {}

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        CustomerDto customer = new CustomerDto("Alice", "alice@example.com");

        // MappingJackson2HttpMessageConverter serializes Java records/objects into JSON payloads
        String json = objectMapper.writeValueAsString(customer);
        boolean containsName = json.contains("Alice"); // true
        boolean containsEmail = json.contains("alice@example.com"); // true

        CustomerDto deserialized = objectMapper.readValue(json, CustomerDto.class);
        boolean matches = customer.equals(deserialized); // true

        System.out.println(
                "Serialized JSON: "
                        + json
                        + ", matches: "
                        + matches
                        + ", fields: "
                        + (containsName && containsEmail));
    }
}
