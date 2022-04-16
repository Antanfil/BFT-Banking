package pt.tecnico.sec.server;

import java.io.IOException;
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

		
		// Start the server
		ServerSetup serverSetup = new ServerSetup(Hhost,Hport);

		serverSetup.start();
		// Do not exit the main thread. Wait until server is terminated.

		serverSetup.awaitTermination();

	}
}
