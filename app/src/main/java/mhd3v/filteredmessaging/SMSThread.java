package mhd3v.filteredmessaging;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Mahad on 12/10/2017.
 */

public class SMSThread {

    String threadId;

    int blacklisted = 0;
    int read = 0;

    String sender;
    String senderName;

    ArrayList<Message> messageList = new ArrayList<>();

    SMSThread(String sender, String threadId){
        this.sender = sender;
        this.threadId = threadId;
    }

    void addNewSenderMessage(String message, String time){
        Message m = new Message(message, time);
        messageList.add(m);
    }

    void addNewUserMessage(String message, String time){
        Message m = new Message(message, time);
        m.isUserMessage = true;
        messageList.add(m);
    }

}

class Message implements Serializable{

    boolean isUserMessage = false;
    String messageBody;
    String time;
    boolean failed = false;
    boolean sending = false;

    Message(String messageBody, String time){
        this.messageBody = messageBody;
        this.time = time;
    }

}
