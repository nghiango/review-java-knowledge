package lab.java25boot4.whatsnew.questions;

/** Q15: what is {@code RestTestClient} and how does it differ from MockMvc / WebTestClient? */
public class Q15RestTestClientShapeExample {

    public static void main(String[] args) {
        // All three styles assert the same thing: HTTP 200 and a JSON body field.
        boolean mockMvcStyle =
                true; // mockMvc.perform(get("/orders/7")).andExpect(jsonPath("$.id").value(7))
        boolean webTestClientStyle =
                true; // webTestClient.get().uri("/orders/7").exchange().expectBody()... (needs WebFlux)
        boolean restTestClientStyle =
                true; // restTestClient.get().uri("/orders/7").exchange().expectStatus().isOk()...

        System.out.println(mockMvcStyle); // true — server-side only
        System.out.println(webTestClientStyle); // true — requires the reactive test stack
        System.out.println(restTestClientStyle); // true — Boot 4: one client for both, no reactive stack
    }
}
