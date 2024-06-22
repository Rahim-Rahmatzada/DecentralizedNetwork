// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Abdul Rahmatzada
// 220004497
// abdul.rahmatzada@city.ac.uk

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends


public class FullNode implements FullNodeInterface {

    private ServerSocket serverSocket;
    private String nodeName;
    private Map<String, String> localFullNodeStore = new HashMap<>();
    private Map<String, NodeInfo> networkMap = new ConcurrentHashMap<>();



    public boolean listen(String ipAddress, int portNumber) {
        try {
            // Create a ServerSocket to listen for incoming connections
            serverSocket = new ServerSocket(portNumber, 0, InetAddress.getByName(ipAddress));
            System.out.println("Full node listening on " + ipAddress + ":" + portNumber);

            // Generate the node name based on the email and additional information
            nodeName = generateNodeName();

            // Start a new thread to handle incoming connections
            Thread connectionThread = new Thread(() -> {
                while (true) {
                    try {
                        // Accept incoming connections
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Incoming connection from " + clientSocket.getInetAddress());

                        // Create input and output streams for communication
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        // Read the START message from the client
                        String startMessage = in.readLine();
                        if (startMessage == null || !startMessage.startsWith("START")) {
                            System.err.println("Invalid START message received");
                            clientSocket.close();
                            continue;
                        }

                        // Send the START response
                        out.println("START 1 " + nodeName);

                        // Handle the incoming request
                        handleRequest(clientSocket, in, out);
                    } catch (Exception e) {
                        System.err.println("Error handling incoming connection: " + e.getMessage());
                    }
                }
            });
            connectionThread.start();

            // Start a new thread for active mapping
            Thread activeMappingThread = new Thread(() -> {
                performActiveMapping();
            });
            activeMappingThread.start();

            Thread nodeValidationThread = new Thread(() -> {
                validateNodes();
            });
            nodeValidationThread.start();

            return true;
        } catch (Exception e) {
            System.err.println("Failed to start listening: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String generateNodeName() {
        return "abdul.rahmatzada@city.ac.uk:FullNode,1.0,prod-node-1";
    }

    private void handleRequest(Socket clientSocket, BufferedReader in, PrintWriter out) {
        try {
            // Read the request from the client
            String request = in.readLine();

            // Handle different types of requests
            if (request.startsWith("PUT?")) {
                handlePutRequest(clientSocket, in, out, request);
            } else if (request.startsWith("GET?")) {
                handleGetRequest(clientSocket, in, out, request);
            } else if (request.startsWith("NEAREST?")) {
                handleNearestRequest(clientSocket, in, out, request);
            } else if (request.startsWith("NOTIFY?")) {
                handleNotifyRequest(clientSocket, in, out, request);
            } else if (request.startsWith("ECHO?")) {
                handleEchoRequest(clientSocket, in, out);
            } else if (request.startsWith("START")) {
                handleStartRequest(clientSocket, in, out, request);
            } else if (request.startsWith("END")) {
                handleEndRequest(clientSocket, in, out, request);
            }
            else {
                System.err.println("Invalid request received: " + request);
            }

            clientSocket.close();
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }

    private void handleEndRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        try {
            String[] requestParts = request.split(" ", 2);
            if (requestParts.length == 2) {
                String reason = requestParts[1];
                System.out.println("Received END request with reason: " + reason);
            } else {
                System.out.println("Received END request without reason.");
            }

            // Close the client socket
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling END request: " + e.getMessage());
        }
    }

    private void handleStartRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        String nodeName = "";
        String[] startParts = request.split(" ");
        if (startParts.length >= 3) {
            nodeName = startParts[2];
        }

        // Add the node to the network map (passive mapping) with the local node's address
        String localNodeAddress = serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
        System.out.println("Adding your own node to your map " + nodeName + " " + localNodeAddress);
        addNodeToNetworkMap(nodeName, localNodeAddress);

        // Send the START response
        out.println("START 1 " + nodeName);
    }

    private void handlePutRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        try {
            System.out.println("Handling PUT? request...");

            // Parse the PUT? request
            String[] requestParts = request.split(" ");
            int keyLines = Integer.parseInt(requestParts[1]);
            int valueLines = Integer.parseInt(requestParts[2]);

            System.out.println("Key lines: " + keyLines);
            System.out.println("Value lines: " + valueLines);

            // Read the key and value from the client
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < keyLines; i++) {
                keyBuilder.append(in.readLine()).append("\n");
            }
            String key = keyBuilder.toString();

            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < valueLines; i++) {
                valueBuilder.append(in.readLine()).append("\n");
            }
            String value = valueBuilder.toString();

