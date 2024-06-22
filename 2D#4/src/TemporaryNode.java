// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Abdul Rahmatzada
// 220004497
// abdul.rahmatzada@city.ac.uk


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

// DO NOT EDIT starts
interface TemporaryNodeInterface {
    public boolean start(String startingNodeName, String startingNodeAddress);
    public boolean store(String key, String value);
    public String get(String key);
}
// DO NOT EDIT ends


public class TemporaryNode implements TemporaryNodeInterface {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nodeName;


    public boolean start(String startingNodeName, String startingNodeAddress) {
        try {

            nodeName = startingNodeName;

            // Parse the starting node address
            String[] addressParts = startingNodeAddress.split(":");
            if (addressParts.length != 2) {
                System.err.println("Invalid startingNodeAddress format. Expected format: <IP>:<Port>");
                return false;
            }
            String ip = addressParts[0];
            int port = Integer.parseInt(addressParts[1]);

            // Establish a TCP connection to the starting node
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send the START message
            out.println("START 1 " + startingNodeName); // Protocol version is assumed to be 1

            // Await the START response
            String startResponse = in.readLine();
            if (startResponse == null || !startResponse.startsWith("START")) {
                System.err.println("Invalid or no START response received.");
                return false;
            }

            // At this point, the connection and protocol initiation are successful
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start connection: " + e.getMessage());
            // Clean up resources
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Exception ex) {
                System.err.println("Failed to close resources: " + ex.getMessage());
            }
            return false;
        }
    }

    public boolean store(String key, String value) {
        try {
            // Ensure key and value end with newline character
            if (!key.endsWith("\n")) {
                key += "\n";
            }
            if (!value.endsWith("\n")) {
                value += "\n";
            }

            // Compute key's hashID
            String hashID = bytesToHex(HashID.computeHashID(key));
            System.out.println("the key's hash " + hashID);

            // Find nearest nodes
            List<NodeInfo> nearestNodes = findNearestNodes(hashID);

                // Store on remote node
                for (NodeInfo node : nearestNodes) {
                    System.out.println("node being stored or smth " + node.name + " " + node.address);
                    boolean storeSuccess = attemptStoreOnNode(node.getName(), node.getAddress(), key, value);
                    if (storeSuccess) {
                        return true;
                    }
                }
                return false;
        } catch (Exception e) {
            System.err.println("Failed to store key-value pair: " + e.getMessage());
            return false;
        } finally {
            // Clean up resources
            closeResources();
        }
    }


    // closeResources method
    private void closeResources() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to close resources: " + e.getMessage());
        }
    }


    private boolean attemptStoreOnNode(String nodeName, String nodeAddress, String key, String value) {
        Socket remoteSocket = null;
        BufferedReader remoteIn = null;
        PrintWriter remoteOut = null;

        try {
            // Establish TCP connection to remote node
            remoteSocket = new Socket(nodeAddress.split(":")[0], Integer.parseInt(nodeAddress.split(":")[1]));
            remoteIn = new BufferedReader(new InputStreamReader(remoteSocket.getInputStream()));
            remoteOut = new PrintWriter(remoteSocket.getOutputStream(), true);

            // Send START message
            remoteOut.println("START 1 " + nodeName);
            System.out.println("Sent START message: START 1 " + nodeName);

            // Await START response
            String startResponse = remoteIn.readLine();
            System.out.println("Received START response: " + startResponse);

            if (startResponse == null || !startResponse.startsWith("START")) {
                throw new Exception("Invalid or no START response received from remote node.");
            }

            // Send PUT? request
            String putRequest = constructPutRequest(key, value);
            remoteOut.println(putRequest);
            System.out.println("Sent PUT? request: " + putRequest);


            // Await response
            String response = remoteIn.readLine();
            System.out.println("Received response for PUT?: " + response);


            if (response.equals("SUCCESS")) {
                return true;
            } else {
                System.out.println("Failed Response received");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to store key-value pair on remote node: " + e.getMessage());
            return false;
        } finally {
            // Clean up resources
            try {
                if (remoteSocket != null) {
                    remoteSocket.close();
                }
                if (remoteIn != null) {
                    remoteIn.close();
                }
                if (remoteOut != null) {
                    remoteOut.close();
                }
                System.out.println("cleaning up resources");
            } catch (Exception e) {
                System.err.println("Failed to close remote resources: " + e.getMessage());
            }
        }
    }

    private List<NodeInfo> findNearestNodes(String hashID) {
        try {
            // Send NEAREST? request to current node
            out.println("NEAREST? " + hashID);

            System.out.println("Sending a nearest request to " + hashID);

            // Read NODES response
            String nodesResponse = in.readLine();
            System.out.println("Received response for NEAREST?: " + nodesResponse);
            if (nodesResponse == null || !nodesResponse.startsWith("NODES")) {
                throw new Exception("Invalid or no NODES response received.");
            }

            // Parse NODES response to extract node names and addresses
            String[] nodesParts = nodesResponse.split(" ");
            int numNodes = Integer.parseInt(nodesParts[1]);
            List<NodeInfo> nearestNodes = new ArrayList<>();
            for (int i = 0; i < numNodes; i++) {
                String nodeName = in.readLine();
                String nodeAddress = in.readLine();
                System.out.println("node name + address being added to map " + nodeName + " " + nodeAddress);
                nearestNodes.add(new NodeInfo(nodeName, nodeAddress));
            }

            return nearestNodes;
        } catch (Exception e) {
            System.err.println("Failed to find nearest nodes: " + e.getMessage());
            return Collections.emptyList();
        }
    }



    // NodeInfo class
    private static class NodeInfo {
        private String name;
        private String address;

        public NodeInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }
    }

    // constructPutRequest method
    private String constructPutRequest(String key, String value) {
        int keyLines = key.split("\n").length;
        int valueLines = value.split("\n").length;
        return "PUT? " + keyLines + " " + valueLines + "\n" + key + value;
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    public String get(String key) {
        try {

            System.out.println("Getting value for key: " + key.trim());

            // Ensure key ends with newline character
            if (!key.endsWith("\n")) {
                key += "\n";
            }

            // Compute key's hashID
            String hashID = bytesToHex(HashID.computeHashID(key));
            System.out.println("Key hashID: " + hashID);

            // Find the nearest nodes
            List<NodeInfo> nearestNodes = findNearestNodes(hashID);
            System.out.println("Nearest nodes found: " + nearestNodes.size());

            // Check if the current node is in the list of nearest nodes
            boolean isCurrentNodeInNearestNodes = nearestNodes.stream()
                    .anyMatch(node -> node.getName().equals(nodeName));
            System.out.println("Is current node in nearest nodes: " + isCurrentNodeInNearestNodes);

                System.out.println("Retrieving value remotely");
                // If the current node is not in the nearest nodes, send GET? request to the nearest nodes
                for (NodeInfo node : nearestNodes) {
                    System.out.println("Attempting retrieval from node: " + node.getName());
                    String value = getRemoteValue(node, key);
                    System.out.println("Remote value: " + value);
                    if (value != null) {
                        return value;
                    }
                }
                System.out.println("Value not found on any nearest node");
                return null;

        } catch (Exception e) {
            System.err.println("Failed to get value for key: " + e.getMessage());
            return null;
        }
    }


    private String getRemoteValue(NodeInfo node, String key) {
        try {
            System.out.println("Establishing TCP connection to remote node: " + node.getName());
            // Establish TCP connection to the remote node
            Socket remoteSocket = new Socket(node.getAddress().split(":")[0], Integer.parseInt(node.getAddress().split(":")[1]));
            BufferedReader remoteIn = new BufferedReader(new InputStreamReader(remoteSocket.getInputStream()));
            PrintWriter remoteOut = new PrintWriter(remoteSocket.getOutputStream(), true);

            System.out.println("Sending START message to remote node: " + node.getName());
            // Send START message
            remoteOut.println("START 1 " + nodeName);

            // Await START response
            String startResponse = remoteIn.readLine();
            System.out.println("START response from remote node: " + startResponse);
            if (startResponse == null || !startResponse.startsWith("START")) {
                throw new Exception("Invalid or no START response received from remote node.");
            }

            System.out.println("Sending GET? request to remote node: " + node.getName());
            // Send GET? request
            remoteOut.println("GET? " + key.split("\n").length);
            remoteOut.println(key);

            // Await response
            String response = remoteIn.readLine();
            System.out.println("GET? response from remote node: " + response);
            if (response.startsWith("VALUE")) {
                // Extract the value from the VALUE response
                int valueLines = Integer.parseInt(response.split(" ")[1]);
                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 0; i < valueLines; i++) {
                    valueBuilder.append(remoteIn.readLine());
                    if (i < valueLines - 1) {
                        valueBuilder.append("\n");
                    }
                }
                System.out.println("Remote value: " + valueBuilder.toString());
                return valueBuilder.toString();
            } else if (response.equals("NOPE")) {
                System.out.println("Remote node does not have the value");
                return null;
            } else {
                throw new Exception("Invalid response received from remote node.");
            }
        } catch (Exception e) {
            System.err.println("Failed to get value from remote node: " + e.getMessage());
            return null;
        }
    }
}
