package pt.tecnico.sec.server;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import java.io.IOException;


import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;


public class ServerMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println(ServerMain.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String Hhost = args[0];
		final int Hport = Integer.parseInt(args[1]);
		
		final Server server = new Server();
		final BindableService impl = (BindableService) new ServerImpl(server);


		
		// Create a new server to listen on port
		io.grpc.Server serverServer = ServerBuilder.forPort(Hport).addService(impl).build();
		
		// Start the server
		
		serverServer.start();
		
		// Server threads are running in the background.
		System.out.println("Server started");
		
		// Do not exit the main thread. Wait until server is terminated.
		serverServer.awaitTermination();

		
	}
	
}
