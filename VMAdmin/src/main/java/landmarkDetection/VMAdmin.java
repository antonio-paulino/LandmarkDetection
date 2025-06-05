package landmarkDetection;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

public class VMAdmin {

    private static final Logger logger = LoggerFactory.getLogger(VMAdmin.class);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean exit = false;
        VMService vmService = new VMService();

        String PROJECT_ID = "cn2425-t1-g11";
        String ZONE = "europe-southwest1-a";

        while (!exit) {
            try {
                int option = showMenu();

                switch (option) {
                    case 1 -> vmService.listVMInstances(PROJECT_ID, ZONE);
                    case 2 -> vmService.listManagedInstanceGroups(PROJECT_ID, ZONE);
                    case 3 -> {
                        List<String> groupNames = vmService.getManagedInstanceGroupNames(PROJECT_ID, ZONE);
                        if (groupNames.isEmpty()) {
                            System.out.println("No managed instance groups found.");
                            break;
                        }

                        System.out.println("\nAvailable Instance Groups:");
                        for (int i = 0; i < groupNames.size(); i++) {
                            System.out.printf(" %d - %s%n", i + 1, groupNames.get(i));
                        }

                        System.out.println();
                        System.out.print("Choose a group to resize (enter number): ");
                        int index = Integer.parseInt(scanner.nextLine()) - 1;

                        if (index < 0 || index >= groupNames.size()) {
                            System.out.println("Invalid selection.");
                            break;
                        }

                        String selectedGroup = groupNames.get(index);

                        System.out.print("Enter new desired size: ");
                        int size = Integer.parseInt(scanner.nextLine());

                        vmService.resizeManagedInstanceGroup(PROJECT_ID, ZONE, selectedGroup, size);
                    }
                    case 99 -> {
                        logger.info("Exiting VMAdmin...");
                        exit = true;
                    }
                    default -> System.out.println("Invalid option.");
                }

            } catch (Exception ex) {
                logger.error("Error during operation: {}", ex.getMessage(), ex);
            }
        }
        scanner.close();
    }


    private static int showMenu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println("\n======= VM Admin Menu =======");
            System.out.println(" 1 - List VM Instances");
            System.out.println(" 2 - List Managed Instance Groups");
            System.out.println(" 3 - Resize Managed Instance Group");
            System.out.println("99 - Exit");
            System.out.print("Choose an option: ");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }
}
