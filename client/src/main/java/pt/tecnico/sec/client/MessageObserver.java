package pt.tecnico.sec.client;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.Server;
import pt.tecnico.sec.client.ResponseColector;

import java.util.List;

public class MessageObserver<R> implements StreamObserver<R> {
    ResponseColector colector = new ResponseColector();
    int ack = 0;

    @Override
    public synchronized void onNext(R r) {
        this.notifyAll();
        if( r instanceof Server.MessageResponse){
            Server.MessageResponse re = (Server.MessageResponse) r;
            colector.putResponse( re );
            colector.increment();
        }
        else {
            System.out.println("Error");
        }

    }


    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
        //System.out.println("Request completed");
    }

    public void clean(){
        colector.clean();
    }
    public int getTotalResponses(){
        return colector.getCont();
    }
    public int getACK(){
        return colector.getCont();
    }


    public Server.MessageResponse getUpdatedValue(){
        return colector.getUpdatedResponse();
    }

    public List<Server.MessageResponse> getAllValues(){
        return colector.getResponses();
    }

    public void deleteFromColector(int serverId) {
        this.colector.deleteFromColector(serverId);
    }

    public int getAck() {
        return this.colector.getAck();
    }

}
