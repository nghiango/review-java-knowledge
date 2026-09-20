package lab.springmvc.questions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

public class Q17JacksonCustomSerializerExample {

    record MaskedCreditCard(String cardNumber) {}

    static class MaskedCreditCardSerializer extends JsonSerializer<MaskedCreditCard> {
        @Override
        public void serialize(
                MaskedCreditCard value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            String num = value.cardNumber();
            if (num != null && num.length() >= 4) {
                gen.writeString("****-****-****-" + num.substring(num.length() - 4));
            } else {
                gen.writeString("****");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(MaskedCreditCard.class, new MaskedCreditCardSerializer());
        mapper.registerModule(module);

        MaskedCreditCard card = new MaskedCreditCard("4111222233334444");
        String json = mapper.writeValueAsString(card); // "\"****-****-****-4444\""

        boolean isMasked = json.contains("****-****-****-4444"); // true

        System.out.println("Masked serialized output: " + json + ", valid: " + isMasked);
    }
}
