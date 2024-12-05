module net.guess.rpdevutility {
    requires javafx.controls;
    requires javafx.fxml;


    opens net.guess.rpdevutility to javafx.fxml;
    exports net.guess.rpdevutility;
}