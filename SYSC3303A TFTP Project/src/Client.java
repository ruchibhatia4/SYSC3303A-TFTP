import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

/**
 * @author Group 5
 * @version 5/21/2018 (Iteration #1)
 * 
 * Client-side Algorithm
 */
public class Client extends Thread implements Runnable{	
	private DatagramPacket sendPacket, receivePacket;		// Packet data is assigned when sending/receiving from server
	private DatagramSocket sendReceiveSocket;				// Dual receive and send packets to/from server 
	private Print printable;								// General packet information is printed to console if verbose is enabled
	private static Constants.ModeType mode;					// Verbose or quiet console output mode
	private static boolean requestedThreadClosing;			// Request closing of client thread
	private byte msg[] = new byte[512];						// Data packet
	private static String filename = "";					// File name to be read or written
	private static String typeOfRequest = "";				// Enum string of RRQ or QRQ
	private static Scanner in = new Scanner(System.in);		// User input scanner
	private static String userInput;						// User response for verbose or quiet
	private static String userPreference;					// User response for normal or test mode
	private static int userPortPrefence;					// Normal mode or Test Mode (PORT 69 or 23)
	private static String userServerIP;						// If direct mode is enabled send it to this address else, pass it to host
	private static InetAddress serverAddress;				// Convert user response to InetAddress type and assign it to this variable
	private static String userClientDirectory;				// Read/Write file into/from this directory
	private static String optionSelected = "";				// Request type (RRQ or WRQ)

	/**
	 * Client constructor
	 */
	public Client() {
		this.printable = new Print(mode);

		try {
			sendReceiveSocket = new DatagramSocket(42);
		} catch (SocketException e){
			print("[CLIENT] ERROR OCCURED: " + e.getMessage());
		}
	}

