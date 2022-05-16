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


    ServerSetup(String host,int port, String serverName , int replicas) throws IOException{
		System.out.println(serverName);
		String text = serverName.split("\\.")[0];
		text = text.concat(".txt");
        File file = new File(text);

		if( file.createNewFile() ){
			server = new Server(serverName , port , host , 8080, replicas);
			server.saveState();
		}
		else{
			try(FileInputStream fis = new FileInputStream(file);
					ObjectInputStream ois = new ObjectInputStream(fis)){

				server = (Server) ois.readObject();
				server.cleanBroadcastList();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				server = new Server(serverName, port , host , 8080, replicas);
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

	public void broadcast(){
		server.propagatePK();
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
