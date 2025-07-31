import model.GameModel;
import view.GameView;
import controller.GameController;
import javax.swing.*;

//точка входа
public class FlappyBird {
    public static void main(String[] args) {
        //создание интерфейса происходит в EDT
        SwingUtilities.invokeLater(() -> {
            GameModel model = new GameModel();//данные
            GameView view = new GameView();//интерфейс
            GameController controller = new GameController(model, view);//управление
            view.setController(controller);//связываем представление с контроллером
            controller.startGame();
        });
    }
}