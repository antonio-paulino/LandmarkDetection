package landmarkDetection;

import io.grpc.ServerBuilder;

public class Server {

    private static int svcPort = 8000;

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);

            System.out.println("Starting server on port " + svcPort);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                            .addService(new Service(svcPort))
                                    .build();
            svc.start();

            System.out.println("Server started on port " + svcPort);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));

            svc.awaitTermination();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }
}
