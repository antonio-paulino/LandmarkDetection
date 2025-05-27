package landmarkDetection;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageReceiveHandler implements MessageReceiver {

    private static final Logger logger = LoggerFactory.getLogger(MessageReceiveHandler.class);
    private final Firestore firestore;

    public MessageReceiveHandler() {
        this.firestore = FirestoreOptions.getDefaultInstance().getService();
    }

    @Override
    public void receiveMessage(PubsubMessage msg, AckReplyConsumer ackReply) {
        try {
            String json = msg.getData().toStringUtf8();
            LandmarksApp.PubSubPayload payload = LandmarksApp.PubSubPayload.fromString(json);

            String gcsUri = String.format("gs://%s/%s", payload.bucket, payload.blob);

            DetectLandmark.detectLandmarksGcs(gcsUri, payload.requestId);

            ackReply.ack();

            logger.info("Message successfully processed: {}", payload.requestId);
        } catch (Exception e) {
            logger.error("Failed to process message: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}