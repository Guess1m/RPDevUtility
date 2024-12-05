package net.guess.rpdevutility;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import net.guess.rpdevutility.network.ServerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import static net.guess.rpdevutility.network.FileUtils.*;
import static net.guess.rpdevutility.network.ServerUtils.BROADCAST_PORT;
import static net.guess.rpdevutility.network.ServerUtils.isConnected;

public class MainController {
	int IDNum = 0;
	
	public void initialize() {
		Platform.runLater(() -> {
			if (!isConnected) {
				String dataFolderPath = getJarPath() + File.separator + "data";
				File dataFolder = new File(dataFolderPath);
				if (!dataFolder.exists()) {
					dataFolder.mkdirs();
					System.out.println("Created data folder: " + dataFolderPath);
				} else {
					System.out.println("Data folder already exists, clearing: " + dataFolderPath);
					deleteFiles();
				}
				System.out.println("Creating base files before starting server...");
				createFiles();
				
				new Thread(() -> ServerUtils.startBroadcasting(BROADCAST_PORT)).start();
			} else {
				System.out.println("Already Connected");
			}
			try {
				ServerUtils.runStartServerRefresh(BROADCAST_PORT);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number");
			}
		});
	}
	
	@javafx.fxml.FXML
	public void updateID(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			// Random name
			String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Chris", "Anna"};
			String[] lastNames = {"Smith", "Johnson", "Brown", "Taylor", "Anderson", "Lee", "Wilson", "Martin"};
			String name = firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(
					lastNames.length)];
			
			// Random birthday
			int year = 1950 + random.nextInt(60); // Random year between 1950 and 2009
			int month = 1 + random.nextInt(12);  // Random month
			int day = 1 + random.nextInt(LocalDate.of(year, month, 1).lengthOfMonth()); // Valid day for month/year
			String birthday = LocalDate.of(year, month, day).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
			
			// Random gender
			String gender = random.nextBoolean() ? "Male" : "Female";
			
			// Random address
			String[] streets = {"Main St", "Elm St", "Park Ave", "Broadway", "Maple St", "Cedar Ave", "Pine Dr", "Oak St"};
			String[] cities = {"Springfield", "Riverside", "Centerville", "Georgetown", "Fairview", "Franklin", "Greenville"};
			String address = (100 + random.nextInt(900)) + " " + streets[random.nextInt(
					streets.length)] + ", " + cities[random.nextInt(cities.length)];
			
			// Random image address and index
			String imageAddress = "ID Number: " + IDNum++;
			int index = random.nextInt(100);
			
			String xmlContent = """
					<?xml version="1.0" encoding="utf-8"?>
					<IDs>
					  <ID>
					    <Name>%s</Name>
					    <Birthday>%s</Birthday>
					    <Gender>%s</Gender>
					    <Address>%s</Address>
					    <Address>%s</Address>
					    <Index>%d</Index>
					  </ID>
					</IDs>
					""".formatted(name, birthday, gender, address, imageAddress, index);
			
			String filePath = getJarPath() + "/data/currentID.xml";
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(xmlContent);
				System.out.println("Random ID updated #" + IDNum);
			} catch (IOException e) {
				System.err.println("Error writing XML to file: " + e.getMessage());
			}
		}
	}
	
}