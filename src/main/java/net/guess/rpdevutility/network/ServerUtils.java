package net.guess.rpdevutility.network;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.guess.rpdevutility.network.FileUtils.watchForFileChanges;

public class ServerUtils {
	public static boolean isConnected = false;
	public static int BROADCAST_PORT = 8888;
	public static String pubINET;
	public static String pubPORT;
	public static ServerSocket serverSocket;
	public static PrintWriter writer;
	public static Socket clientSocket;
	private static ScheduledExecutorService broadcastExecutorService;
	
	public static void startServer(int port) {
		try {
			System.out.println("Initializing server on port " + port);
			serverSocket = new ServerSocket(port);
			System.out.println("Server started successfully on port " + port);
			
			pubPORT = String.valueOf(port);
			System.out.println("Waiting for a client connection on port " + port);
			clientSocket = serverSocket.accept();
			System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
			handleClient();
		} catch (BindException e) {
			System.out.println("Port " + port + " is already in use.");
		} catch (IOException e) {
			System.out.println("Error starting server: " + e.getMessage());
			System.out.println("Stack trace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			System.out.println("Server stopping.");
			stopServer();
			System.out.println("Server stopped.");
		}
	}
	
	public static void startBroadcasting(int port) {
		broadcastExecutorService = Executors.newSingleThreadScheduledExecutor();
		broadcastExecutorService.scheduleAtFixedRate(() -> {
			if (!isConnected) {
				try (DatagramSocket socket = new DatagramSocket()) {
					socket.setBroadcast(true);
					String message = "SERVER_DISCOVERY:" + port;
					byte[] buffer = message.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					                                           InetAddress.getByName("255.255.255.255"),
					                                           BROADCAST_PORT);
					socket.send(packet);
					System.out.println("Broadcasted server on port: " + port + " Using port: " + BROADCAST_PORT);
				} catch (IOException e) {
					System.out.println("Error broadcasting server availability: " + e.getMessage());
				}
			}
		}, 0, 5, TimeUnit.SECONDS);
	}
	
	public static void stopBroadcasting() {
		if (broadcastExecutorService != null && !broadcastExecutorService.isShutdown()) {
			broadcastExecutorService.shutdown();
			System.out.println("Broadcasting stopped");
		}
	}
	
	public static void disconnectClient() {
		if (clientSocket != null && !clientSocket.isClosed()) {
			try {
				clientSocket.close();
				System.out.println("Client socket closed");
			} catch (IOException e) {
				System.out.println("Error closing client socket: " + e.getMessage());
			}
		}
	}
	
	public static void sendShutdownMessage() {
		if (writer != null) {
			writer.println("SHUTDOWN");
			System.out.println("Shutdown message sent to client");
		}
	}
	
	public static void stopServer() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				sendShutdownMessage();
				serverSocket.close();
				System.out.println("Closed ServerSocket");
				disconnectClient();
				System.out.println("Disconnected Client");
				stopBroadcasting();
			}
		} catch (IOException e) {
			System.out.println("Error closing server socket: " + e.getMessage());
		}
	}
	
	private static void handleClient() {
		try {
			System.out.println("Client connected from: " + clientSocket.getInetAddress());
			isConnected = true;
			pubINET = String.valueOf(clientSocket.getInetAddress());
			System.out.println("Watching for file changes");
			
			startFileWatchers();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			writer = new PrintWriter(clientSocket.getOutputStream(), true);
			
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			executor.scheduleAtFixedRate(() -> {
				try {
					if (!clientSocket.isClosed()) {
						writer.println("HEARTBEAT");
					} else {
						handleClientDisconnection(executor);
					}
				} catch (Exception e) {
					System.out.println("Error sending heartbeat: " + e.getMessage());
				}
			}, 0, 5, TimeUnit.SECONDS);
			
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				if (inputLine.equals("HEARTBEAT")) {
					System.out.println("Received heartbeat from client");
				}
			}
		} catch (SocketException e) {
			System.out.println("Client disconnected");
			isConnected = false;
		} catch (IOException e) {
			System.out.println("Error with client connection: " + e.getMessage());
		} finally {
			disconnectClient();
		}
	}
	
	public static void runStartServerRefresh(int port) {
		if (!isConnected) {
			new Thread(() -> startServer(port)).start();
		} else {
			System.out.println("Already connected");
		}
	}
	
	private static void startFileWatchers() {
		String dataPath = FileUtils.getJarPath() + File.separator + "data";
		watchForFileChanges(dataPath, "callout.xml", "UPDATE_CALLOUT");
		watchForFileChanges(dataPath, "gameData.data", "UPDATE_LOCATION");
		watchForFileChanges(dataPath, "worldPeds.data", "UPDATE_WORLD_PED");
		watchForFileChanges(dataPath, "worldCars.data", "UPDATE_WORLD_VEH");
		watchForFileChanges(dataPath, "trafficStop.data", "UPDATE_TRAFFIC_STOP");
		FileUtils.watchForIDChanges(FileUtils.getJarPath() + File.separator + "data", "currentID.xml");
	}
	
	private static void handleClientDisconnection(ScheduledExecutorService executor) {
		System.out.println("Client Heartbeat Lost");
		isConnected = false;
		executor.shutdown();
		System.out.println("Client Heartbeat check stopped");
	}
}
