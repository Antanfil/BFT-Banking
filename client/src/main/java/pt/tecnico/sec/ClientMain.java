package pt.tecnico.sec;
 
 import java.io.BufferedReader;

 import pt.tecnico.sec.server.grpc.*;
 import pt.tecnico.sec.server.grpc.Server.*;
 import pt.tecnico.sec.server.ServerFrontend;

 import java.util.List;
 import java.util.StringTokenizer;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.io.IOException;
 
 public class ClientMain {
 	
 	public static void main(String[] args) throws NumberFormatException, InterruptedException{
 		System.out.println(ClientMain.class.getSimpleName());
 		
 		// receive and print arguments
 		System.out.printf("Received %d arguments%n", args.length);
 		for (int i = 0; i < args.length; i++) {
 			System.out.printf("arg[%d] = %s%n", i, args[i]);
 		}
 
 		final String host = args[0];
 		final int port = Integer.parseInt(args[1]);


 		ServerFrontend frontend = null ;
		frontend = new ServerFrontend( host , port );
 		Client client = new Client( frontend );


		int b,v;
		double c1 , c2;
		String s;
		StringTokenizer st;
		List<String> tokens = new ArrayList<>();
		String command;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

 		do{
 			try{
 				System.out.printf("> ");
 				st = new StringTokenizer(input.readLine());
 				while (st != null && st.hasMoreElements()) {
 					tokens.add(st.nextToken());
 				}
 				command = tokens.get(0);
 				switch (command) {
 					case "balance":
 						//b = client.askForBalance();


 					case "ping":
 						client.pingWorking();
 						break;

 					default:
 						break;
 				}
 				
 
 			}catch(IOException e){
 				System.out.println(e);
 			}
 		}while( true );
 	 
 	
 	}
 
 }

 