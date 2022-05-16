package pt.tecnico.sec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerMain {

	public static void main(String[] args) throws IOException {
		System.out.println(ServerMain.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String Hhost = args[0];
		final int Hport = Integer.parseInt(args[1]);
		final String serverName = args[2];
		final int replicas = Integer.parseInt(args[3]);

		
		// Start the server
		ServerSetup serverSetup = new ServerSetup(Hhost,Hport,serverName , replicas);

		serverSetup.start();
		// Do not exit the main thread. Wait until server is terminated.
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));

		// Reading data using readLine
		reader.readLine();
		serverSetup.broadcast();

		serverSetup.awaitTermination();

	}
}
