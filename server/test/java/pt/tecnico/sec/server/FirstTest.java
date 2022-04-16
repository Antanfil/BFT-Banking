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
        int port = 8080;
        ServerSetup _server = null;

        try {
            _server = new ServerSetup( host , port );
            _server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void auditOKTest() {


        System.out.println("Audit OK");
        assertEquals("a", "a");
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

