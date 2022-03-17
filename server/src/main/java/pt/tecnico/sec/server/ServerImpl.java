package pt.tecnico.sec.server;

import pt.tecnico.sec.server.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;
import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerImpl extends ServerServiceGrpc.ServerServiceImplBase {

    Server _server;

    public ServerImpl(Server server) {
        _server = server;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        String input = request.getInput();
        String output = "Hello " + input + "!";

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!").asRuntimeException());
        }

        PingResponse resp = PingResponse.newBuilder().setOutput(output).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }


   /* @Override
    public void getbalance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String name = request.getName();

        if (name == null || name.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Name cannot be empty!").asRuntimeException());
        }

        int balance = _server.get_REC_Balance(name);

        BalanceResponse resp = BalanceResponse.newBuilder().setBalance(balance).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
*/

}
