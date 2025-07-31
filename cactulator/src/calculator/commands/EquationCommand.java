package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class EquationCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (arguments.length != 2) {
            throw new CalculatorException(("EQUATION requires two arguments: exp number and x-value"));
        }
        int exp;
        try {
            exp = Integer.parseInt(arguments[0]);
        } catch (NumberFormatException e) {
            throw new CalculatorException("Invalid exp for EQUATION: " + arguments[0], e);
        }
        if (exp > 10) {
            throw new CalculatorException("Exp value is too big.");
        }
        if (exp <= 0) {
            throw new CalculatorException("Exp value is too small.");
        }
        if (stack.size() < exp) {
            throw new CalculatorException("Not enough elements in stack for coefs");
        }
        try {
            double xValue = Double.parseDouble(arguments[1]);
            double multiplier = xValue;
            double result = 0;
            for (int i = 0; i < exp; i++) {
                double coef = stack.pop();
                result += coef * xValue;
                xValue *= multiplier;
            }
            stack.push(result);
        } catch (NumberFormatException e) {
            throw new CalculatorException("Invalid value for EQUATION: " + arguments[1], e);
        }
    }
}
