package pt.tecnico.sec.client;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.Server;

import java.util.List;

public class MessageObserver<R> implements StreamObserver<R> {
    ResponseColector colector = new ResponseColector();

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
    public List<Server.MessageResponse> updatedValue(){
        return colector.getUpdatedResponse();
    }

}
