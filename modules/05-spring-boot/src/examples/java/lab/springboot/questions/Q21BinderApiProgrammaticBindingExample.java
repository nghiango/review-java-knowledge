package lab.springboot.questions;

import java.util.Map;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

public class Q21BinderApiProgrammaticBindingExample {

    record ServerConfig(String host, int port, boolean sslEnabled) {}

    public static void main(String[] args) {
        ConfigurationPropertySource source =
                new MapConfigurationPropertySource(
                        Map.of(
                                "server.network.host", "10.0.0.1",
                                "server.network.port", "8443",
                                "server.network.ssl-enabled", "true"));

        Binder binder = new Binder(source);
        BindResult<ServerConfig> result = binder.bind("server.network", ServerConfig.class);

        boolean isBound = result.isBound(); // true
        ServerConfig config = result.get();
        int port = config.port(); // 8443
        boolean ssl = config.sslEnabled(); // true

        System.out.println(
                "Bound: "
                        + isBound
                        + ", host: "
                        + config.host()
                        + ", port: "
                        + port
                        + ", ssl: "
                        + ssl);
    }
}
