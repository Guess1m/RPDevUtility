package com.Guess.ReportsPlusServer.util.network;

import com.Guess.ReportsPlusServer.util.log.LogUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.Guess.ReportsPlusServer.util.log.LogUtils.log;
import static com.Guess.ReportsPlusServer.util.network.FileUtils.*;

public class ServerUtils {
	public static boolean isConnected = false;
	public static String pubINET;
	public static String pubPORT;
	public static ServerSocket serverSocket;
	public static PrintWriter writer;
	public static Socket clientSocket;
	public static int BROADCAST_PORT;
	private static ServerStatusListener statusListener;
	private static ScheduledExecutorService broadcastExecutorService;
	
	public static void setStatusListener(ServerStatusListener listener) {
		statusListener = listener;
	}
	
	public static void startServer(int port, Label statusLabel) {
		try {
			log("Initializing server on port " + port, LogUtils.Severity.INFO);
			serverSocket = new ServerSocket(port);
			log("Server started successfully on port " + port, LogUtils.Severity.INFO);
			
			updateStatusLabel(statusLabel, "Server Started", "orange");
			
			pubPORT = String.valueOf(port);
			log("Waiting for a client connection on port " + port, LogUtils.Severity.INFO);
			clientSocket = serverSocket.accept();
			log("Client connected from " + clientSocket.getInetAddress().getHostAddress(), LogUtils.Severity.INFO);
			handleClient(statusLabel);
		} catch (BindException e) {
			log("Port " + port + " is already in use.", LogUtils.Severity.WARN);
			updateStatusLabel(statusLabel, "Port " + port + " is already in use.", "red");
		} catch (IOException e) {
			log("Error starting server: " + e.getMessage(), LogUtils.Severity.ERROR);
			log("Stack trace: " + Arrays.toString(e.getStackTrace()), LogUtils.Severity.ERROR);
			updateStatusLabel(statusLabel, "Error starting server.", "red");
		} finally {
			log("Server stopping.", LogUtils.Severity.INFO);
			stopServer();
			log("Server stopped.", LogUtils.Severity.INFO);
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
					log("Broadcasted server on port: " + port + " Using port: " + BROADCAST_PORT,
					    LogUtils.Severity.INFO);
				} catch (IOException e) {
					log("Error broadcasting server availability: " + e.getMessage(), LogUtils.Severity.ERROR);
				}
			}
		}, 0, 5, TimeUnit.SECONDS);
	}
	
	public static void stopBroadcasting() {
		if (broadcastExecutorService != null && !broadcastExecutorService.isShutdown()) {
			broadcastExecutorService.shutdown();
			log("Broadcasting stopped", LogUtils.Severity.INFO);
		}
	}
	
	public static void disconnectClient() {
		if (clientSocket != null && !clientSocket.isClosed()) {
			try {
				clientSocket.close();
				log("Client socket closed", LogUtils.Severity.INFO);
			} catch (IOException e) {
				log("Error closing client socket: " + e.getMessage(), LogUtils.Severity.ERROR);
			}
		}
	}
	
	public static void sendShutdownMessage() {
		if (writer != null) {
			writer.println("SHUTDOWN");
			log("Shutdown message sent to client", LogUtils.Severity.INFO);
		}
	}
	
	public static void stopServer() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				sendShutdownMessage();
				serverSocket.close();
				log("Closed ServerSocket", LogUtils.Severity.INFO);
				disconnectClient();
				log("Disconnected Client", LogUtils.Severity.INFO);
				stopBroadcasting();
			}
		} catch (IOException e) {
			log("Error closing server socket: " + e.getMessage(), LogUtils.Severity.ERROR);
		}
	}
	
	private static void handleClient(Label statusLabel) {
		try {
			log("Client connected from: " + clientSocket.getInetAddress(), LogUtils.Severity.INFO);
			isConnected = true;
			notifyStatusChanged(isConnected);
			pubINET = String.valueOf(clientSocket.getInetAddress());
			updateStatusLabel(statusLabel, "Client Connected", "green");
			log("Watching for file changes", LogUtils.Severity.DEBUG);
			
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
					log("Error sending heartbeat: " + e.getMessage(), LogUtils.Severity.ERROR);
				}
			}, 0, 5, TimeUnit.SECONDS);
			
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				if (inputLine.equals("HEARTBEAT")) {
					log("Received heartbeat from client", LogUtils.Severity.DEBUG);
					writer.println("HEARTBEAT");
				}
			}
		} catch (SocketException e) {
			log("Client disconnected", LogUtils.Severity.WARN);
			updateStatusLabel(statusLabel, "Client Disconnected", "#ff5e5e");
			isConnected = false;
			notifyStatusChanged(isConnected);
		} catch (IOException e) {
			log("Error with client connection: " + e.getMessage(), LogUtils.Severity.ERROR);
		} finally {
			disconnectClient();
		}
	}
	
	public static void runStartServerRefresh(Label statusLabel, int port) {
		if (!isConnected) {
			new Thread(() -> startServer(port, statusLabel)).start();
		} else {
			log("Already connected", LogUtils.Severity.WARN);
		}
	}
	
	private static void notifyStatusChanged(boolean isConnected) {
		if (statusListener != null) {
			log("Server Connection Status Changed: " + isConnected, LogUtils.Severity.DEBUG);
			Platform.runLater(() -> statusListener.onStatusChanged(isConnected));
		}
	}
	
	private static void updateStatusLabel(Label statusLabel, String text, String color) {
		Platform.runLater(() -> {
			statusLabel.setText(text);
			statusLabel.setStyle("-fx-background-color: " + color + ";");
		});
	}
	
	private static void startFileWatchers() {
		String dataPath = FileUtils.getJarPath() + File.separator + "data";
		watchForFileChanges(dataPath, "callout.xml", "UPDATE_CALLOUT");
		watchForFileChanges(dataPath, "location.data", "UPDATE_LOCATION");
		watchForFileChanges(dataPath, "worldPeds.data", "UPDATE_WORLD_PED");
		watchForFileChanges(dataPath, "worldCars.data", "UPDATE_WORLD_VEH");
		watchForFileChanges(dataPath, "trafficStop.data", "UPDATE_TRAFFIC_STOP");
		FileUtils.watchForIDChanges(FileUtils.getJarPath() + File.separator + "data", "currentID.xml");
	}
	
	private static void handleClientDisconnection(ScheduledExecutorService executor) {
		log("Client Heartbeat Lost", LogUtils.Severity.WARN);
		isConnected = false;
		notifyStatusChanged(isConnected);
		executor.shutdown();
		log("Client Heartbeat check stopped", LogUtils.Severity.INFO);
	}
}

