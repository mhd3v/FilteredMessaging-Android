package mhd3v.filteredsms;

import java.util.ArrayList;

/**
 * Created by Mahad on 12/10/2017.
 */

public class sms {

    String sender;

    ArrayList<messages> messages = new ArrayList<>();


    ArrayList<messages> userMessages = new ArrayList<>();


    sms(String sender, String message, String time){

        this.sender = sender;

        messages m = new messages(message, time);

        messages.add(m);

    }

    void addNew(String message, String time){
        messages m = new messages(message, time);
        messages.add(m);
    }

    void addNewUserMessage(String message, String time){
        messages m = new messages(message, time);
        userMessages.add(m);
    }

}

class messages{

    String messageBody;
    String time;


    messages(String messageBody, String time){
        this.messageBody = messageBody;
        this.time = time;
    }

}
