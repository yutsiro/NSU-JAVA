package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class DefineCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (arguments.length != 2) {
            throw new CalculatorException("DEFINE requires exactly two arguments: name and value");
        }
        String name = arguments[0];//только строки
        try {
            double value = Double.parseDouble(arguments[1]);
            parameters.put(name, value);
        } catch (NumberFormatException e) {
            throw new CalculatorException("Invalid value for DEFINE: " + arguments[1], e);
        }
    }
}