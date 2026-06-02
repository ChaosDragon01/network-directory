module com.directoryclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires retrofit2;
    requires okhttp3;

    exports com.directoryclient;
    exports com.directoryclient.network;
    opens com.directoryclient.ui to javafx.fxml;
}
