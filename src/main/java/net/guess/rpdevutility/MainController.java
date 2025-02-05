package net.guess.rpdevutility;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import net.guess.rpdevutility.network.ServerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import static net.guess.rpdevutility.network.FileUtils.*;
import static net.guess.rpdevutility.network.ServerUtils.BROADCAST_PORT;
import static net.guess.rpdevutility.network.ServerUtils.isConnected;

public class MainController {
	int NumberCount = 0;
	@javafx.fxml.FXML
	private ToggleButton LicNone;
	@javafx.fxml.FXML
	private ToggleButton wantedFalse;
	@javafx.fxml.FXML
	private ToggleButton LicRandom;
	@javafx.fxml.FXML
	private ToggleGroup wantedStatus;
	@javafx.fxml.FXML
	private ToggleButton wantedTrue;
	@javafx.fxml.FXML
	private ToggleButton LicValid;
	@javafx.fxml.FXML
	private ToggleButton LicUnlicensed;
	@javafx.fxml.FXML
	private ToggleGroup licStatus;
	@javafx.fxml.FXML
	private ToggleButton LicExpired;
	@javafx.fxml.FXML
	private ToggleButton LicSuspended;
	@javafx.fxml.FXML
	private TextField WPName;
	@javafx.fxml.FXML
	private ToggleButton regValid;
	@javafx.fxml.FXML
	private ToggleButton insValid;
	@javafx.fxml.FXML
	private ToggleButton insRandom;
	@javafx.fxml.FXML
	private ToggleButton insExpired;
	@javafx.fxml.FXML
	private ToggleButton regExpired;
	@javafx.fxml.FXML
	private TextField WVPlate;
	@javafx.fxml.FXML
	private ToggleButton insNone;
	@javafx.fxml.FXML
	private ToggleButton regNone;
	@javafx.fxml.FXML
	private ToggleGroup licStatus11;
	@javafx.fxml.FXML
	private ToggleButton regRandom;
	@javafx.fxml.FXML
	private ToggleGroup licStatus1;
	
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
			
