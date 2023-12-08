# Demo application

Written in kotlin + Akka frameword

![Architecture](Ateam.svg)

The demo application simulates communication between hardware devices using the TCP protocol. 
The devices can be either publishers or subscribers. Messages sent by publishers are received by subscribers. 
After establishing a connection, it becomes clear whether the connecting party is a publisher or a subscriber 
based on the SUBSCRIBER or PUBLISHER message.

After starting the program, it can be easily tested, for example, with the telnet application.

The tests support the BDD/Cucumber framework.
