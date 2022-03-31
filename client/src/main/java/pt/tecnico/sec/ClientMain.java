package pt.tecnico.sec;
 
 import java.io.*;

 import java.security.*;
 import java.security.cert.CertificateException;
 import java.util.List;
 import java.util.StringTokenizer;
 import java.util.ArrayList;

public class ClientMain {
 	
 	public static void main(String[] args) throws NumberFormatException, InterruptedException, NoSuchProviderException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableEntryException {
 		System.out.println(ClientMain.class.getSimpleName());

 		// receive and print arguments
 		System.out.printf("Received %d arguments%n ", args.length);
 		for (int i = 0; i < args.length; i++) {
 			System.out.printf("arg[%d] = %s%n", i, args[i]);
 		}
 
 		final String host = args[0];
 		final int port = Integer.parseInt(args[1]);
		int clientID = 0;
		boolean again = true;


 		ServerFrontend frontend = null ;
		frontend = new ServerFrontend( host , port );


		String command;
		StringTokenizer st;
		StringTokenizer st1;
		List<String> tokens = new ArrayList<>();
		List<String> tokens1 = new ArrayList<>();
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader input1 = new BufferedReader(new InputStreamReader(System.in));


		again = true;
		System.out.println("Please insert your client id and password:");
		try {
			System.out.printf("> ");
			tokens1.clear();
			st1 = new StringTokenizer(input1.readLine());
			while (st1 != null && st1.hasMoreElements()) {
				tokens1.add(st1.nextToken());
			}
			clientID = Integer.parseInt(tokens1.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Client client = new Client( frontend , clientID);

		System.out.println("Welcome to your Home banking Client No " + clientID + "\n");
		System.out.println("Generating Client's Public/Private Key Pair ... \n");
		client.loadKeyStore( tokens1.get(1) );
		System.out.println("Done !\n");
		System.out.println("Exchanging Keys with the Server ...\n");
		int value0 = client.exchangeKeys();
		if(value0 == -1 ){
			System.out.println(" Could not exchange Keys. Sorry \n");
			return ;
		}
		System.out.println("Done !\n");
		System.out.println("Connecting to server ...\n");
		int value = client.connect( 0 );
		if(value == -2 ){
			System.out.println(" Could not connect to server. Sorry \n");
			return ;
		}
		System.out.println("Connection to server complete !! We are ready to go. \n");

		do {
			try {
				System.out.println("Select an action to perform: \n" +
						"1.- Open an account ( account alias )\n" +
						"2.- Send amount (source account alias & destination account alias & amount needed)\n" +
						"3.- Check account (account alias needed ) \n" +
						"4.- Receive amount (account alias needed) \n" +
						"5.- Audit (account alias needed)  \n" +
						"6.- Exit\n");
				System.out.printf("> ");
				tokens.clear();
				st = new StringTokenizer(input.readLine());
				while (st != null && st.hasMoreElements()) {
					tokens.add(st.nextToken());
				}
				command = tokens.get(0);
				System.out.println(command);
				int status;
				String status1;
				switch (command) {
					case "ping":
						client.pingWorking( );
						break;
					case "1":
						status = client.openAccount( tokens.get(1) , 0 );
						if(status == -1)
							System.out.println("Something went wrong. Try Again");
						else if(status == -2)
							System.out.println("Couldn't reach the server. Try again later");
						else if(status == 0)
							System.out.println("Account opened successfully !!\n");
						else
							System.out.println("Unknown error");
						break;
					case "2":
						status = client.sendAmount( tokens.get(1) , tokens.get(2) , Integer.parseInt( tokens.get(3) ) , 0 );
						if(status == -1)
							System.out.println("Something went wrong. Try Again \n");
						else if(status == -2)
							System.out.println("Couldn't reach the server. Try again later");
						else if(status == 0)
							System.out.println("Money Sent !! Awaiting confirmation by recipient.\n");
						else if(status == 1)
							System.out.println("You don't have enough money in this account :( \n");
						else
							System.out.println("Unknown error");
						break;
					case "3":
						status1 = client.checkAccount( tokens.get(1) , 0 );
						if( status1.equals("-1") )
							System.out.println("Something went wrong. Try Again \n");
						else if(status1 == "-2")
							System.out.println("Couldn't reach the server. Try again later");
						else if( status1.length() > 7 ){
							System.out.println("You have pending money entries to accept !!\n");
							System.out.println(status1);
						}
						else if( status1.length() > 7 ) {
							System.out.println("Your balance is:");
							System.out.println(">" + status1 + "\n");
						}
						else
							System.out.println("Unknown error");
						break;
					case "4":
						status = client.receiveAmount( tokens.get(1) , 0);
						if(status == -1)
							System.out.println("Something went wrong. Try Again \n");
						else if(status == -2)
							System.out.println("Couldn't reach the server. Try again later");
						else if(status == 0)
							System.out.println("Money entered your account !! \n");
						else
							System.out.println("Unknown error");
						break;
					case "5":
						status1 = client.audit( tokens.get(1) , 0);
						if(status1.equals("-1"))
							System.out.println("Something went wrong. Try Again");
						else if(status1 == "-2")
							System.out.println("Couldn't reach the server. Try again later");
						else if(status1.length() > 7 ) {
							System.out.println("List of accounts transactions:");
							System.out.println(status1 + "\n");
						}
						else
							System.out.println("Unknown error");
						break;
					case "6":
						status = client.closeConnection( 0 );
						if(status == -1)
							System.out.println("Something went wrong. Try Again");
						else if(status == -2)
							System.out.println("Couldn't reach the server. Try again later");
						else if(status == 0){
							again = false;
							System.out.println("Connection Closing ...");
						}
						else
							System.out.println("Unknown error");
						break;
					default:
						break;
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		} while (again );


		System.out.println(" Goodbye!! Se you soon. \n");
		return ;

 	}
 
 }

 