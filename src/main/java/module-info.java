module edu.lk.ijse.chatapplication {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.lk.ijse.chatapplication to javafx.fxml;
    exports edu.lk.ijse.chatapplication;
}