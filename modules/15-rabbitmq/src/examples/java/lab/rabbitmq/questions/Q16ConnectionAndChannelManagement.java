package lab.rabbitmq.questions;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

/**
 * Q16: How does CachingConnectionFactory manage AMQP connections and channels, and what is channel
 * churn?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q16ConnectionAndChannelManagement {

    public static void main(String[] args) {
        // Connections are heavy TCP sockets involving TLS handshakes, authentication, and
        // heartbeats.
        // Applications typically open ONE shared Connection per process, multiplexing lightweight
        // Channels over it.
        CachingConnectionFactory factory = new CachingConnectionFactory();
        boolean multiplexChannelsOverConnection = true; // true

        // CacheMode:
        // CHANNEL (default): Single shared connection, channels are cached up to channelCacheSize.
        // CONNECTION: Multiple connections cached, each with its own channel pool.
        CachingConnectionFactory.CacheMode mode = CachingConnectionFactory.CacheMode.CHANNEL;
        boolean defaultIsChannelCache =
                (mode == CachingConnectionFactory.CacheMode.CHANNEL); // true

        // Channel Churn Hazard:
        // If channelCacheSize is too small (e.g. default 25) and concurrency is 100,
        // channels are continuously created and closed (channel churn), causing high CPU on both
        // client and broker.
        int recommendedChannelCacheSize = 100;
        factory.setChannelCacheSize(recommendedChannelCacheSize);
        boolean boundsChannelChurn = (factory.getChannelCacheSize() >= 100); // true
    }
}
