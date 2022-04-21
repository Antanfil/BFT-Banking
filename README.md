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

`cd server
`

`./keystore`
>alias: server
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

We will now initialize the necessary servers:
* on localhost port 8080 (make sure this address is not already in use);
* the name of yhe keystore 
* the integer representing the maximum number of tolerated Byzantine faults, in this case 1, indicating that will be 
  initialized 3 server's replicas:

`mvn clean compile exec:java -Dexec.args="localhost 8080 server1.p12 1"`

As we can start more than one server, after the initialization, we must press the enter key to proceed with the 
broadcast of public keys across servers.

### Initializing Client1

We will now initialize Client 1 .

Open a new terminal.

Change to the Home Directory ( cd .. )

Change to the Client Directory ( cd client )

Run the following command to start a client:

`mvn clean compile exec:java -Dexec.args="localhost 8080 1"`

The integer that follows the port represents the same thing as no server, the maximum number of acceptable Byzantine 
failures.

When asked insert "1 password" ( corresponds to clientId=1 and password=password)

### Creating Client1's accounts

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




- If you wish to **reset the state of the server** delete the following file:

`PROJECT_HOME_DIR/server/server.txt`

