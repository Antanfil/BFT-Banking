# BFT-Banking

Bisantine Fault Tolerant System for Banking

# Demo

In this Demo we will exemplify the different functionalities of the developed project.


###Key Generation:
We will now create a couple of keys for our client to use and for the accounts:

`cd client`

**First Client:**

`./keystore.sh`
(When asked insert your alias and password)
>alias: client_1
clientID: 1
password = password


**First Account (for client 1):**
./keystore.sh
(When asked insert your alias and password)
>alias: acc_1
clientID: 1
password: password

**Second Account (for client 1):**
./keystore.sh
(When asked insert your alias and password)
>alias: acc_2
clientID: 1
password: password


**Server keys**:

Go to the Home directory ( cd .. )

And create 3 server keys

`cd server
`

`./keystore`
>alias: server1
password: password

`./keystore`
>alias: server2
password: password

`./keystore`
>alias: server3
password: password



### Next Steps:

Execute the following commands in the project Home Directory:

`cd server-contract
`

`
mvn clean install
`

`cd ..
`

`mvn clean compile
`

`
mvn clean install
`


### Initializing Server

We will now initialize the necessary servers (on different terminals):
* starting on localhost port 8080 (make sure this address is not already in use);
* the name of the keystore 
* the integer representing the number of servers in this case 3

`mvn clean compile exec:java -Dexec.args="localhost 8080 server1.p12 3"`

`mvn clean compile exec:java -Dexec.args="localhost 8081 server2.p12 3"`

`mvn clean compile exec:java -Dexec.args="localhost 8082 server3.p12 3"`

As we can start more than one server, after the initialization, we must press the enter key to proceed with the 
broadcast of public keys across servers.

After all servers are up and running press "enter" on each server

### **Initializing Client1**

We will now initialize Client 1 .

Open a new terminal.

Change to the Home Directory ( cd .. )

Change to the Client Directory ( cd client )

Run the following command to start a client:

`mvn clean compile exec:java -Dexec.args="localhost 8080 1"`

The integer that follows the port represents the maximum number of servers that can be down at any given time. In this case since we have 3 server's running we can only tolerate 1 server to be down in other to guarantee the security properties of our system.

When asked insert "1 password" ( corresponds to clientId=1 and password=password)

## **Creating Client1's accounts**

**IMPORTANT! DENIAL OF SERVICE** - In order to prevent DDOS attack we implemented a POW system in our servers. As such after each operation in the client, the user will be prompted a simple question (random everytime) to solve. Make sure to type the anwser to this question whenever prompted and press enter.


**IMPORTANT! Fault tolerance** - In order to test how our system works with servers we suggesting utilizing signals such as **SIGTSTP** (ctrl+z) to stop the servers and the command **fg** to resume the servers.


Run the commands to create 2 accounts:

`1 acc_1 password`

`1 acc_2 password`

### Some examples / tests / operations:

- Lets **check the balance of account 1** by running:

##

    3 acc_1 password 

    //The value should be 50.


- Lets **audit account 2** by running:
##

    5 acc_2 password
        
    //There should be no transactions yet

- Lets **transfer 25 units from acc_1 to acc_2**:
##

    2 acc_1 acc_2 25 password

    //Money should be awaiting confirmation from acc_2

- Lets **accept the incoming transfer**:
##

    4 acc_2 password

    //Money should have entered our account

- Lets **check the balance of both accounts and audit** them:
##

    //(account 1)
    3 acc_1 password
    5 acc_1 password 
    
    //there should be 25 units
    //there should be 1 transaction)


    //(account 2)
    3 acc_2 password
    5 acc_2 password 

    //there should be 75 units 
    //there should be 1 transaction

- Lets **try sending too much money from acc_1 to acc_2**:
###

    2 acc_1 acc_2 1000 password

    //there should be an error message

- Lets try to **stop the server abruptly and restart it** to check if it keeps the same state:

###

    (Control-C on Server) Send the interrupt (terminate) signal SIGINT
        
    // Restart both the client and the server as done previously

    // Do the following commands on the client

    2 acc_1 password
    
    // balance should still be 25

- Shutdown the Client gracefully:
##

    6




- If you wish to **reset the state of the servers** delete the following file:

`PROJECT_HOME_DIR/server/server1.txt`
`PROJECT_HOME_DIR/server/server2.txt`
`PROJECT_HOME_DIR/server/server3.txt`


