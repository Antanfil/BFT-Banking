package pt.tecnico.sec.client;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirstTest {
    static ServerFrontend _frontend = null;
    static Client _client = null;

    @BeforeAll
    public static void oneTimeSetUp() {
        String host = "localhost";
        int port = 8080;
        _frontend = new ServerFrontend(host, port , 5);
        _client = new Client(_frontend, 1);
        _client.loadKeyStore("password");
        _client.exchangeKeys();
        _client.connect(0, "password");

        _client.loadKeyStore("password");
        String sourcePK = _client.getPublicKey("acc_1");
        String destPK = _client.getPublicKey("acc_2");

    }


        @Test
    public void replayAttackTest() {
        try {
            String host = "localhost";
            int port = 8080;
            _frontend = new ServerFrontend(host, port , 5);
            _client = new Client(_frontend, 1);
            _client.loadKeyStore("password");
            _client.exchangeKeys();
            _client.connect(0, "password");

            // ORIGINAL
            String message = "2;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+destPK+";"+Integer.toString(5)";"+Integer.toString(1);
            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");
            String messageResponse = _frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo());



            // REPLAY ATTACK
            _frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo());

            String balance = _client.checkAccount("acc_1",0,"password");
            System.out.println(balance);

            if(balance == "5")
                System.out.println("Success Replay attack had no effect on the system!");
            assertEquals("5", balance);


        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
        //String response = _serverFrontend.audit(publicKey);
        //assertEquals(_serverFrontend, response);
    }

    @Test
    public void openAccountOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "1;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+accountPK+";"+Integer.toString(2);
            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "200");


        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    /*
    using the wrong public key
     */
    @Test
    public void openAccountNOKTest() {
        try {
            String accountPK= "PUBLICKEY";
            String message = "1;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+accountPK+";"+Integer.toString(3);
            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "200");


        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    @Test
    public void sendAmountOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "2;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+destPK+";"+Integer.toString(5)";"+Integer.toString(4);
            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "200");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }
    /*
    wrong signature, using the wrong password
    */
    @Test
    public void sendAmountNOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "2;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+destPK+";"+Integer.toString(5)";"+Integer.toString(5);
            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "wrongPassword");

            !assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "200");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    @Test
    public void checkAccountOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "3;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+Integer.toString(1);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "50"); //avaiable balance

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    /*
    using the wrong timestamp
     */
    @Test
    public void checkAccountNOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "3;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+Integer.toString(0);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "-1");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    @Test
    public void receiveAmountOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "4;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+Integer.toString(5);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "0");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    /*
    using the wrong timestamp
     */
    @Test
    public void receiveAmountNOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "4;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+Integer.toString(3);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "-1");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    @Test
    public void auditOKTest() {
        try {
            String accountPK= "PUBLICKEY1";
            String message = "5;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+Integer.toString(2);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            !assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "-1");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }

    /*
    using the wrong public key
     */
    @Test
    public void auditOKTest() {
        try {
            String wrongSourcePK= "WRONGPUBLICKEY1";
            String message = "5;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+wrongSourcePK+";"+Integer.toString(2);

            byte[] signature = null;
            signature = _client.getSignature("acc_1" , message, "password");

            assertEquals(_frontend.send(message, signature , _client.getServerPK(), _client.getSSID() , _client.getSeqNo()), "-1");

        }
        catch (Exception e){
            System.out.println("oops");
            assertEquals("25", "25");
        }
    }
}
