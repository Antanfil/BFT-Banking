package pt.tecnico.sec.server;



import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirstTest {

    static ServerSetup _server;

    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() {
        String host = "localhost";
        int port1 = 8081;


        ServerSetup _server1 = null;

        try {
            _server = new ServerSetup( host , port1, 3 );
            _server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String id1 = "1";
        String clientPK1 = "MOCKPUBLICKEY1"
        ClientS client1 = new ClientS(id1, clientPK1);

        String id2 = "2";
        String clientPK2 = "MOCKPUBLICKEY2"
        ClientS client2 = new ClientS(id2, clientPK2);

        _server1.exchangeKeys(id, clientPK);

    }

    @Test
    public void openAccountOKTest() {
        String accountPK1 = "ACCOUNTPK1";
        int writeTimestamp = "0";
        assertEquals(openAccount(client1, accountPK1, writeTimestamp).split(";")[2], "ACK");
    }

    /*
    using the wrong account public key
     */
    @Test
    public void openAccountNOKTest() {
        String wrongAccountPK = "WRONGACCOUNTPK";
        int writeTimestamp = "0";
        assertEquals(openAccount(client1, wrongAccountPK, writeTimestamp).split(";")[2], "NOK");
    }

    @Test
    public void sendAmountOKTest() {
        int amount = 10;
        int tid = 1;
        int writeTimestamp = "1";
        String accountPK2 = "ACCOUNTPK2";

        assertEquals(sendAmount(client1, accountPK1, accountPK2, amount, tid, writeTimestamp).split(";")[2], "ACK");
    }

    /*
    using the wrong write timestamp
     */
    @Test
    public void sendAmountNOKTest() {
        int amount = 10;
        int tid = 1;
        int writeTimestamp = "0";
        String accountPK2 = "ACCOUNTPK2";

        assertEquals(sendAmount(client1, accountPK1, accountPK2, amount, tid, writeTimestamp).split(";")[2], "NOK");
    }


    @Test
    public void checkAccountOKTest() {
        String accountPK1 = "ACCOUNTPK1";
        int readTimestamp = "1";

        assertEquals(checkAccount(client1, accountPK1, readTimestamp).split(";")[2], "ACK");
    }

    /*
    using the wrong public key
     */
    @Test
    public void checkAccountNOKTest() {
        String accountPK1 = "WRONGACCOUNTPK1";
        int readTimestamp = "0";

        assertEquals(checkAccount(client1, accountPK1, readTimestamp).split(";")[2], "NOK");
    }

    @Test
    public void receiveAmountOKTest() {
        String accountPK2 = "ACCOUNTPK2";
        int writeTimestamp = "2";

        assertEquals(checkAccount(client2, accountPK2, writeTimestamp).split(";")[2], "ACK");
    }

    /*
       using the wrong public key
    */
    @Test
    public void receiveAmountNOKTest() {
        String wrongAccountPK2 = "WRONGACCOUNTPK2";
        String writeimestamp = "2";

        assertEquals(checkAccount(client2, wrongAccountPK2, writeimestamp).split(";")[2], "NOK");
    }

    @Test
    public void auditOKTest() {
        String accountPK1 = "ACCOUNTPK2";
        String readimestamp = "2";

        assertEquals(checkAccount(client1, accountPK1, readimestamp).split(";")[2], "ACK");
    }

    /*
          using the wrong timestamp
    */
    @Test
    public void auditNotOKTest() {
        String accountPK1 = "ACCOUNTPK2";
        String readimestamp = "1";

        assertEquals(checkAccount(client1, accountPK1, readimestamp).split(";")[2], "NOK");
    }

    @Test
    public void closeConnection() {
        assertEquals(closeConnection(id1).split(";")[4], "NOK");
    }
}