package landmarkDetection;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VMService {

    public void listVMInstances(String project, String zone) throws IOException {
        try (InstancesClient client = InstancesClient.create()) {
            System.out.println("==== Listing VM instances in zone: " + zone);
            for (Instance instance : client.list(project, zone).iterateAll()) {
                System.out.println("Name: " + instance.getName() + ", VM ID: " + instance.getId());
                String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                System.out.println("IP: " + ip + ", Status: " + instance.getStatus() +
                        ", Last Start: " + instance.getLastStartTimestamp());
            }
        }
    }

    public void resizeManagedInstanceGroup(String project, String zone, String groupName, int newSize)
            throws IOException, ExecutionException, InterruptedException {
        try (InstanceGroupManagersClient client = InstanceGroupManagersClient.create()) {
            OperationFuture<Operation, Operation> future = client.resizeAsync(project, zone, groupName, newSize);
            Operation result = future.get();
            System.out.println("Resizing completed with status: " + result.getStatus());
        }
    }

    void listManagedInstanceGroups(String project, String zone) throws IOException {
        try (InstanceGroupManagersClient managersClient = InstanceGroupManagersClient.create()) {
            for (InstanceGroupManager manager : managersClient.list(project, zone).iterateAll()) {
                System.out.println("Name: " + manager.getName());
                System.out.println("Template: " + manager.getInstanceTemplate());
            }
        }
    }

    public List<String> getManagedInstanceGroupNames(String project, String zone) throws IOException {
        List<String> groupNames = new ArrayList<>();
        try (InstanceGroupManagersClient client = InstanceGroupManagersClient.create()) {
            for (InstanceGroupManager manager : client.list(project, zone).iterateAll()) {
                groupNames.add(manager.getName());
            }
        }
        return groupNames;
    }

}
