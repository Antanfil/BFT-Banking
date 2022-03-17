package pt.tecnico.sec.server;

import java.util.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.tecnico.sec.server.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;


public class ServerFrontend {

    ManagedChannel channel;
    ServerServiceGrpc.ServerServiceBlockingStub stub;


    public ServerFrontend(String host, int port) {

        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ServerServiceGrpc.newBlockingStub(channel);


    }

    public String Ping(String input) {
        try {
            PingRequest pingreq = PingRequest.newBuilder().setInput(input).build();
            PingResponse pingresp = PingResponse.newBuilder().build();

            pingresp = stub.ping(pingreq);
            return pingresp.getOutput();
        } catch (StatusRuntimeException e) {
            return "Caught error with description: " + e.getStatus().getDescription();
        }

    }

    public String Ping(ServerServiceGrpc.ServerServiceBlockingStub st) {
        try {
            PingRequest pingreq = PingRequest.newBuilder().setInput("1").build();
            PingResponse pingresp = PingResponse.newBuilder().build();

            pingresp = st.ping(pingreq);
            return pingresp.getOutput();
        } catch (StatusRuntimeException e) {
            return "Caught error with description: " + e.getStatus().getDescription();
        }

    }
/*
    public int Balance(String name) {
        try {
            BalanceRequest breq = BalanceRequest.newBuilder().setName(name).build();
            BalanceResponse bresp = BalanceResponse.newBuilder().build();
            bresp = stub.getbalance(breq);
            return bresp.getBalance();
        } catch (StatusRuntimeException e) {
            System.out.println("Caught error with description: " + e.getStatus().getDescription());
            return -1;
        }
    }
*/
}
