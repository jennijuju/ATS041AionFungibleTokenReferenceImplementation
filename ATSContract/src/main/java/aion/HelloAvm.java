package aion;

import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;

public class HelloAvm
{
    private static String myStr = "Hello AVM";

    @Callable
    public static void sayHello() {
        Blockchain.println("Hello Avm");
    }

    @Callable
    public static String greet(String name) {
        return "Hello " + name;
    }

    @Callable
    public static String getString() {
        Blockchain.println("Current string is " + myStr);
        return myStr;
    }

    @Callable
    public static void setString(String newStr) {
        myStr = newStr;
        Blockchain.println("New string is " + myStr);
    }

}
