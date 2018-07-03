package mhd3v.filteredmessaging;

public class Filteration {

    public static boolean checkMessage(String message){

        message = message.toLowerCase();

        if(message.contains("username"))
            return true;

        else if(message.contains("sale") || message.contains("deal") || message.contains("% off") || message.contains("offer"))  //negative keywords
            return false;

        else if(message.contains("your") && ( message.contains("code") || message.contains("order") || message.contains("password"))) //positive keywords
            return true;

        else if(message.length() <= 135)
            return true;

        else
            return false;
    }

}
