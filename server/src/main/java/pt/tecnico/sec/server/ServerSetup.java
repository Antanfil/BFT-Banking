package pt.tecnico.sec.server;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ServerSetup {

	private io.grpc.Server serverServer;
	private Server server;


    ServerSetup(String host,int port) throws IOException{
        File file = new File("server.txt" );

		if( file.createNewFile() ){
			server = new Server();
			server.saveState();
		}
		else{
			try(FileInputStream fis = new FileInputStream(file);
					ObjectInputStream ois = new ObjectInputStream(fis)){

				server = (Server) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				server = new Server();
			}
		}


		final BindableService impl = new ServerImpl(server);


		
		// Create a new server to listen on port
		serverServer = ServerBuilder.forPort(port).addService(impl).build();
		
		// Start the server
		
    }

	public void start() throws IOException {
		serverServer.start();
		System.out.println("Server started, listening on " + serverServer.getPort());
	}
	
	public void awaitTermination(){
		// Do not exit the main thread. Wait until server is terminated.
		try {
			serverServer.awaitTermination();
			serverServer.shutdown();
		} catch (InterruptedException e) {
			System.out.println("Shutting Down");
			server.shutDown();
			e.printStackTrace();
		}
	}

}
