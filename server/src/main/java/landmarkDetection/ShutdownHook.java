package landmarkDetection;

public class ShutdownHook extends Thread {
    io.grpc.Server svc;
    Service service;

    public ShutdownHook(io.grpc.Server svc, Service service) {
        this.service = service;
        this.svc = svc;
    }

    @Override
    public void run() {
        System.err.println("*shutdown gRPC server, because JVM is shutting down");
        try {
            // Initiates an orderly shutdown in which preexisting calls continue
            // but new calls are rejected. So we can clean and finish work
            service.shutdownPublisher();
            svc.shutdown();
            svc.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
    }
}
