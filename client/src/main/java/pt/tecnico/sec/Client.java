package pt.tecnico.sec;

import java.security.KeyStore;
import java.util.*;

import pt.tecnico.sec.server.ServerFrontend;

public class Client {

    ServerFrontend _frontend;
    private int id;
    private List<Account> accounts ;

    String pbkAlias;
    String prkAlias;

    public Client( ServerFrontend frontend){
        _frontend = frontend;
    }

   /* public int askForBalance(){
        return _frontend.Balance(_nickName);
    }*/


    public boolean pingWorking( ){
        String input = "App";

        String s = _frontend.Ping( input );
        System.out.println( s );
        return true;
    }

}
