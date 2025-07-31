package calculator;

import org.testng.Assert;//класс TestNG для проверки утверждений
import org.testng.annotations.BeforeMethod;
//аннтоация для метода, который выполняется перед каждым тестом
import org.testng.annotations.Test;//аннтоация для обозначения тестовых методов

import java.io.IOException;

public class CalculatorTest {
    private Calculator calculator;//юзается во всех тестах
    //это поле инициализируется перед каждым тестом в методе setUp
    @BeforeMethod
    public void setUp() throws IOException {
        calculator = new Calculator("resources/commands.properties");
    }//это все выполняется перед каждым @Test
    //создает объект Calculator перед каждым тестом
    @Test
    public void testDefineAndPush() throws CalculatorException {
        calculator.processCommand("DEFINE a 4");
        calculator.processCommand("PUSH a");
        Assert.assertEquals(calculator.getStack().peek(), 4.0);
    }

    @Test
    public void testAdd() throws CalculatorException {
        calculator.processCommand("PUSH 3");
        calculator.processCommand("PUSH 5");
        calculator.processCommand("+");
        Assert.assertEquals(calculator.getStack().peek(), 8.0);
    }

    @Test
    public void testSqrt() throws CalculatorException {
        calculator.processCommand("PUSH 16");
        calculator.processCommand("SQRT");
        Assert.assertEquals(calculator.getStack().peek(), 4.0);
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testSqrtNegative() throws CalculatorException {
        calculator.processCommand("PUSH -4");
        calculator.processCommand("SQRT");
    }//ждет выброс исключения

    @Test
    public void testPop() throws CalculatorException {
        calculator.processCommand("PUSH 10");
        calculator.processCommand("POP");
        Assert.assertTrue(calculator.getStack().isEmpty());
    }

    @Test
    public void testSubtract() throws CalculatorException {
        calculator.processCommand("PUSH 10");
        calculator.processCommand("PUSH 4");
        calculator.processCommand("-");
        Assert.assertEquals(calculator.getStack().peek(), 6.0);
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testSubtractNotEnoughElements() throws CalculatorException {
        calculator.processCommand("PUSH 5");
        calculator.processCommand("-");
    }

    @Test
    public void testMultiply() throws CalculatorException {
        calculator.processCommand("PUSH 3");
        calculator.processCommand("PUSH 4");
        calculator.processCommand("*");
        Assert.assertEquals(calculator.getStack().peek(), 12.0);
    }

    @Test
    public void testDivide() throws CalculatorException {
        calculator.processCommand("PUSH 12");
        calculator.processCommand("PUSH 4");
        calculator.processCommand("/");
        Assert.assertEquals(calculator.getStack().peek(), 3.0);
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testDivideByZero() throws CalculatorException {
        calculator.processCommand("PUSH 10");
        calculator.processCommand("PUSH 0");
        calculator.processCommand("/");
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testInvalidCommand() throws CalculatorException {
        calculator.processCommand("INVALID");
    }

    @Test
    public void testEq1() throws CalculatorException {
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("EQUATION 3 2");
        Assert.assertEquals(calculator.getStack().peek(), 14.0);
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testEqBigExp() throws CalculatorException {
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("EQUATION 100 2");
    }
    @Test(expectedExceptions = CalculatorException.class)
    public void testEqLessArgs() throws CalculatorException {
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("EQUATION 2");
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testEqInvalidExp() throws CalculatorException {
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("EQUATION ss 2");
    }

    @Test(expectedExceptions = CalculatorException.class)
    public void testEqLessStack() throws CalculatorException {
        calculator.processCommand("PUSH 1");
        calculator.processCommand("PUSH 1");
        calculator.processCommand("EQUATION 3 2");
    }
}
