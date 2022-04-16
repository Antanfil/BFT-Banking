package pt.tecnico.sec.client;

import pt.tecnico.sec.server.grpc.Server;

import java.util.ArrayList;
import java.util.List;

public class ResponseColector {
    List<Server.MessageResponse> responses = new ArrayList<Server.MessageResponse>();
    ResponseColector(){}

    public int cont=0;

    public void clean(){
        responses.clear();
        cont=0;
    }

    synchronized void increment(){
        cont=cont+1;
    }
    public int getCont(){
        return cont;
    }

    synchronized void putResponse( Server.MessageResponse resp){
        responses.add(resp);
    }

    //returna o response de um read que tem a tag mais atualizada
    //i.e. o valor mais recente de todos os recebidos
    synchronized List<Server.MessageResponse> getUpdatedResponse( ){
        Server.MessageResponse res = Server.MessageResponse.newBuilder().build();
        res = responses.get(0);
        return responses;
    }

}
