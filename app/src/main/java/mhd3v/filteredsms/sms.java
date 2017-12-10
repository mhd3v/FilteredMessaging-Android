package mhd3v.filteredsms;

import java.util.ArrayList;

/**
 * Created by Mahad on 12/10/2017.
 */

public class sms {

    String sender;

    ArrayList<String> messages = new ArrayList<>();


    ArrayList<String> userMessages = new ArrayList<>();


    sms(String sender, String message){

        this.sender = sender;

        this.messages.add(message);

    }

    void addNew(String message){

        this.messages.add(message);
    }

    void addNewUserMessage(String message){
        userMessages.add(message);
    }

}
