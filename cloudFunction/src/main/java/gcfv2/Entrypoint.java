package gcfv2;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.compute.v1.*;
import java.io.BufferedWriter;

public class Entrypoint implements HttpFunction {

    private static final String PROJECT_ID = "cn2425-t1-g11";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();

        String zone = request.getFirstQueryParameter("zone").orElse("europe-southwest1-a");
        String groupName = request.getFirstQueryParameter("group").orElse(null);

        if (groupName == null) {
            response.setStatusCode(400);
            writer.write("Missing required query parameter: group\n");
            return;
        }

        StringBuilder ipList = new StringBuilder();

        try (InstanceGroupsClient instanceGroupsClient = InstanceGroupsClient.create();
             InstancesClient instancesClient = InstancesClient.create()) {

            InstanceGroupsListInstancesRequest instanceGroupsListRequest =
                    InstanceGroupsListInstancesRequest.newBuilder()
                            .setInstanceState("ALL")
                            .build();

            ListInstancesInstanceGroupsRequest requestList =
                    ListInstancesInstanceGroupsRequest.newBuilder()
                            .setProject(PROJECT_ID)
                            .setZone(zone)
                            .setInstanceGroup(groupName)
                            .setInstanceGroupsListInstancesRequestResource(instanceGroupsListRequest)
                            .build();

            for (InstanceWithNamedPorts instance : instanceGroupsClient.listInstances(requestList).iterateAll()) {
                String instanceUrl = instance.getInstance();
                String[] parts = instanceUrl.split("/");
                String instanceName = parts[parts.length - 1];

                Instance instanceDetails = instancesClient.get(PROJECT_ID, zone, instanceName);
                if (instanceDetails.getStatus().equals("RUNNING")) {
                    String ip = instanceDetails.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                    if (ipList.length() > 0) ipList.append(",");
                    ipList.append(ip);
                }
            }
        }

        response.appendHeader("Content-Type", "text/plain");
        writer.write(ipList.toString());
    }
}
