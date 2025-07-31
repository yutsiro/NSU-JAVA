package calculator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.io.IOException;

public class Calculator {
    private static final Logger LOGGER = Logger.getLogger(Calculator.class.getName());
    private final Deque<Double> stack = new ArrayDeque<>();
    private final Map<String, Double> parameters = new HashMap<>();
    private final CommandFactory commandFactory;

    public Calculator(String configFile) throws IOException {
        this.commandFactory = new CommandFactory(configFile);
    }

    public void processCommand(String line) throws CalculatorException {
        if (line.startsWith("#")) {
            return;
        }

        String[] parts = line.split("\\s+");
        String commandName = parts[0];
        String[] arguments = new String[parts.length - 1];
        System.arraycopy(parts, 1, arguments, 0, arguments.length);

        Command command = commandFactory.createCommand(commandName);
        LOGGER.info("Executing command: " + commandName);
        command.execute(stack, parameters, arguments);
    }

    public Deque<Double> getStack() {
        return stack;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }
}