	/**
	 * 
	 */
	public void run() {
		try {
			sendReceivePackets();
		} catch (Exception e) {
			print("[Client] Thread Error Occured: " + e.getMessage());
			System.out.print("[Client] Stack trace information --> ");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void sendReceivePackets() throws Exception {
		String mode = "";
		receivePacket = new DatagramPacket(new byte[512], 512);
		sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), userPortPrefence);

		for(;;) {
			if (requestedThreadClosing == true) {
				msg = new String("CloseServerThreads").getBytes();
				sendPacket.setData(msg);
				printable.PrintSendingPackets(Constants.ServerType.CLIENT, Constants.ServerType.HOST, sendPacket.getAddress(),
						sendPacket.getPort(), sendPacket.getLength(), sendPacket.getData());
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					print("Client: Error occured while sending packet ==> Stack Trace "  + e.getStackTrace().toString());
					System.exit(1);
				}
				Thread.currentThread().interrupt();
				break;
			} else {
				// Read (RRQ) or Write (WRQ) requests write format into packet data
				byte data[] = filename.getBytes();
				for(int i = 0; i < data.length; i++) {
					msg[0] = Constants.PacketByte.ZERO.getPacketByteType();
					if (typeOfRequest.equals("RRQ")) {
						msg[1] = Constants.PacketByte.RRQ.getPacketByteType();
					} else if (typeOfRequest.equals("WRQ")) {
						msg[1] = Constants.PacketByte.WRQ.getPacketByteType();
					} else {
						msg = new String("CloseServerThreads").getBytes();
						sendPacket.setData(msg);
						printable.PrintSendingPackets(Constants.ServerType.CLIENT, Constants.ServerType.HOST, sendPacket.getAddress(),
								sendPacket.getPort(), sendPacket.getLength(), sendPacket.getData());
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							print("Client: Error occured while sending packet ==> Stack Trace "  + e.getStackTrace().toString());
							System.exit(1);
						}
						Thread.currentThread().interrupt();
						break;
					}

					System.arraycopy(data,0,msg,2,data.length);
					msg[filename.getBytes().length + 2] = Constants.PacketByte.ZERO.getPacketByteType();

					// Convert mode from String to Byte[] and add it to message, then add 0 byte.
					mode = "netascii";
					byte[] modeArray = mode.getBytes();
					System.arraycopy(modeArray, 0, msg, filename.getBytes().length + 3, modeArray.length);
					int sizeOfMsg = filename.getBytes().length + modeArray.length + 4;
					msg[sizeOfMsg - 1] = Constants.PacketByte.ZERO.getPacketByteType();
					if (userPortPrefence == Constants.ClientPacketSendType.NORMAL.getPortID()) {
						sendPacket = new DatagramPacket(msg, sizeOfMsg, serverAddress, userPortPrefence);
					} else {
						sendPacket = new DatagramPacket(msg, sizeOfMsg, InetAddress.getLocalHost(), userPortPrefence);
					}	
				}

				// Add actual packet send or build logic
				if (typeOfRequest.equals("RRQ")) {
					print("[Client]: Reading file \n");
					// Copy file contents to Client directory + filename <-- Receive from server
				} else {
					print("[Client]: Writing file \n");
					// Read file from Client directory + filename --> send to server
				}

				// Send the packet to ErrorSimulator or Main Server depending on mode
				if (userPortPrefence == Constants.ClientPacketSendType.NORMAL.getPortID()) {
					printable.PrintSendingPackets(Constants.ServerType.CLIENT, Constants.ServerType.MAIN_SERVER, sendPacket.getAddress(),
							sendPacket.getPort(), sendPacket.getLength(), sendPacket.getData());
				} else {
					printable.PrintSendingPackets(Constants.ServerType.CLIENT, Constants.ServerType.HOST, sendPacket.getAddress(),
							sendPacket.getPort(), sendPacket.getLength(), sendPacket.getData());
				}
			
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				print("Client: Packet sent to ErrorSimulator.\n");

				// Receive a packet from server
				print("Client: Waiting for packets to arrive.\n");

				try {
					sendReceiveSocket.receive(receivePacket);
				} catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				printable.PrintReceivedPackets(Constants.ServerType.CLIENT, Constants.ServerType.SECONDARY_SERVER, receivePacket.getAddress(),
						receivePacket.getPort(), receivePacket.getLength(), receivePacket.getData());
			}
		}
		print("Client: Closing old sockets ...");
		sendReceiveSocket.close();
		print("[Done] Client: Closed old sockets");
		System.exit(0);
	}

	/**
	 * 
	 * @throws IOException
	 */
	private static void getRequestTypeFromUser() throws IOException {		
		do {
			System.out.println("Enter (R) for read request, (W) for write request, or (E) for program termination: ");
			optionSelected = in.nextLine().toUpperCase();
			System.out.println("[Client] Requested inputted: " + optionSelected + "\n");
		} while (!(optionSelected.equals("R") || optionSelected.equals("W") || optionSelected.equals("E")));

		if (optionSelected.equals("R")) {
			typeOfRequest = Constants.PacketString.RRQ.getPacketStringType();
		} 

		if (optionSelected.toUpperCase().equals("W")) {
			typeOfRequest = Constants.PacketString.WRQ.getPacketStringType();
		} 

		if (optionSelected.toUpperCase().equals("E")) {
			System.out.println("Client: Shutdown requested to server");
			requestedThreadClosing = true;
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	private static void getFileNameFromUser() throws IOException{
		System.out.println("Enter [1] Default File name on Server (i.e. test.txt) [2] Custom File name on Server: \n");
		int fileDefault = in.nextInt();

		if (fileDefault == 2) {
			do {				
				System.out.println("Enter file name: \n");
				filename = in.nextLine();
			} while (filename.length() == 0);
		} else {
			filename = "test.txt";
		}

		if (filename.toUpperCase().equals("E")) {
			System.out.println("Client: Shutdown requested to server");
			requestedThreadClosing = true;
		} else {
			System.out.println("\n [Client] Filename entered: " + filename);
		}
	}

	/**
	 * 
	 * @param printable
	 */
	private void print(String printable) {
		if (mode == Constants.ModeType.VERBOSE) {
			System.out.println(printable);
		}
	}

	/**
	 * 
	 * @return 
	 */
	public static synchronized InetAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * 
	 * @param serverAddress
	 */
	public static synchronized void setServerAddress(InetAddress serverAddress) {
		Client.serverAddress = serverAddress;
	}

	public static void main(String[] args) throws IOException {
		/**
		 * Note 1: Client user interface should be able handle the following tasks:
		 * 
		 * 1) The file transfer operation (read file from server, write file to server)
		 * 2) The name of the file that is to be transferred 
		 * 
		 *------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------
		 * 
		 * Note 2: Client UI needs the following information from the user:
		 * 
		 * 1) Normal or Test Mode
		 * 2) Verbose or Quiet Mode
		 * 3) IP Address of Server
		 * 4) Client Directory
		 * 5) Packet information printed to console if verbose is enabled for all clients,
		 * 	  host (error simulator), and server
		 * 
		 */

		// 1) Normal or Test Mode
		do {
			System.out.println("Send packets directly to Server or send to ErrorSimulator first ([Y]es {Connect Directly to Server} / [N]o {ErrorSimulator First}) ?");
			userPreference = in.nextLine().toUpperCase();
		} while (!(userPreference.equals("Y") || userPreference.equals("N")));

		if (userPreference.equals("Y")) {
			userPortPrefence = Constants.ClientPacketSendType.NORMAL.getPortID();
		} 

		if (userPreference.equals("N")) {
			userPortPrefence = Constants.ClientPacketSendType.TEST.getPortID();
		}

		// 2) Verbose or Quiet Mode
		do {
			System.out.println("Enter console output mode (VERBOSE or QUIET): ");
			userInput = in.nextLine().toUpperCase();
		} while (!(userInput.equals("VERBOSE") || userInput.equals("QUIET")));

		if (userInput.equals("VERBOSE")) {
			mode = Constants.ModeType.VERBOSE;
		} 

		if (userInput.equals("QUIET")) {
			mode = Constants.ModeType.QUIET;
		}

		// 3) IP Address of Server
		do {
			System.out.println("Enter server IP Address: ");
			userServerIP = in.nextLine();
		} while (userServerIP .length() == 0);

		try {
			setServerAddress(InetAddress.getByName(userServerIP));
		} catch (Exception e) {
			System.out.println("[Client] Server IP address format error occured: " + e.getMessage());
		}

		// 4) Client Directory
		do {
			System.out.println("Enter client directory: ");
			userClientDirectory = in.nextLine();
		} while (userClientDirectory.length() == 0);

		// 5) Read or Write request
		try {
			getRequestTypeFromUser();
		} catch (IOException e) {
			System.out.println("[Client] Request type error occured: " + e.getMessage());
		}

		// 6) File name that will be written to server or read from server
		try {
			getFileNameFromUser();
		} catch (Exception e) {
			System.out.println("[Client] File type error occured: " + e.getMessage());
		}

		// 7) Start a new client thread instance
		in.close();
		try {
			Client client = new Client();
			client.start();
			System.out.println("[Client] ~ Started Client \n");
		} catch (Exception e) {
			System.out.println("[Client] ~ Error occured while starting host: " + e.getMessage());
		}
	}
}