            System.out.println("Received PUT? request:");
            System.out.println("Key: " + key.trim());
            System.out.println("Value: " + value.trim());

            // Store the key-value pair locally
            storeLocalValue(key, value);
            out.println("SUCCESS");
        } catch (Exception e) {
            System.err.println("Error handling PUT? request: " + e.getMessage());
            out.println("FAILED");
        }
    }

    private void handleGetRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        try {
            System.out.println("Handling GET? request...");

            // Parse the GET? request
            String[] requestParts = request.split(" ");
            int keyLines = Integer.parseInt(requestParts[1]);

            System.out.println("Key lines: " + keyLines);

            // Read the key from the client
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < keyLines; i++) {
                keyBuilder.append(in.readLine()).append("\n");
            }
            String key = keyBuilder.toString();

            System.out.println("Requested key: " + key.trim());

            // Check if the current node has the value stored locally
            String value = getLocalValue(key);
            if (value != null) {
                System.out.println("Value found locally.");

                // Return the value to the client
                out.println("VALUE " + value.split("\n").length);
                out.println(value);
            } else {
                System.out.println("Value not found locally.");
                out.println("NOPE");
            }
        } catch (Exception e) {
            System.err.println("Error handling GET? request: " + e.getMessage());
            out.println("NOPE");
        }
    }

    private void handleNearestRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        try {
            System.out.println("Handling NEAREST? request...");

            // Parse the NEAREST? request
            String[] requestParts = request.split(" ");
            String targetHashID = requestParts[1];

            System.out.println("Target HashID: " + targetHashID);

            // Find the closest nodes to the target hashID
            List<NodeInfo> closestNodes = findNearestNodes(targetHashID);

            System.out.println("Number of closest nodes found: " + closestNodes.size());


            // Construct the NODES response
            StringBuilder response = new StringBuilder();
            response.append("NODES ").append(closestNodes.size()).append("\n");
            for (NodeInfo node : closestNodes) {
                response.append(node.getName());
                response.append(node.getAddress());
            }

            System.out.println("Constructed NODES response:");
            System.out.println(response.toString());

            // Send the NODES response to the client
            out.println(response.toString().trim());
        } catch (Exception e) {
            System.err.println("Error handling NEAREST? request: " + e.getMessage());
            out.println("NODES 0");
        }
    }

    private void handleNotifyRequest(Socket clientSocket, BufferedReader in, PrintWriter out, String request) {
        try {
            System.out.println("Handling NOTIFY request...");

            // Read the node name and address from the request
            System.out.println("Reading node name from the request...");
            String nodeName = in.readLine();
            System.out.println("Node name read: " + nodeName);

            System.out.println("Reading node address from the request...");
            String nodeAddress = in.readLine();
            System.out.println("Node address read: " + nodeAddress);

            // Add the node to the network map
            System.out.println("Adding node to the network map...");
            addNodeToNetworkMap(nodeName, nodeAddress);
            System.out.println("Node added to the network map: " + nodeName + ", Address: " + nodeAddress);

            // Send the NOTIFIED response to the client
            System.out.println("Sending NOTIFIED response to the client...");
            out.println("NOTIFIED");
            System.out.println("NOTIFIED response sent.");

        } catch (Exception e) {
            System.err.println("Error handling NOTIFY request: " + e.getMessage());
        }
    }

    private void handleEchoRequest(Socket clientSocket, BufferedReader in, PrintWriter out) {
        out.println("OHCE");
    }


    private List<NodeInfo> findNearestNodes(String hashID) {
        List<NodeInfo> nearestNodes = new ArrayList<>();
        List<Pair<Integer, NodeInfo>> nodeDistances = new ArrayList<>();

        // Iterate through the network map and calculate the distances
        for (Map.Entry<String, NodeInfo> entry : networkMap.entrySet()) {
            String nodeHashID = entry.getKey();
            NodeInfo nodeInfo = entry.getValue();

            // Calculate the distance between the node's hashID and the given hashID
            int distance = NodeUtils.computeDistance(nodeHashID, hashID);

            // Add the distance and node information to the nodeDistances list
            nodeDistances.add(new Pair<>(distance, nodeInfo));
        }

        // Sort the nodeDistances list based on the distances in ascending order
        Collections.sort(nodeDistances, Comparator.comparingInt(Pair::getFirst));

        // Take the top three nodes with the smallest distances
        for (int i = 0; i < Math.min(3, nodeDistances.size()); i++) {
            nearestNodes.add(nodeDistances.get(i).getSecond());
        }

        return nearestNodes;
    }

    private void performActiveMapping() {
        try {
            int iteration = 1;

            while (true) {

                // Generate a random hash for querying
                String randomHash = generateRandomHash();

                // Iterate through the network map
                for (Map.Entry<String, NodeInfo> entry : networkMap.entrySet()) {
                    NodeInfo nodeInfo = entry.getValue();

                    // Skip sending NEAREST? request to the current node
                    if (nodeInfo.getAddress().trim().equals(serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort())) {
                        continue;
                    }


                    try {
                        // Establish a TCP connection to the node
                        String[] addressParts = nodeInfo.getAddress().trim().split(":");
                        String ip = addressParts[0];
                        int port = Integer.parseInt(addressParts[1]);
                        Socket nodeSocket = new Socket(ip, port);
                        BufferedReader nodeIn = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);

                        nodeOut.println("START 1 " + nodeName);
                        String startResponse = nodeIn.readLine();
                        if (startResponse == null || !startResponse.startsWith("START")) {
                            throw new Exception("Invalid or no START response received from node.");
                        }

                        // Send NEAREST? request
                        nodeOut.println("NEAREST? " + randomHash);

                        // Read NODES response
                        String nodesResponse = nodeIn.readLine();

                        if (nodesResponse != null && nodesResponse.startsWith("NODES")) {
                            String[] nodesParts = nodesResponse.split(" ");
                            int numNodes = Integer.parseInt(nodesParts[1]);

                            // Parse node names and addresses from the response
                            for (int i = 0; i < numNodes; i++) {
                                String receivedNodeName = nodeIn.readLine();
                                String receivedNodeAddress = nodeIn.readLine();

                                // Add a newline character to the receivedNodeName
                                receivedNodeName += "\n";

                                // Add the node to the network map if it's not already present
                                String nodeHashID = NodeUtils.bytesToHex(HashID.computeHashID(receivedNodeName));
                                if (!networkMap.containsKey(nodeHashID)) {
                                    addNodeToNetworkMap(receivedNodeName, receivedNodeAddress);
                                }
                            }
                        }
                        // Close the node socket
                        nodeSocket.close();
                    } catch (IOException e) {
                        System.err.println("IO error occurred while processing node: " + nodeInfo.getName().trim());
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number for node: " + nodeInfo.getName().trim());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println("Error processing node: " + nodeInfo.getName().trim() + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                iteration++;

                // Sleep for a certain interval before the next iteration
                Thread.sleep(5000); // Sleep for 5 seconds
            }
        } catch (InterruptedException e) {
            System.err.println("Active mapping thread interrupted: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            if (e != null) {
                System.err.println("Error in active mapping: " + e.getMessage());
                e.printStackTrace();
            } else {
                System.err.println("Error in active mapping: null exception caught ");
            }
        }
    }

    private String generateRandomHash() {
        // Generate a random 256-bit hash
        byte[] randomBytes = new byte[32];
        new Random().nextBytes(randomBytes);
        return NodeUtils.bytesToHex(randomBytes);
    }

    private void storeLocalValue(String key, String value) {
        localFullNodeStore.put(key, value);
        System.out.println("Storing locally:");
        System.out.println("Key: " + key.trim());
        System.out.println("Value: " + value.trim());
        System.out.println("Local store contents: " + localFullNodeStore);
    }

    private String getLocalValue(String key) {
        String value = localFullNodeStore.get(key);
        System.out.println("Local value for key " + key.trim() + ": " + value);
        return value;
    }


    private void addNodeToNetworkMap(String nodeName, String nodeAddress) {
        try {
            // Ensure node name and address end with a newline character
            if (!nodeName.endsWith("\n")) {
                nodeName += "\n";
            }
            if (!nodeAddress.endsWith("\n")) {
                nodeAddress += "\n";
            }

            String nodeHashID = NodeUtils.bytesToHex(HashID.computeHashID(nodeName));
            NodeInfo newNodeInfo = new NodeInfo(nodeName, nodeAddress);

            // Compute the distance of the new node from the current node
            String currentNodeHashID = NodeUtils.bytesToHex(HashID.computeHashID(this.nodeName + "\n"));
            int newNodeDistance = NodeUtils.computeDistance(nodeHashID, currentNodeHashID);

            // Check if there are already 3 nodes at the same distance
            Map<Integer, List<NodeInfo>> nodesByDistance = new ConcurrentSkipListMap<>();
            for (NodeInfo existingNodeInfo : networkMap.values()) {
                String existingNodeHashID = NodeUtils.bytesToHex(HashID.computeHashID(existingNodeInfo.getName()));
                int existingNodeDistance = NodeUtils.computeDistance(existingNodeHashID, currentNodeHashID);
                nodesByDistance.computeIfAbsent(existingNodeDistance, k -> new ArrayList<>()).add(existingNodeInfo);
            }

            List<NodeInfo> nodesAtSameDistance = nodesByDistance.getOrDefault(newNodeDistance, new ArrayList<>());
            if (nodesAtSameDistance.size() < 3) {
                // Add the new node to the network map
                networkMap.put(nodeHashID, newNodeInfo);
                nodesAtSameDistance.add(newNodeInfo);
                nodesByDistance.put(newNodeDistance, nodesAtSameDistance);
                System.out.println("Node added to active map:");
                System.out.println("Name: " + nodeName.trim());
                System.out.println("Address: " + nodeAddress.trim());
            }
        } catch (Exception e) {
            System.err.println("Error adding node to network map: " + e.getMessage());
        }
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        try {
            System.out.println("Handling incoming connections...");

            // Parse the starting node address
            String[] addressParts = startingNodeAddress.split(":");
            if (addressParts.length != 2) {
                System.err.println("Invalid startingNodeAddress format. Expected format: <IP>:<Port>");
                return;
            }
            String ip = addressParts[0];
            int port = Integer.parseInt(addressParts[1]);
            System.out.println("Starting node address: " + ip + ":" + port);

            // Establish a TCP connection to the starting node
            System.out.println("Connecting to starting node...");
            Socket nodeSocket = new Socket(ip, port);
            System.out.println("Connected to starting node.");
            BufferedReader nodeIn = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
            PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);

            handleStartRequest(nodeSocket, nodeIn, nodeOut, "START 1 " + nodeName);

            // Add the starting node to the network map
            System.out.println("Adding initial node to the network map: " + startingNodeName + " " + startingNodeAddress);
            addNodeToNetworkMap(startingNodeName, startingNodeAddress);

            // Await START response
            System.out.println("Waiting for START response...");
            String startResponse = nodeIn.readLine();
            System.out.println("START response received: " + startResponse);
            if (startResponse == null || !startResponse.startsWith("START")) {
                throw new Exception("Invalid or no START response received from starting node.");
            }

            // Send NOTIFY request
            System.out.println("Sending NOTIFY request...");
            nodeOut.println("NOTIFY?");
            System.out.println("NOTIFY? request sent.");
            nodeOut.println(nodeName);
            System.out.println("Node name sent: " + nodeName);
            nodeOut.println(serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
            System.out.println("Node address sent: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
            System.out.println("NOTIFY request sent.");

            // Await NOTIFIED response
            System.out.println("Waiting for NOTIFIED response...");
            String notifiedResponse = nodeIn.readLine();
            System.out.println("NOTIFIED response received: " + notifiedResponse);
            if (notifiedResponse == null || !notifiedResponse.equals("NOTIFIED")) {
                throw new Exception("Invalid or no NOTIFIED response received from starting node.");
            }

            // Close the node socket
            nodeSocket.close();
            System.out.println("Node socket closed.");

            System.out.println("Successfully connected to the 2D#4 network.");
        } catch (Exception e) {
            System.err.println("Error connecting to the 2D#4 network: " + e.getMessage());
        }
    }

    private void validateNodes() {
        try {
            while (true) {
                List<NodeInfo> nodesToRemove = new ArrayList<>();

                // Iterate through the network map
                for (Map.Entry<String, NodeInfo> entry : networkMap.entrySet()) {
                    NodeInfo nodeInfo = entry.getValue();

                    // Skip checking the current node
                    if (nodeInfo.getAddress().trim().equals(serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort())) {
                        continue;
                    }

                    try {
                        // Establish a TCP connection to the node
                        String[] addressParts = nodeInfo.getAddress().trim().split(":");
                        String ip = addressParts[0];
                        int port = Integer.parseInt(addressParts[1]);
                        Socket nodeSocket = new Socket(ip, port);
                        BufferedReader nodeIn = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);

                        nodeOut.println("START 1 " + nodeName);
                        String startResponse = nodeIn.readLine();
                        if (startResponse == null || !startResponse.startsWith("START")) {
                            throw new Exception("Invalid or no START response received from node.");
                        }

                        // Send ECHO? request
                        nodeOut.println("ECHO?");

                        // Read OHCE response
                        String echoResponse = nodeIn.readLine();
                        if (!echoResponse.equals("OHCE")) {
                            // Incorrect response, add node to the list of nodes to remove
                            nodesToRemove.add(nodeInfo);
                        }

                        // Close the node socket
                        nodeSocket.close();
                    } catch (IOException e) {
                        System.err.println("IO error occurred while validating node: " + nodeInfo.getName().trim());
                        e.printStackTrace();
                        // Add node to the list of nodes to remove
                        nodesToRemove.add(nodeInfo);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number for node: " + nodeInfo.getName().trim());
                        e.printStackTrace();
                        // Add node to the list of nodes to remove
                        nodesToRemove.add(nodeInfo);
                    } catch (Exception e) {
                        System.err.println("Error validating node: " + nodeInfo.getName().trim() + " - " + e.getMessage());
                        e.printStackTrace();
                        // Add node to the list of nodes to remove
                        nodesToRemove.add(nodeInfo);
                    }
                }

                // Remove nodes from the network map
                for (NodeInfo nodeToRemove : nodesToRemove) {
                    String nodeHashID = NodeUtils.bytesToHex(HashID.computeHashID(nodeToRemove.getName()));
                    networkMap.remove(nodeHashID);
                    System.out.println("Removed node from the network map: " + nodeToRemove.getName().trim());
                }

                // Sleep for a certain interval before the next validation
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Node validation thread interrupted: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            if (e != null) {
                System.err.println("Error in node validation: " + e.getMessage());
                e.printStackTrace();
            } else {
                System.err.println("Error in node validation: null exception caught ");
            }
        }
    }


    private static class Pair<K, V> {
        private K first;
        private V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public K getFirst() {
            return first;
        }

        public V getSecond() {
            return second;
        }
    }

}
