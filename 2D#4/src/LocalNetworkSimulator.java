import java.util.ArrayList;
import java.util.List;

public class LocalNetworkSimulator {
    private String ipAddress;
    private int startingPort;
    private int numNodes;
    private List<FullNodeInterface> nodes;

    public LocalNetworkSimulator(String ipAddress, int startingPort, int numNodes) {
        this.ipAddress = ipAddress;
        this.startingPort = startingPort;
        this.numNodes = numNodes;
        this.nodes = new ArrayList<>();
    }

    public void startNetwork() {
        try {
            // Create and start the initial node
            FullNodeInterface initialNode = createInitialNode();
            nodes.add(initialNode);

            // Create and connect the remaining nodes to the initial node
            connectRemainingNodes();

            System.out.println("Local network simulation started with " + numNodes + " nodes.");
        } catch (Exception e) {
            System.err.println("Error starting the local network simulation: " + e.getMessage());
        }
    }

    private FullNodeInterface createInitialNode() {
        FullNodeInterface initialNode = new FullNode();
        String initialNodeAddress = ipAddress + ":" + startingPort;
        initialNode.listen(ipAddress, startingPort);
        System.out.println("Initial node started: " + initialNodeAddress);
        return initialNode;
    }

    private void connectRemainingNodes() {
        String initialNodeName = generateNodeName(0);
        String initialNodeAddress = ipAddress + ":" + startingPort;

        for (int i = 1; i < numNodes; i++) {
            int port = startingPort + i;
            String nodeAddress = ipAddress + ":" + port;
            String nodeName = generateNodeName(i);

            FullNodeInterface node = createFullNode(ipAddress, port);
            node.handleIncomingConnections(initialNodeName, initialNodeAddress);
            nodes.add(node);
            System.out.println("Node connected: " + nodeName + " (" + nodeAddress + ")");
        }
    }

    private FullNodeInterface createFullNode(String ipAddress, int port) {
        FullNodeInterface node = new FullNode();
        node.listen(ipAddress, port);
        return node;
    }

    private String generateNodeName(int index) {
        return "LocalNode-" + index;
    }

    public void stopNetwork() {
        // The FullNodeInterface doesn't provide a way to stop the nodes,
        // so we can't implement this method without modifying the FullNode class.
        System.out.println("Stopping the local network simulation is not supported.");
    }

    public static void main(String[] args) {
        // Check if the required command-line arguments are provided
        if (args.length != 3) {
            System.err.println("Usage: java LocalNetworkSimulator <ip_address> <starting_port> <num_nodes>");
            return;
        }

        String ipAddress = args[0];
        int startingPort = Integer.parseInt(args[1]);
        int numNodes = Integer.parseInt(args[2]);

        // Create and start the local network simulation
        LocalNetworkSimulator simulator = new LocalNetworkSimulator(ipAddress, startingPort, numNodes);
        simulator.startNetwork();

        // Keep the program running until interrupted (e.g., by pressing Ctrl+C)
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            simulator.stopNetwork();
        }
    }
}