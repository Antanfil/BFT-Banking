package pt.tecnico.sec.client;

import pt.tecnico.sec.server.grpc.Server;

import java.util.ArrayList;
import java.util.List;

public class ResponseColector {

    int ack = 0;
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
        if( resp.getMessage().split(";").length > 2 ){
            if(resp.getMessage().split(";")[2]!="o" || resp.getMessage().split(";")[2]!="c"){
                String ack = resp.getMessage().split(";")[4];
                if (ack.equals("ACK") ){
                    this.ack++;
                    responses.add(resp);
                }
            }
        }
        else{
            responses.add(resp);
        }
    }

    //returna o response de um read que tem a tag mais atualizada
    //i.e. o valor mais recente de todos os recebidos
    synchronized Server.MessageResponse getUpdatedResponse( ){
        Server.MessageResponse res = Server.MessageResponse.newBuilder().build();
        res = responses.get(0);
        return res;
    }
    synchronized List<Server.MessageResponse> getResponses( ){

        return responses;
    }

    public int getAck() {
       /* for (int i = 0; i < responses.size(); i++) {
            String ack = responses.get(i).getMessage().split(";")[4];
            if (ack.equals("ACK")) this.ack++;
        }*/
        return this.ack;
    }

    public void deleteFromColector(int serverId) {
        for (int i = 0; i < responses.size(); i++) {
            if (Integer.parseInt(responses.get(i).getMessage().split(";")[5]) == serverId)
                this.responses.remove(i);
        }
    }

}
