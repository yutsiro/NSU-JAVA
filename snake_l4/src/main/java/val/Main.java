package val;

import val.controller.GameContext;
import val.controller.MainController;
import val.gui.ControlPanel;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        GameContext context = new GameContext();

        MainController controller = new MainController(context);

        JFrame frame = new JFrame("Game controls");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        ControlPanel panel = new ControlPanel(controller);
        controller.setControls(panel);
        frame.add(panel);
        frame.pack();

    }
}