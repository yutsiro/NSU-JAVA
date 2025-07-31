package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class PushCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (arguments.length != 1) {
            throw new CalculatorException("PUSH requires exactly one argument");
        }
        String arg = arguments[0];
        Double value;
        if (parameters.containsKey(arg)) {
            value = parameters.get(arg);
        } else {
            try {//дробное число с запятой
                value = Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                throw new CalculatorException("Invalid argument for PUSH: " + arg, e);
            }
        }
        stack.push(value);
    }
}