package landmarkDetection;

import com.google.cloud.WriteChannel;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.stub.StreamObserver;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicestubs.*;

public class Service extends ServiceGrpc.ServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final Storage storage;
    private final String bucketName = "cn2425-t1-g11";
    private final Publisher publisher;

    public Service(int svcPort) throws IOException {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.publisher = Publisher.newBuilder(TopicName.of("cn2425-t1-g11", "landmark-detection-topic")).build();
        logger.info("gRPC service is running on port: {}", svcPort);
    }

    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        logger.info("Received isAlive health check request");
        responseObserver.onNext(TextMessage.newBuilder().setTxt("Service is alive").build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ImageChunk> imageUpload(StreamObserver<UploadResult> responseObserver) {
        return new StreamObserver<>() {
            private final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
            private final String blobName = UUID.randomUUID().toString();

            @Override
            public void onNext(ImageChunk chunk) {
                try {
                    imageStream.write(chunk.getImageChunk().toByteArray());
                } catch (IOException e) {
                    logger.error("Failed to write image chunk: {}", e.getMessage());
                    responseObserver.onError(
                            Status.INTERNAL.withDescription("Failed to process image chunk").withCause(e).asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Image upload failed: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                try {
                    byte[] imageData = imageStream.toByteArray();

                    if (imageData.length == 0) {
                        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Empty image data").asRuntimeException());
                        return;
                    }

                    uploadToCloudStorage(blobName, imageData);
                    String requestId = blobName;

                    PubSubPayload payload = new PubSubPayload(requestId, bucketName, blobName);

                    publishToPubSub(payload);

                    logger.info("Image successfully uploaded with ID: {}", requestId);
                    responseObserver.onNext(UploadResult.newBuilder().setRequestId(requestId).build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    logger.error("Upload failed: {}", e.getMessage());
                    responseObserver.onError(
                            Status.INTERNAL.withDescription("Failed to upload image").withCause(e).asRuntimeException()
                    );
                }
            }

            private void uploadToCloudStorage(String blob, byte[] data) throws IOException {
                BlobId blobId = BlobId.of(bucketName, blob);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpg").build();

                if (data.length < 1_000_000) {
                    storage.create(blobInfo, data);
                } else {
                    try (WriteChannel writer = storage.writer(blobInfo)) {
                        writer.write(ByteBuffer.wrap(data));
                    }
                }

                logger.info("Upload to Cloud Storage completed for blob: {}", blob);
            }
        };
    }

    private void publishToPubSub(PubSubPayload payload) {
        try {

            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(payload.toString()))
                    .build();
            publisher.publish(message).get();

        } catch (Exception e) {
            logger.error("Failed to publish to Pub/Sub: {}", e.getMessage());
            publisher.shutdown();
        } finally {
            publisher.shutdown();
        }

    }

    public static byte[] toByteArray(BufferedImage image) {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert image to byte array: " + e.getMessage(), e);
        }
    }

    @Override
    public void getMap(DetectionRequest request, StreamObserver<MapResponse> responseObserver) {
        String requestId = request.getRequestId();
        logger.info("Received Image info request for id: {}", requestId);

        try {

            Blob blob = storage.get(BlobId.of(bucketName, requestId));
            if (blob == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Image not found").asRuntimeException());
                return;
            }

            publishToPubSub(new PubSubPayload(requestId, bucketName, blob.getName()));

            // TODO: obter informações da imagem do Firestore

            BufferedImage imageData = GoogleMapsImage.getImage(38.756881,-9.116445);

            ImageChunk imageChunk = ImageChunk.newBuilder()
                    .setImageChunk(ByteString.copyFrom(toByteArray(imageData)))
                    .build();

            MapResponse mapResponse = MapResponse.newBuilder()
                    .setMapURL(GoogleMapsImage.getImageUrl(38.756881,-9.116445))
                    .setMapImage(imageChunk)
                    .build();

            responseObserver.onNext(mapResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Failed to get the mapImage: {}", e.getMessage());
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Failed to get the mapImage").withCause(e).asRuntimeException()
            );
        }
    }
    @Override
    public void getDetectionResult(DetectionRequest request, StreamObserver<DetectionResult> responseObserver) {
        String requestId = request.getRequestId();
        logger.info("Received detection result request for id: {}", requestId);

    }
}
