## TemporaryNode.java

The `TemporaryNode.java` class allows you to:

- Connect to the 2D#4 network 
- Find the full node with the hashID closest to the given hashID
- Store a (key, value) pair correctly handling all responses
- Given a key, find the value from the network

## FullNode.java

The `FullNode.java` class provides the following functionality:

- Connects to the 2D#4 network and notifies other full nodes of my node's address 
- Accepting incoming connections
- Responds to ECHO? correctly 
- Responds to PUT? correctly
- Responds to GET? correctly
- Passive mapping of the network 
- Active mapping of the network
- Responds to NEAREST? correctly 

## Compilation and Testing

To compile and test the project, follow these steps:

1. Compile with the following command `javac *.java`
2. When testing the `FullNode class`, the node's name is set to: `abdul.rahmatzada@city.ac.uk:FullNode,1.0,prod-node-1`