package landmarkDetection;

public class Service extends LandmarkDetectionServiceGrpc.LandmarkDetectionServiceImplBase {
    public Service(int svcPort){
        System.out.println("Service is available on port:" + svcPort);
    }
}
