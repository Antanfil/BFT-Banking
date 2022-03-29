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
    @Override
    public void send(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        String clientPubKey = request.getMessage();

        _server.receiveKey(clientPubKey);
        MessageResponse resp= MessageResponse.newBuilder().setMessage(_server.getKey()).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();

    }

}
