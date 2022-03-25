package pt.tecnico.sec;
 
 import java.io.*;

 import pt.tecnico.sec.server.grpc.*;
 import pt.tecnico.sec.server.grpc.Server.*;
 import pt.tecnico.sec.server.ServerFrontend;

 import java.security.*;
 import java.security.KeyStore.PasswordProtection;
 import java.security.KeyStore.PrivateKeyEntry;
 import java.security.cert.CertificateException;
 import java.security.cert.X509Certificate;
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
		int SSID;
		int SeqNo;
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
		System.out.println("Please insert your client id:");
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


		client.createKeyStore();

		System.out.println("Done !\n");
		System.out.println("Connecting to server ...\n");
		// TODO
		System.out.println("Connection to server complete !! We are ready to go. \n");

		do {
			try {
				System.out.println("Select an action to perform: \n" +
						"1.- Open an account\n" +
						"2.- Send amount (source account number & destination account number & amount needed)\n" +
						"3.- Check account (account number needed ) \n" +
						"4.- Receive amount (account number needed) \n" +
						"5.- Audit (account number needed)  \n" +
						"6.- Exit\n");
				System.out.printf("> ");
				tokens.clear();
				st = new StringTokenizer(input.readLine());
				while (st != null && st.hasMoreElements()) {
					tokens.add(st.nextToken());
				}
				command = tokens.get(0);
				System.out.println(command);
				switch (command) {
					case "ping":
						client.pingWorking();
						break;
					case "1":
						break;
					case "2":
						break;
					case "3":
						break;
					case "4":
						break;
					case "5":
						break;
					case "6":
						again = false;
						break;
					default:
						break;
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		} while (again );


		System.out.println(" Goodbye!! Se you soon. \n");


 	}
 
 }

 