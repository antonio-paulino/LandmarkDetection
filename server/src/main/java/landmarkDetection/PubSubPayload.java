package landmarkDetection;

import com.google.gson.Gson;

public class PubSubPayload {
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
