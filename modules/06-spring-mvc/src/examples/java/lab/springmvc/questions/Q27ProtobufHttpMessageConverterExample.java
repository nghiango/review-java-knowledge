package lab.springmvc.questions;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

@SuppressWarnings("unused")
public final class Q27ProtobufHttpMessageConverterExample {
    private Q27ProtobufHttpMessageConverterExample() {}

    public static class BinaryPayload {
        private final byte[] data;

        public BinaryPayload(byte[] data) {
            this.data = data;
        }

        public byte[] data() {
            return data;
        }
    }

    // Custom HttpMessageConverter registered in WebMvcConfigurer.extendMessageConverters:
    public static class BinaryPayloadConverter extends AbstractHttpMessageConverter<BinaryPayload> {
        public static final MediaType APPLICATION_BINARY =
                new MediaType("application", "x-custom-binary");

        public BinaryPayloadConverter() {
            super(APPLICATION_BINARY);
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return BinaryPayload.class.isAssignableFrom(clazz);
        }

        @Override
        protected BinaryPayload readInternal(
                Class<? extends BinaryPayload> clazz, HttpInputMessage inputMessage)
                throws IOException, HttpMessageNotReadableException {
            byte[] bytes = inputMessage.getBody().readAllBytes();
            return new BinaryPayload(bytes);
        }

        @Override
        protected void writeInternal(BinaryPayload payload, HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {
            outputMessage.getBody().write(payload.data());
        }
    }

    public static void main(String[] args) {
        BinaryPayloadConverter converter = new BinaryPayloadConverter();
        List<MediaType> supported = converter.getSupportedMediaTypes();
        boolean supportsBinary =
                supported.contains(BinaryPayloadConverter.APPLICATION_BINARY); // true
    }
}
