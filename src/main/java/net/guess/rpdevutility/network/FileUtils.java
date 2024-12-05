package net.guess.rpdevutility.network;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;

public class FileUtils {
	
	static long idLastUpdateTimestamp = 0;
	static boolean idUpdateBufferActive = false;
	
	public static synchronized void sendFileToClient(String fileToSend) throws IOException {
		try (Socket sock = ServerUtils.serverSocket.accept(); FileInputStream fis = new FileInputStream(
				fileToSend); BufferedInputStream bis = new BufferedInputStream(
				fis); OutputStream os = sock.getOutputStream()) {
			
			byte[] mybytearray = new byte[(int) new File(fileToSend).length()];
			bis.read(mybytearray, 0, mybytearray.length);
			System.out.println("Sending " + fileToSend + "(" + mybytearray.length + " bytes)");
			os.write(mybytearray, 0, mybytearray.length);
			os.flush();
			System.out.println("Sent.");
		}
	}
	
	public static void watchForIDChanges(String directoryPath, String fileNameToWatch) {
		Path dir = Paths.get(directoryPath);
		
		Thread watchThread = new Thread(() -> {
			try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
				dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
				
				while (true) {
					WatchKey key;
					try {
						key = watcher.take();
					} catch (InterruptedException x) {
						Thread.currentThread().interrupt();
						return;
					}
					
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						
						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}
						
						WatchEvent<Path> ev = (WatchEvent<Path>) event;
						Path fileName = ev.context();
						if (fileName.toString().equals(fileNameToWatch)) {
							Path filePath = dir.resolve(fileName);
							try {
								if (Files.size(filePath) > 0) {
									long currentTime = System.currentTimeMillis();
									if (!idUpdateBufferActive || (currentTime - idLastUpdateTimestamp >= 1000)) {
										System.out.println(
												fileName + " has been " + (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modified" : "created"));
										
										ServerUtils.writer.println("UPDATE_ID");
										System.out.println("sent update ID message");
										sendFileToClient(
												getJarPath() + File.separator + "data" + File.separator + fileNameToWatch);
										idLastUpdateTimestamp = currentTime;
										idUpdateBufferActive = true;
										new Timer().schedule(new TimerTask() {
											@Override
											public void run() {
												idUpdateBufferActive = false;
											}
										}, 1000);
									}
								}
							} catch (IOException e) {
								System.err.println(e);
							}
						}
					}
					
					boolean valid = key.reset();
					if (!valid) {
						break;
					}
				}
			} catch (IOException e) {
				System.err.println("Error setting up file watch: " + e);
			}
		});
		watchThread.setDaemon(true);
		watchThread.start();
	}
	
	public static void watchForFileChanges(String directoryPath, String fileNameToWatch, String updateCommand) {
		Path dir = Paths.get(directoryPath);
		Thread watchThread = new Thread(() -> {
			try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
				dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
				long lastUpdateTime = 0;
				final boolean[] bufferActive = {false};
				
				while (true) {
					WatchKey key;
					try {
						key = watcher.take();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						
						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}
						
						Path fileName = ((WatchEvent<Path>) event).context();
						if (fileName.toString().equals(fileNameToWatch)) {
							long currentTime = System.currentTimeMillis();
							if (!bufferActive[0] || (currentTime - lastUpdateTime >= 1000)) {
								System.out.println(
										fileName + " has been " + (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modified" : "created"));
								ServerUtils.writer.println(updateCommand);
								System.out.println("Sent update " + updateCommand + " message");
								sendFileToClient(
										getJarPath() + File.separator + "data" + File.separator + fileNameToWatch);
								lastUpdateTime = currentTime;
								bufferActive[0] = true;
								new Timer().schedule(new TimerTask() {
									@Override
									public void run() {
										bufferActive[0] = false;
									}
								}, 1000);
							}
						}
					}
					
					if (!key.reset()) {
						break;
					}
				}
			} catch (IOException e) {
				System.err.println("Error setting up file watch: " + e);
			}
		});
		watchThread.setDaemon(true);
		watchThread.start();
	}
	
	public static String getJarPath() {
		try {
			String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			
			return new File(jarPath).getParent();
		} catch (URISyntaxException e) {
			System.err.println("GetJarPath failed: " + e);
			return "";
		}
	}
	
	private static void deleteFile(String filename) {
		try {
			String filePath = getJarPath() + File.separator + "data" + File.separator + filename;
			Path path = Path.of(filePath);
			if (Files.exists(path)) {
				Files.delete(path);
				System.out.println("Deleted " + filename);
			}
		} catch (IOException e) {
			System.err.println("Error Deleting " + filename);
		}
	}
	
	public static void deleteFiles() {
		deleteFile("currentID.xml");
		deleteFile("callout.xml");
		deleteFile("worldPeds.data");
		deleteFile("worldCars.data");
		deleteFile("location.data");
		deleteFile("trafficStop.data");
	}
	
	public static void createFiles() {
		createFile("currentID.xml");
		createFile("callout.xml");
		createFile("worldPeds.data");
		createFile("worldCars.data");
		createFile("location.data");
		createFile("trafficStop.data");
	}
	
	public static void createFile(String filename) {
		try {
			String filePath = getJarPath() + File.separator + "data" + File.separator + filename;
			Path path = Path.of(filePath);
			if (!Files.exists(path)) {
				Files.createFile(path);
				System.out.println("created: " + filename);
			} else {
				System.out.println("already exists: " + filename);
			}
		} catch (IOException e) {
			System.err.println("Could not create file: " + filename);
		}
	}
	
}
