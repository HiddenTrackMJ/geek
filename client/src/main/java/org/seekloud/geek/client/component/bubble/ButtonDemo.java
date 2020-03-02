package org.seekloud.geek.client.component.bubble;

import com.jfoenix.animation.alert.JFXAlertAnimation;
import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class ButtonDemo extends Application {

    @Override
    public void start(Stage stage) {
        FlowPane main = new FlowPane();
        main.setVgap(20);
        main.setHgap(20);



        main.getChildren().add(new Button("Java Button"));
        JFXButton jfoenixButton = new JFXButton("JFoenix Button");
        main.getChildren().add(jfoenixButton);


        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setBody(new Label("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."));
        JFXAlert<Void> alert = new JFXAlert<>(stage);
        alert.setOverlayClose(true);
        alert.setAnimation(JFXAlertAnimation.CENTER_ANIMATION);
        alert.setContent(layout);
        alert.initModality(Modality.NONE);
        jfoenixButton.setOnAction(action-> alert.showAndWait());


        JFXButton button = new JFXButton("RAISED BUTTON");
        button.getStyleClass().add("button-raised");
        main.getChildren().add(button);

        JFXButton button1 = new JFXButton("DISABLED");
        button1.setDisable(true);
        main.getChildren().add(button1);

        StackPane pane = new StackPane();
        pane.getChildren().add(main);
        StackPane.setMargin(main, new Insets(100));
        pane.setStyle("-fx-background-color:WHITE");

        final Scene scene = new Scene(new JFXDecorator(stage, pane), 800, 200);
//        scene.getStylesheets().add(ButtonDemo.class.getResource("/css/jfoenix-components.css").toExternalForm());

        stage.setTitle("JFX Button Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
