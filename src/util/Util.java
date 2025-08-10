package util;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Util {
    public static Scanner intScanner = new Scanner(System.in);
    public static Scanner strScanner = new Scanner(System.in);

    public static int getNumber(String text) {
        while (true) {
            try {
                System.out.print(text + ": ");
                return intScanner.nextInt();
            }catch (InputMismatchException e) {
                System.out.println("Enter valid value");
                intScanner.nextLine();
            }
        }
    }

    public static String getText(String text) {
        System.out.print(text + ": ");
        return strScanner.nextLine();
    }
}
