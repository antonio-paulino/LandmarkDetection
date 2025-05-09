package landmarkDetection;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.grpc.stub.StreamObserver;
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

    public Service(int svcPort) {
        this.storage = StorageOptions.getDefaultInstance().getService();
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

                    // TODO: Publish to Pub/Sub here with requestId, bucket, and blob

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
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpeg").build();

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
}
