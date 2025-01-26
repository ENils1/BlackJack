package no.blackjack.handlers;

import java.util.Random;

public class NumberUtil {

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int getRandomNumber(int min, int max) { //Inclusive min, exclusive max - 10-100? -> getRandomNumber(10-101);
        Random r = new Random();
        return (r.nextInt(max - min) + min);
    }
}
