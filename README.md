# 2D#4 Distributed Hash Table

This repository contains an implementation of the 2D#4 (Two-Dee-Hash-Four) protocol, a peer-to-peer distributed hash table. The protocol allows nodes to participate in a decentralized network and store and retrieve key-value pairs.

## Overview

The 2D#4 protocol is designed to create a distributed hash table across a set of nodes. Each node in the network stores a portion of the hash table entries. The system is fault-tolerant, scalable, and does not require central coordination.

Nodes communicate over TCP using a simple request-response protocol. The protocol specification is defined in the [2D#4 RFC].

## Implementation

The repository includes two main components:

1. `FullNode`: Represents a full participant in the 2D#4 network. It accepts incoming connections, handles requests, stores key-value pairs, and maintains a network map.

2. `TemporaryNode`: Represents a limited participant in the 2D#4 network. It can connect to the network, store key-value pairs, and retrieve values for a given key.

The implementation is written in Java and adheres to the protocol specification outlined in the RFC.

## Getting Started

To run the 2D#4 nodes, follow these steps:

1. Clone the repository:

2. Build the project:

3. Compile and run the 'LocalNetworkSimulator' class with the desired IP address, starting port number, and the number of full nodes:

## Testing

The repository includes test programs (`CmdLineFullNode`, `CmdLineStore`, `CmdLineGet`) that demonstrate how to use the `FullNode` and `TemporaryNode` objects. You can use these programs to test your implementation and verify its functionality.

## Acknowledgements

The 2D#4 protocol and implementation are based on the coursework assignment for the IN2011 Computer Networks module at City, University of London.
