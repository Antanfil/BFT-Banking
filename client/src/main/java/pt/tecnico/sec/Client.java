package pt.tecnico.sec;

import java.util.*;

import pt.tecnico.sec.server.ServerFrontend;

public class Client {

    ServerFrontend _frontend;

    public Client( ServerFrontend frontend){
        _frontend = frontend;
    }

   /* public int askForBalance(){
        return _frontend.Balance(_nickName);
    }*/


    public boolean pingWorking( ){
        String s = _frontend.Ping(" App");
        System.out.println( s );
        return true;
    }

}
