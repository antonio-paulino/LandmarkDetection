package landmarkDetection;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.pubsub.v1.SubscriptionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarksApp {

    private static final Logger logger = LoggerFactory.getLogger(LandmarksApp.class);

    private static final String PROJECT_ID = "cn2425-t1-g11";
    private static final String SUBSCRIPTION_ID = "landmark-detection-topic-sub";

    public static void main(String[] args) {
        String subscriptionName = SubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID).toString();

        ExecutorProvider executorProvider = InstantiatingExecutorProvider
                .newBuilder()
                .setExecutorThreadCount(1)
                .build();

        Subscriber subscriber = Subscriber.newBuilder(subscriptionName, new MessageReceiveHandler())
                .setExecutorProvider(executorProvider)
                .build();

        subscriber.startAsync().awaitRunning();
        logger.info("Subscriber started.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            subscriber.stopAsync();
        }));

        while (true) {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                logger.info("Interrupted; shutting down.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    static class PubSubPayload {
        public String requestId;
        public String bucket;
        public String blob;

        public PubSubPayload(String requestId, String bucket, String blob) {
            this.requestId = requestId;
            this.bucket = bucket;
            this.blob = blob;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

        public static PubSubPayload fromString(String json) {
            return new Gson().fromJson(json, PubSubPayload.class);
        }
    }
}
