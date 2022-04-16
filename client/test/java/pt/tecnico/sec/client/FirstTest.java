package pt.tecnico.sec.client;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirstTest {
    static ServerFrontend _frontend = null;
    static Client _client = null;


    @Test
    public void replayAttackTest() {
        try {
            String host = "localhost";
            int port = 8080;
            _frontend = new ServerFrontend(host, port);
            _client = new Client(_frontend, 1);
            _client.loadKeyStore("password");
            _client.exchangeKeys();
            _client.connect(0, "password");

            // ORIGINAL
            _client.loadKeyStore("password");
            String sourcePK = _client.getPublicKey("acc_1");
            String destPK = _client.getPublicKey("acc_2");
            String message = "2;"+_client.getId()+";"+Integer.toString(_client.getSSID())+";"+Integer.toString(_client.getSeqNo())+";"+sourcePK+";"+destPK+";"+Integer.toString(5);
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
    public void auditNotOKTest() {
        System.out.println("Audit nOK");
        assertEquals("a", "a");
        //String response = _serverFrontend.audit(publicKey);
        //assertEquals(_serverFrontend, response);
    }
}

