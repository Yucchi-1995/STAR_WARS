package starwars.styletextcrawler;

import com.interactivemesh.jfx.importer.ImportException;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import java.net.URL;
import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 *
 * @author Yucchi
 */
public class StarWarsStyleTextCrawler extends Application {

    private final static double FIELD_OF_VIEW = 70.0;
    private final static double WIDTH = 1_920;
    private final static double HEIGHT = 1_080;
    private final static int FOG_WIDTH = 1_920;
    private final static int FOG_HEIGHT = 250;
    private double dragStartX;
    private double dragStartY;

    @Override
    public void start(Stage stage) {

        final Group root = new Group();
        // SubScene
        final SubScene subScene = createSubScene();
        // FogScene
        final Parent fogScene = createFogScene();

        root.getChildren().addAll(subScene, fogScene);

        final Scene scene = new Scene(root, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);

        scene.setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
        });

        scene.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragStartX);
            stage.setY(e.getScreenY() - dragStartY);
        });

        stage.setTitle("STAR WARS");
        Image myIcon = new Image(this.getClass().getResource("resources/sakura_icon.png").toExternalForm());
        stage.getIcons().add(myIcon);
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        // オープニングアニメーション
        DoubleProperty openOpacityProperty = new SimpleDoubleProperty(0.0);
        stage.opacityProperty().bind(openOpacityProperty);
        Timeline openTimeline = new Timeline(
                new KeyFrame(
                        new Duration(100),
                        new KeyValue(openOpacityProperty, 0.0)
                ), new KeyFrame(
                        new Duration(2_000),
                        new KeyValue(openOpacityProperty, 1.0)
                ));
        openTimeline.setCycleCount(1);
        openTimeline.play();
        stage.centerOnScreen();
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(we -> {
            // クロージングアニメーション
            DoubleProperty closeOpacityProperty = new SimpleDoubleProperty(1.0);
            stage.opacityProperty().bind(closeOpacityProperty);

            Timeline closeTimeline = new Timeline(
                    new KeyFrame(
                            new Duration(100),
                            new KeyValue(closeOpacityProperty, 1.0)
                    ), new KeyFrame(
                            new Duration(2_500),
                            new KeyValue(closeOpacityProperty, 0.0)
                    ));

            EventHandler<ActionEvent> eh = ae -> {
                Platform.exit();
                System.exit(0);
            };

            closeTimeline.setOnFinished(eh);
            closeTimeline.setCycleCount(1);
            closeTimeline.play();

            we.consume();
        });

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private SubScene createSubScene() {

        // SubScene 用グループ
        final Group subGroup = new Group();

        // 奥行きバッファ有効、アンチエイリアス属性バランス設定
        final SubScene subScene = new SubScene(subGroup, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);

        // 透視投影カメラ
        final PerspectiveCamera cam = new PerspectiveCamera(true);
        // Field of View 
        cam.setFieldOfView(FIELD_OF_VIEW);
        // Clipping Planes
        cam.setNearClip(1.0);
        cam.setFarClip(3_500.0);
        subScene.setCamera(cam);

        // 3D モデルをインポート
        final ObjModelImporter textModelImporter = new ObjModelImporter();
        try {
            URL objUrl = this.getClass().getResource("resources/STAR WARS_1.obj");
            textModelImporter.read(objUrl);
        } catch (ImportException e) {
            // G'catch! :p
            e.printStackTrace();
        }
        final MeshView[] textMesh = textModelImporter.getImport();
        textModelImporter.close();

        final Group textGroup = new Group();
        for (MeshView _textMesh : textMesh) {
            textGroup.getChildren().addAll(_textMesh);
        }

        subGroup.getChildren().add(textGroup);

        // カメラをX軸上で下向きに28度回転させる。
        cam.getTransforms().setAll(new Rotate(-28.0, Rotate.X_AXIS));

        // テキストを水平になるようにX軸を中心に回転させる。
        textGroup.getTransforms().setAll(new Rotate(-90.0, Rotate.X_AXIS));
        // テキストを下方向 300 移動させる。
        textGroup.setTranslateY(300.0);
        // テキストを 500 手前に移動させる。
        textGroup.setTranslateZ(-500.0);

        // テキスト移動アニメーション
        TranslateTransition trans = new TranslateTransition(Duration.millis(35_000), textGroup);
        trans.setToZ(2_400);
        trans.setInterpolator(Interpolator.EASE_IN);
        trans.setCycleCount(Animation.INDEFINITE);
        trans.play();

        return subScene;
    }

    // FogScene
    private Parent createFogScene() {

        Fog fog = new Fog(FOG_WIDTH, FOG_HEIGHT);

        StackPane fogPane = new StackPane(fog.getView());

        return fogPane;
    }

    // フォグもどき
    private static class Fog {

        private final int width;
        private final int height;
        private final Pane fog;
        private final Random rnd = new Random();

        private Fog(int WIDTH, int HEIGHT) {

            this.width = WIDTH;
            this.height = HEIGHT;
            this.fog = new Pane();

            for (int i = 0; i < width + height; i++) {
                fog.getChildren().add(createFogElement());
            }
            fog.setEffect(new GaussianBlur((width + height) / 1.5));

        }

        private Circle createFogElement() {
            Circle circle = new Circle(rnd.nextInt(width - 50) + 25, rnd.nextInt(height - 50) + 25, 15 + rnd.nextInt(50));
            circle.setFill(Color.BLACK);
            AnimationTimer anim = new AnimationTimer() {

                double xVel = rnd.nextDouble() * 40 - 20;
                double yVel = rnd.nextDouble() * 40 - 20;

                long lastUpdate = 0;

                @Override
                public void handle(long now) {
                    if (lastUpdate > 0) {
                        double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                        double x = circle.getCenterX();
                        double y = circle.getCenterY();
                        if (x + elapsedSeconds * xVel > width || x + elapsedSeconds * xVel < 0) {
                            xVel = -xVel;
                        }
                        if (y + elapsedSeconds * yVel > height || y + elapsedSeconds * yVel < 0) {
                            yVel = -yVel;
                        }
                        circle.setCenterX(x + elapsedSeconds * xVel);
                        circle.setCenterY(y + elapsedSeconds * yVel);
                    }
                    lastUpdate = now;
                }

            };
            anim.start();
            return circle;
        }

        public Node getView() {
            return fog;
        }

    }

}
