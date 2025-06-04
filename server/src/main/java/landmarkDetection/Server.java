package landmarkDetection;

import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static int svcPort = 8000;

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);

            Service service = new Service(svcPort);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    .addService(service)
                    .build();

            svc.start();
            logger.info("gRPC server started successfully on port: {}", svcPort);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc, service));
            logger.info("Shutdown hook registered.");

            svc.awaitTermination();
        } catch (Exception ex) {
            logger.error("Error starting gRPC server: {}", ex.getMessage(), ex);
        }
    }
}