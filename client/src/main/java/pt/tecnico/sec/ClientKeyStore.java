package pt.tecnico.sec;

import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientKeyStore {


    KeyPair clientKeyPair ;

    Map< String , KeyPair > accounts ;

    public ClientKeyStore( ) throws NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        keyGen.initialize(1024 );
        clientKeyPair = keyGen.generateKeyPair();

    }

    public PublicKey getPublicKey() {
        return clientKeyPair.getPublic();
    }

    public void setKeyEntry(String alias , KeyPair pair ){


        accounts.put(alias , pair);

    }

    public PublicKey getKeyEntry(String alias){
        return accounts.get(alias).getPublic() ;
    }


}
