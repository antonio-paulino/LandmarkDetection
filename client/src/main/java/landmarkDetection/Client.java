package landmarkDetection;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import servicestubs.*;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private static ServiceGrpc.ServiceBlockingStub blockingStub;
    private static ServiceGrpc.ServiceStub nonBlockingStub;


    public static void main(String[] args) {
        try {
            int svcPort = 8000;
            List<String> ips = new ArrayList<>();

            if (args.length > 0) {
                for (String arg : args) {
                    if (arg.matches("\\d+")) {
                        svcPort = Integer.parseInt(arg);
                    } else {
                        ips.add(arg.trim());
                    }
                }
            } else {
                ips = getGrpcServerIps();
            }

            if (ips.isEmpty()) {
                System.out.println("No IP found.");
                return;
            }
            String svcIP = ips.get(new Random().nextInt(ips.size()));


            logger.info("Connecting to server {}:{}", svcIP, svcPort);
            System.out.println("Connecting to server " + svcIP + ":" + svcPort);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    .usePlaintext()
                    .build();
            blockingStub = ServiceGrpc.newBlockingStub(channel);
            nonBlockingStub = ServiceGrpc.newStub(channel);

            boolean end = false;

            while (!end) {
                try {
                    int option = Menu();

                    switch (option) {
                        case 1:
                            isAlive();
                            break;
                        case 2:
                            uploadImage();
                            break;
                        case 3:
                            getImageInfo();
                            break;
                        case 99:
                            end = true;
                            logger.info("Exiting client...");
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    logger.error("Execution call error: {}", ex.getMessage());
                }
            }
            read("Press ENTER to exit", new Scanner(System.in));
        } catch (Exception ex) {
            logger.error("Unhandled exception: {}", ex.getMessage());
        }
    }

    static void isAlive() {
        TextMessage reply = blockingStub.isAlive(ProtoVoid.newBuilder().build());
        System.out.println("Ping server: " + reply.getTxt());
    }

    static void uploadImage() {
        String imagePath = read("Enter image path: ", new Scanner(System.in));
        final boolean[] uploadCompleted = {false};

        try {
            StreamObserver<UploadResult> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(UploadResult value) {
                    System.out.println("Upload completed. Request ID: " + value.getRequestId());
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Upload error: {}", t.getMessage());
                    uploadCompleted[0] = true;
                }

                @Override
                public void onCompleted() {
                    System.out.println("Upload stream completed.");
                    uploadCompleted[0] = true;
                }
            };

            StreamObserver<ImageChunk> requestObserver = nonBlockingStub.imageUpload(responseObserver);

            byte[] buffer = new byte[1024];
            try (FileInputStream fis = new FileInputStream(imagePath)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    ImageChunk chunk = ImageChunk.newBuilder()
                            .setImageChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                            .build();
                    requestObserver.onNext(chunk);
                }
            }

            requestObserver.onCompleted();
            while (!uploadCompleted[0]) {
                Thread.sleep(100);
            }

        } catch (Exception e) {
            logger.error("Upload error: {}", e.getMessage());
        }


    }

    static void getImageInfo() {
        String requestId = read("Enter request ID: ", new Scanner(System.in));

        Scanner scan = new Scanner(System.in);
        int op;
        do {
            System.out.println();
            System.out.println("\n======= Menu =======");
            System.out.println("1 - Get image results");
            System.out.println("2 - Get map");
            System.out.print("Option: ");
            op = scan.nextInt();
        } while (op < 1 || op > 2);

        if (op == 1) {
            getImageResults(requestId);
        } else if (op == 2) {
            getMap(requestId);
        } else {
            System.out.println("Invalid option. Please try again.");
        }
    }

    private static void getImageResults(String requestId) {
        try {
            DetectionResult results = blockingStub.getDetectionResult(DetectionRequest.newBuilder().setRequestId(requestId).build());
            if (results.getLandmarksCount() > 0) {
                System.out.println("Landmarks detected:");
                results.getLandmarksList().forEach(landmark ->
                        System.out.println(" - " + landmark.getName() + " at " + landmark.getLatitude() + ", " + landmark.getLongitude() + " with confidence " + landmark.getConfidence())
                );
            } else {
                System.out.println("No landmarks detected for request ID: " + requestId);
            }
        } catch (Exception e) {
            logger.error("Error retrieving image results: {}", e.getMessage());

        }
    }

    private static void getMap(String requestId) {
        try {
            MapResponse mapResponse = blockingStub.getMap(DetectionRequest.newBuilder().setRequestId(requestId).build());
            if (mapResponse.getMapURL().isEmpty()) {
                System.out.println("No map available for request ID: " + requestId);
                return;
            }

            ImageChunk img = mapResponse.getMapImage();
            if (img.getImageChunk().isEmpty()) {
                System.out.println("No image data received.");
                return;
            }


            byte[] imageBytes = img.getImageChunk().toByteArray();
            java.nio.file.Files.write(java.nio.file.Paths.get("map_result_"+ requestId +".jpg"), imageBytes);
            System.out.println("Map image saved as map_result_" + requestId + ".jpg");

        } catch (Exception e) {
            logger.error("Error retrieving map: {}", e.getMessage());
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("\n======= Menu =======");
            System.out.println(" 1 - isAlive");
            System.out.println(" 2 - Upload Image");
            System.out.println(" 3 - Get Image Info");
            System.out.println("99 - Exit");
            System.out.print("Option: ");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.print(msg);
        return input.nextLine();
    }

    private static List<String> getGrpcServerIps() {
        String url = "https://cn-http-function-269388437762.europe-southwest1.run.app/?group=vm-grpc-instance-group";
        List<String> ips = new ArrayList<>();
        try {
            java.net.URL obj = new java.net.URL(url);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(con.getInputStream()))) {
                    String response = in.readLine();
                    if (response != null && !response.isEmpty()) {
                        for (String ip : response.split(",")) {
                            ips.add(ip.trim());
                        }
                    }
                }
            } else {
                System.out.println("Error Get IPs, Code HTTP: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error Get IPs: " + e.getMessage());
        }
        return ips;
    }

}
