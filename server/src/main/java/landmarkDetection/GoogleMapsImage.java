package landmarkDetection;

import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.stub.StreamObserver;
import servicestubs.ImageChunk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GoogleMapsImage {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String apiKey = dotenv.get("GOOGLE_MAPS_API_KEY");
    private static final String baseUrl = "https://maps.googleapis.com/maps/api/staticmap";
    private static final String size = "600x600";

    public static String getImageUrl(double latitude, double longitude) {
        return String.format(
                "%s?center=%f,%f&zoom=15&size=%s&markers=color:red|%f,%f&key=%s",
                baseUrl, latitude, longitude, size, latitude, longitude, apiKey
        );
    }

    public static BufferedImage getImage(double latitude, double longitude) {
        String mapUrl = getImageUrl(latitude, longitude);
        try {
            URL url = new URL(mapUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (InputStream in = conn.getInputStream()) {
                return javax.imageio.ImageIO.read(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to get the Map image: " + e.getMessage(), e);
        }
    }
}

