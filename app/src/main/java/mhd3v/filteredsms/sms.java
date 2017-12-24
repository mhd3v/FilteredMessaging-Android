package mhd3v.filteredsms;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Mahad on 12/10/2017.
 */

public class sms{

    String threadId;

    String sender;

    String senderName;

    messages lastMessage;

    ArrayList<messages> messages = new ArrayList<>();


    sms(String sender, String threadId){

        this.sender = sender;
        this.threadId = threadId;

    }

    void addNewSenderMessage(String message, String time){
        messages m = new messages(message, time);
        messages.add(m);
    }

    void addNewUserMessage(String message, String time){
        messages m = new messages(message, time);
        lastMessage = m;
        m.isUserMessage = true;
        messages.add(m);
    }




}

class messages implements Serializable{

    boolean isUserMessage = false;
    String messageBody;
    String time;


    messages(String messageBody, String time){
        this.messageBody = messageBody;
        this.time = time;
    }

}
