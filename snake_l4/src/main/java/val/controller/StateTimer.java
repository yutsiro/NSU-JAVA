package val.controller;

import lombok.AllArgsConstructor;
import val.controller.interfaces.StateConstructor;

@AllArgsConstructor
public class StateTimer implements Runnable{

    private StateConstructor stateConstructor;
    private int delay;

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(delay);
                stateConstructor.releaseFreshState();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
