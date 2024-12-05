package com.Guess.ReportsPlusServer.util.network;

import com.Guess.ReportsPlusServer.Launcher;
import com.Guess.ReportsPlusServer.util.log.LogUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.Guess.ReportsPlusServer.MainController.SettingsStage;
import static com.Guess.ReportsPlusServer.MainController.handleClose;
import static com.Guess.ReportsPlusServer.util.log.LogUtils.log;
import static com.Guess.ReportsPlusServer.util.log.LogUtils.logError;

public class FileUtils {
	
	private static double xOffset = 0;
	private static double yOffset = 0;
	static long idLastUpdateTimestamp = 0;
	static boolean idUpdateBufferActive = false;
	
	public static synchronized void sendFileToClient(String fileToSend) throws IOException {
		try (Socket sock = ServerUtils.serverSocket.accept(); FileInputStream fis = new FileInputStream(
				fileToSend); BufferedInputStream bis = new BufferedInputStream(
				fis); OutputStream os = sock.getOutputStream()) {
			
			byte[] mybytearray = new byte[(int) new File(fileToSend).length()];
			bis.read(mybytearray, 0, mybytearray.length);
			log("Sending " + fileToSend + "(" + mybytearray.length + " bytes)", LogUtils.Severity.INFO);
			os.write(mybytearray, 0, mybytearray.length);
			os.flush();
			log("Sent.", LogUtils.Severity.DEBUG);
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
										log(fileName + " has been " + (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modified" : "created"),
										    LogUtils.Severity.INFO);
										ServerUtils.writer.println("UPDATE_ID");
										log("sent update ID message", LogUtils.Severity.DEBUG);
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
								logError("Error checking file size: ", e);
							}
						}
					}
					
					boolean valid = key.reset();
					if (!valid) {
						break;
					}
				}
			} catch (IOException e) {
				logError("Error setting up file watch: ", e);
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
								log(fileName + " has been " + (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modified" : "created"),
								    LogUtils.Severity.INFO);
								ServerUtils.writer.println(updateCommand);
								log("Sent update " + updateCommand + " message", LogUtils.Severity.DEBUG);
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
				logError("Error setting up file watch: ", e);
			}
		});
		watchThread.setDaemon(true);
		watchThread.start();
	}
	
	public static AnchorPane createTitleBar(String titleText) {
		ColorAdjust colorAdjust = new ColorAdjust();
		colorAdjust.setSaturation(-1.0);
		colorAdjust.setBrightness(-0.45);
		
		Label titleLabel = new Label(titleText);
		titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
		titleLabel.setAlignment(Pos.CENTER);
		AnchorPane.setLeftAnchor(titleLabel, (double) 0);
		AnchorPane.setRightAnchor(titleLabel, (double) 0);
		AnchorPane.setTopAnchor(titleLabel, (double) 0);
		AnchorPane.setBottomAnchor(titleLabel, (double) 0);
		titleLabel.setEffect(colorAdjust);
		titleLabel.setMouseTransparent(true);
		
		AnchorPane titleBar = new AnchorPane(titleLabel);
		titleBar.setMinHeight(30);
		titleBar.setStyle("-fx-background-color: #383838;");
		
		Image placeholderImage = new Image(Objects.requireNonNull(
				Launcher.class.getResourceAsStream("/com/Guess/ReportsPlusServer/imgs/Logo.png")));
		ImageView placeholderImageView = new ImageView(placeholderImage);
		placeholderImageView.setFitWidth(49);
		placeholderImageView.setFitHeight(49);
		AnchorPane.setLeftAnchor(placeholderImageView, 0.0);
		AnchorPane.setTopAnchor(placeholderImageView, -10.0);
		AnchorPane.setBottomAnchor(placeholderImageView, -10.0);
		placeholderImageView.setEffect(colorAdjust);
		
		Image closeImage = new Image(Objects.requireNonNull(
				Launcher.class.getResourceAsStream("/com/Guess/ReportsPlusServer/imgs/cross.png")));
		ImageView closeImageView = new ImageView(closeImage);
		closeImageView.setFitWidth(15);
		closeImageView.setFitHeight(15);
		AnchorPane.setRightAnchor(closeImageView, 15.0);
		AnchorPane.setTopAnchor(closeImageView, 7.0);
		closeImageView.setEffect(colorAdjust);
		
		Image minimizeImage = new Image(Objects.requireNonNull(
				Launcher.class.getResourceAsStream("/com/Guess/ReportsPlusServer/imgs/minimize.png")));
		ImageView minimizeImageView = new ImageView(minimizeImage);
		minimizeImageView.setFitWidth(15);
		minimizeImageView.setFitHeight(15);
		AnchorPane.setRightAnchor(minimizeImageView, 42.5);
		AnchorPane.setTopAnchor(minimizeImageView, 7.0);
		minimizeImageView.setEffect(colorAdjust);
		
		Rectangle closeRect = new Rectangle(20, 20);
		Rectangle minimizeRect = new Rectangle(20, 20);
		
		closeRect.setFill(Color.TRANSPARENT);
		minimizeRect.setFill(Color.TRANSPARENT);
		
		closeRect.setOnMouseClicked(event -> {
			Stage stage = (Stage) titleBar.getScene().getWindow();
			stage.close();
			if (stage != SettingsStage) {
				handleClose();
			} else {
				SettingsStage = null;
			}
		});
		
		minimizeRect.setOnMouseClicked(event -> {
			Stage stage = (Stage) titleBar.getScene().getWindow();
			stage.setIconified(true);
		});
		
		AnchorPane.setRightAnchor(closeRect, 12.5);
		AnchorPane.setTopAnchor(closeRect, 6.3);
		AnchorPane.setRightAnchor(minimizeRect, 42.5);
		AnchorPane.setTopAnchor(minimizeRect, 6.3);
		
		titleBar.getChildren().addAll(placeholderImageView, closeRect, minimizeRect, closeImageView, minimizeImageView);
		closeRect.toFront();
		minimizeRect.toFront();
		
		titleBar.setOnMouseDragged(event -> {
			Stage stage = (Stage) titleBar.getScene().getWindow();
			stage.setX(event.getScreenX() - xOffset);
			stage.setY(event.getScreenY() - yOffset);
		});
		
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});
		
		return titleBar;
	}
	
	public static String readFile(String filePath) {
		Path path = Paths.get(filePath);
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			logError("Error Reading File " + filePath + ": ", e);
		}
		return content.toString();
	}
	
	public static void writeFile(String filePath, String content) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
			writer.write(content);
		} catch (IOException e) {
			logError("Error Writing File " + filePath + ": ", e);
		}
	}
	
	public static String getJarPath() {
		try {
			String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			
			return new File(jarPath).getParent();
		} catch (URISyntaxException e) {
			logError("GetJarPath URI Syntax Error: ", e);
			return "";
		}
	}
	
	private static void deleteFile(String filename) {
		try {
			String filePath = getJarPath() + File.separator + "data" + File.separator + filename;
			Path path = Path.of(filePath);
			if (Files.exists(path)) {
				Files.delete(path);
				log("deleted: " + filename, LogUtils.Severity.DEBUG);
			}
		} catch (IOException e) {
			logError("Could not delete file: ", e);
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
				log("Created: " + filename, LogUtils.Severity.DEBUG);
			} else {
				log("File already exists: " + filename, LogUtils.Severity.WARN);
			}
		} catch (IOException e) {
			logError("Could not create file: ", e);
		}
	}
	
}
