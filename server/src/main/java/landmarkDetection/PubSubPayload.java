package landmarkDetection;

public class PubSubPayload {
    public String requestId;
    public String bucket;
    public String blob;

    public PubSubPayload(String requestId, String bucket, String blob) {
        this.requestId = requestId;
        this.bucket = bucket;
        this.blob = blob;
    }
}
