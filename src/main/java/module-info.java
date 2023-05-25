module client.multiplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    // не будет сверху, не будет снизу - не заработает gson
    opens client.multiplayer to javafx.fxml, com.google.gson;
    exports client.multiplayer;
}