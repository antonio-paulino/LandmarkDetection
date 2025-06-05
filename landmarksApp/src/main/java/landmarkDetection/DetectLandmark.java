package landmarkDetection;

import com.google.cloud.firestore.*;
import com.google.cloud.vision.v1.*;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DetectLandmark {

    public static void detectLandmarksGcs(String gcsPath, String requestId) throws IOException, ExecutionException, InterruptedException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<Landmark> landmarks = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return;
                }

                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    if (!annotation.getLocationsList().isEmpty()) {
                        LocationInfo info = annotation.getLocationsList().getFirst();
                        Landmark landmark = new Landmark(
                                annotation.getDescription(),
                                info.getLatLng().getLatitude(),
                                info.getLatLng().getLongitude(),
                                annotation.getScore()
                        );
                        landmarks.add(landmark);
                    }
                }
            }
        }

        if (!landmarks.isEmpty()) {
            saveToFirestore(landmarks, requestId);
        } else {
            System.out.println("No landmarks detected.");
        }
    }

    private static void saveToFirestore(List<Landmark> landmarks, String requestId) throws IOException, ExecutionException, InterruptedException {
        Firestore db = FirestoreOptions.getDefaultInstance().toBuilder()
                        .setDatabaseId("cn2425-t1-g11")
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
                .getService();

        List<Map<String, Object>> landmarkList = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            landmarkList.add(Map.of(
                    "name", landmark.name(),
                    "latitude", landmark.latitude(),
                    "longitude", landmark.longitude(),
                    "confidence", landmark.confidence()
            ));
        }

        Map<String, Object> data = Map.of(
                "landmarks", landmarkList,
                "timestamp", new Date()
        );

        ApiFuture<WriteResult> result = db.collection("landmark-detections").document(requestId).set(data);
        System.out.println("Firestore write time: " + result.get().getUpdateTime());
    }
}
