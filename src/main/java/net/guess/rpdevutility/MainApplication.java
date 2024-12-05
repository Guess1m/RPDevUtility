package net.guess.rpdevutility;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
	public static void main(String[] args) {
		launch();
	}
	
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("main.fxml"));
		Scene scene = new Scene(fxmlLoader.load());
		stage.setTitle("RP Developer Tool");
		stage.setScene(scene);
		stage.show();
	}
}