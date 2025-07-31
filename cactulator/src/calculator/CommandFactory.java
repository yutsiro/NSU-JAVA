package calculator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommandFactory {
    private final Map<String, String> commandClasses = new HashMap<>();

    public CommandFactory(String configFile) throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(configFile));
        for (String key : properties.stringPropertyNames()) {
            commandClasses.put(key, properties.getProperty(key));
        }
    }

    public Command createCommand(String commandName) throws CalculatorException {
        if (!commandClasses.containsKey(commandName)) {
            throw new CalculatorException("Unknown command: " + commandName);
        }
        String className = commandClasses.get(commandName);
        try {
            Class<?> clazz = Class.forName(className);
            return (Command) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CalculatorException("Failed to create command: " + commandName, e);
        }
    }
}