			// Generate random name
			String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Chris", "Anna"};
			String[] lastNames = {"Smith", "Johnson", "Brown", "Taylor", "Anderson", "Lee", "Wilson", "Martin"};
			String name = firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(
					lastNames.length)];
			
			// Generate random birthday
			int year = 1950 + random.nextInt(60);
			int month = 1 + random.nextInt(12);
			int day = 1 + random.nextInt(LocalDate.of(year, month, 1).lengthOfMonth());
			String birthday = LocalDate.of(year, month, day).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
			
			// Generate random gender
			String gender = random.nextBoolean() ? "Male" : "Female";
			
			// Generate random address
			String[] streets = {"Main St", "Elm St", "Park Ave", "Broadway", "Maple St", "Cedar Ave", "Pine Dr", "Oak St"};
			String[] cities = {"Springfield", "Riverside", "Centerville", "Georgetown", "Fairview", "Franklin", "Greenville"};
			String address = (100 + random.nextInt(900)) + " " + streets[random.nextInt(
					streets.length)] + ", " + cities[random.nextInt(cities.length)];
			
			// Generate random PedModel and LicenseNumber
			String[] pedModels = {"A_M_M_SALTON_01", "A_F_M_SALTON_01", "A_M_M_SALTON_02"};
			String pedModel = pedModels[random.nextInt(pedModels.length)];
			String licenseNumber = String.valueOf(1000000000L + random.nextLong(9000000000L));
			
			// Generate XML content
			String xmlContent = String.format("""
					                                      <?xml version="1.0" encoding="utf-8"?>
					                                      <IDs>
					                                        <ID>
					                                          <Name>%s</Name>
					                                          <Birthday>%s</Birthday>
					                                          <Gender>%s</Gender>
					                                          <Address>%s</Address>
					                                          <PedModel>%s</PedModel>
					                                          <LicenseNumber>%s</LicenseNumber>
					                                        </ID>
					                                      </IDs>""", name, birthday, gender, address, pedModel, licenseNumber);
			
			String filePath = getJarPath() + "/data/currentID.xml";
			
			// Write the XML to the file
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(xmlContent);
				System.out.println("Random ID updated.");
			} catch (IOException e) {
				System.err.println("Error writing XML to file: " + e.getMessage());
			}
		}
	}
	
	@javafx.fxml.FXML
	public void updateCallout(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			// Random callout type and message
			String[] calloutTypes = {"Disturbance", "Traffic Stop", "Robbery", "Medical Emergency", "Fire"};
			String calloutType = calloutTypes[random.nextInt(calloutTypes.length)];
			String message = "This is a " + calloutType.toLowerCase() + " callout.";
			
			// Random description
			String[] descriptions = {"A very important situation.", "Respond immediately.", "Additional units may be required.", "Use caution on approach."};
			String description = descriptions[random.nextInt(descriptions.length)];
			
			// Random priority
			String[] priorities = {"Code 1", "Code 2", "Code 3"};
			String priority = priorities[random.nextInt(priorities.length)];
			
			// Random street, area, and county
			String[] streets = {"Portola Dr", "Vinewood Blvd", "Alta St", "Del Perro Fwy", "San Andreas Ave"};
			String[] areas = {"Rockford Hills", "Vespucci Canals", "Downtown Vinewood", "Pillbox Hill", "Mirror Park"};
			String[] counties = {"Los Santos", "Blaine County"};
			String street = streets[random.nextInt(streets.length)];
			String area = areas[random.nextInt(areas.length)];
			String county = counties[random.nextInt(counties.length)];
			
			// Current time and date
			String startTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a"));
			String startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			
			// Generate XML content
			String xmlContent = """
					<?xml version="1.0" encoding="utf-8"?>
					<Callouts>
					  <Callout>
					    <Number>%d</Number>
					    <Type>%s</Type>
					    <Description>%s</Description>
					    <Message>%s</Message>
					    <Priority>%s</Priority>
					    <Street>%s</Street>
					    <Area>%s</Area>
					    <County>%s</County>
					    <StartTime>%s</StartTime>
					    <StartDate>%s</StartDate>
					  </Callout>
					</Callouts>
					""".formatted(NumberCount++, calloutType, description, message, priority, street, area, county,
			                      startTime, startDate);
			
			String filePath = getJarPath() + "/data/callout.xml";
			
			// Write the generated XML to a file
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(xmlContent);
				System.out.println("Random Callout updated: #" + NumberCount);
			} catch (IOException e) {
				System.err.println("Error writing XML to file: " + e.getMessage());
			}
		}
	}
	
	@javafx.fxml.FXML
	public void updateLocation(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			// Random street, area, and county
			String[] streets = {"Zancudo Ave", "Grapeseed Main St", "Joshua Rd", "Panorama Dr"};
			String[] areas = {"Sandy Shores", "Grapeseed", "Harmony", "Paleto Bay"};
			String[] counties = {"Blaine County", "Los Santos County"};
			
			String street = streets[random.nextInt(streets.length)];
			String area = areas[random.nextInt(areas.length)];
			String county = counties[random.nextInt(counties.length)];
			
			// Combine into location string
			String locationData = street + ", " + area + ", " + county;
			
			String filePath = getJarPath() + "/data/gameData.data";
			
			// Write the generated location data to a file
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write("location=" + locationData + "|time=" + random.nextInt(1, 9) + ":" + random.nextInt(1,
				                                                                                                 9) + random.nextInt(
						1, 9) + " PM");
				System.out.println("Random Location updated: " + locationData);
			} catch (IOException e) {
				System.err.println("Error writing game data to file: " + e.getMessage());
			}
		}
	}
	
	@javafx.fxml.FXML
	public void updateTrafficStop(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			// Random license plate
			String licensePlate = String.valueOf(NumberCount++);
			
			// Random model
			String[] models = {"ASTEROPE", "BUFFALO", "DOMINATOR", "FELTZER", "SULTAN", "BANSHEE", "ELEGY"};
			String model = models[random.nextInt(models.length)];
			
			// Random stolen status
			String isStolen = random.nextBoolean() ? "True" : "False";
			
			// Random police vehicle status
			String isPolice = random.nextBoolean() ? "True" : "False";
			
			// Random owner
			String[] firstNames = {"Julia", "Michael", "David", "Sophia", "Chris", "Emma", "James", "Olivia"};
			String[] lastNames = {"Alba", "Smith", "Johnson", "Brown", "Taylor", "Anderson", "Lee", "Wilson"};
			String owner = firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(
					lastNames.length)];
			
			// Random registration and insurance status
			String[] status = {"Valid", "Expired", "None"};
			String registration = status[random.nextInt(status.length)];
			String insurance = status[random.nextInt(status.length)];
			
			// Random color in RGB format
			int red = 30 + random.nextInt(226); // Ensure non-black and non-white colors
			int green = 30 + random.nextInt(226);
			int blue = 30 + random.nextInt(226);
			String color = red + "-" + green + "-" + blue;
			
			// Random street and area
			String[] streets = {"126 Forum Drive", "311 Grove Street", "504 Spanish Avenue", "205 Main Street"};
			String[] areas = {"Sandy Shores", "Grapeseed", "Paleto Bay", "Los Santos"};
			String street = streets[random.nextInt(streets.length)];
			String area = areas[random.nextInt(areas.length)];
			
			// Create the data string
			String stopData = String.format(
					"licenseplate=%s&model=%s&isstolen=%s&ispolice=%s&owner=%s&registration=%s&insurance=%s&color=%s&street=%s&area=%s",
					licensePlate, model, isStolen, isPolice, owner, registration, insurance, color, street, area);
			
			String filePath = getJarPath() + "/data/trafficStop.data";
			
			// Write the generated data to a file
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(stopData);
				System.out.println("Random trafficstop updated with number: " + licensePlate);
			} catch (IOException e) {
				System.err.println("Error writing trafficstop data to file: " + e.getMessage());
			}
		}
	}
	
	@javafx.fxml.FXML
	public void updateWorldPed(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			String name = WPName.getText().trim();
			
			String licenseNumber = String.valueOf(1000000000L + random.nextLong(9000000000L));
			
			String[] pedModels = {"A_M_M_TRAMP_01", "A_F_Y_HIPSTER_02", "A_M_Y_BUSINESS_01", "A_F_M_FATWHITE_01", "A_M_Y_LATINO_01", "MP_M_SHOPKEEP_01"};
			String pedModel = pedModels[random.nextInt(pedModels.length)];
			
			int year = 1950 + random.nextInt(50);
			int month = 1 + random.nextInt(12);
			int day = 1 + random.nextInt(LocalDate.of(year, month, 1).lengthOfMonth());
			String birthday = month + "/" + day + "/" + year;
			
			String gender = random.nextBoolean() ? "Male" : "Female";
			
			String[] streets = {"North Calafia Way", "Grove Street", "Mirror Park Blvd", "Vinewood Blvd", "Senora Rd"};
			String[] areas = {"Rancho", "Pillbox Hill", "Sandy Shores", "Richman"};
			String[] counties = {"Los Santos", "Blaine County"};
			String address = (100 + random.nextInt(900)) + " " + streets[random.nextInt(
					streets.length)] + ", " + areas[random.nextInt(areas.length)] + ", " + counties[random.nextInt(
					counties.length)];
			
			String isWanted = wantedTrue.isSelected() ? "True" : "False";
			
			String[] licenseStatuses = {"Valid", "Expired", "Suspended", "None", "Unlicensed"};
			String licenseStatus = LicRandom.isSelected() ? licenseStatuses[random.nextInt(
					licenseStatuses.length)] : LicValid.isSelected() ? "Valid" : LicExpired.isSelected() ? "Expired" : LicSuspended.isSelected() ? "Suspended" : LicNone.isSelected() ? "None" : "Unlicensed";
			
			LocalDate expirationDate = LocalDate.now().plusDays(random.nextInt(1000));
			String licenseExpiration = expirationDate.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
			
			String[] weaponPermitTypes = {"CcwPermit", "FflPermit"};
			String weaponPermitType = weaponPermitTypes[random.nextInt(weaponPermitTypes.length)];
			String weaponPermitStatus = random.nextBoolean() ? "Valid" : "None";
			String weaponPermitExpiration = weaponPermitStatus.equals("Valid") ? expirationDate.plusDays(
					random.nextInt(365)).format(DateTimeFormatter.ofPattern("MM-dd-yyyy")) : "";
			
			String fishPermitStatus = random.nextBoolean() ? "Valid" : "Expired";
			String fishPermitExpiration = fishPermitStatus.equals("Valid") ? expirationDate.plusDays(
					random.nextInt(365)).format(DateTimeFormatter.ofPattern("MM-dd-yyyy")) : "";
			
			String huntPermitStatus = "None";
			String huntPermitExpiration = "";
			
			String isOnParole = "False";
			String isOnProbation = "False";
			
			int timesStopped = random.nextInt(10);
			
			String output = "name=" + name + "&licensenumber=" + licenseNumber + "&pedModel=" + pedModel + "&birthday=" + birthday + "&gender=" + gender + "&address=" + address + "&isWanted=" + isWanted + "&licenseStatus=" + licenseStatus + "&licenseExpiration=" + licenseExpiration + "&weaponPermitType=" + weaponPermitType + "&weaponPermitStatus=" + weaponPermitStatus + "&weaponPermitExpiration=" + weaponPermitExpiration + "&fishPermitStatus=" + fishPermitStatus + "&fishPermitExpiration=" + fishPermitExpiration + "&huntPermitStatus=" + huntPermitStatus + "&huntPermitExpiration=" + huntPermitExpiration + "&isOnParole=" + isOnParole + "&isOnProbation=" + isOnProbation + "&timesStopped=" + timesStopped + "|";
			
			String filePath = getJarPath() + "/data/worldPeds.data";
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
				writer.append(output);
				System.out.println("Random World Ped updated: " + name);
			} catch (IOException e) {
				System.err.println("Error writing World Ped data to file: " + e.getMessage());
			}
		}
	}
	
	@javafx.fxml.FXML
	public void updateWorldVeh(ActionEvent actionEvent) {
		if (isConnected) {
			Random random = new Random();
			
			// Generate random license plate
			String licensePlate = WVPlate.getText().trim();
			
			// Generate random model
			String[] models = {"BALLER2", "SULTAN", "DOMINATOR", "ASTEROPE", "BUFFALO", "ELEGY", "FELTZER"};
			String model = models[random.nextInt(models.length)];
			
			// Generate random registration and insurance expiration dates
			LocalDate regExpDate = LocalDate.now().plusDays(random.nextInt(1000));
			LocalDate insExpDate = regExpDate.plusDays(random.nextInt(365));
			String regExp = regExpDate.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
			String insExp = insExpDate.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
			
			// Generate random VIN (17 alphanumeric characters)
			String vin = random.ints(48, 123).filter(i -> Character.isLetterOrDigit(i)).limit(17).collect(
					StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
			
			// Random stolen status
			String isStolen = random.nextBoolean() ? "True" : "False";
			
			// Random police vehicle status
			String isPolice = random.nextBoolean() ? "True" : "False";
			
			// Generate random owner and address
			String[] firstNames = {"Carlos", "Jessica", "Michael", "Sarah", "David", "Emily", "Chris", "Anna"};
			String[] lastNames = {"Casanova", "Smith", "Johnson", "Brown", "Taylor", "Anderson", "Lee", "Wilson"};
			String[] addresses = {"64 South Shambles St", "35 Elysian Fields Fwy", "79 Popular St", "696 Portola Dr", "401 Mirror Park Blvd", "339 Goma St", "64 South Shambles St", "553 Eclipse Blvd", "359 Ginger St", "65 South Shambles St", "222 San Andreas Ave", "944 Route 68"};
			
			String owner = random.nextBoolean() ? firstNames[random.nextInt(
					firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)] : "Government";
			String ownerAddress = random.nextBoolean() ? addresses[random.nextInt(addresses.length)] : "N/A";
			
			// Generate random driver
			String driver = random.nextBoolean() ? firstNames[random.nextInt(
					firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)] : "";
			
			// Generate random registration status
			String regStatus = getValue(random, regExpired, regNone, regValid, regRandom);
			
			// Generate random insurance status
			String insStatus = getValue(random, insExpired, insNone, insValid, insRandom);
			
			// Generate random color in RGB format
			String color = random.nextInt(256) + "-" + random.nextInt(256) + "-" + random.nextInt(256);
			
			// Combine data into the required format
			String vehicleData = "licenseplate=" + licensePlate + "&model=" + model + "&regexp=" + regExp + "&insexp=" + insExp + "&vin=" + vin + "&isstolen=" + isStolen + "&ispolice=" + isPolice + "&owner=" + owner + "&owneraddress=" + ownerAddress + "&driver=" + driver + "&registration=" + regStatus + "&insurance=" + insStatus + "&color=" + color + "|";
			
			// Write the data to a file
			String filePath = getJarPath() + "/data/worldCars.data";
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
				writer.append(vehicleData);
				System.out.println("World Vehicle updated");
			} catch (IOException e) {
				System.err.println("Error writing world vehicle data to file: " + e.getMessage());
			}
		}
	}
	
	private String getValue(Random random, ToggleButton regExpired, ToggleButton regNone, ToggleButton regValid, ToggleButton regRandom) {
		String regStatus = null;
		if (regExpired.isSelected()) {
			regStatus = "Expired";
		}
		if (regNone.isSelected()) {
			regStatus = "None";
		}
		if (regValid.isSelected()) {
			regStatus = "Valid";
		}
		if (regRandom.isSelected()) {
			String[] registrationStatuses = {"Valid", "Expired", "None"};
			regStatus = registrationStatuses[random.nextInt(registrationStatuses.length)];
		}
		return regStatus;
	}
	
}