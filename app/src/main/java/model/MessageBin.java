package model;

import java.util.ArrayList;

public class MessageBin {
    // a class that holds messages of a certain recipient
    private String recipient;
    private final String id;
    private final ArrayList<Message> messages;

    public MessageBin(String recipient, String id){
        this.recipient = recipient;
        messages = new ArrayList<>();
        this.id = id;
    }

    public String getId() { return id; }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public ArrayList<Message> getMessages(){
        return messages;
    }

    public void deleteMessage(Message msg){
        messages.remove(msg);
    }

    public void addMessage(Message msg){
        int index = messages.size() - 1;
        while (index > -1){
            if(msg.compareTo(messages.get(index)) > 0){ break; }
            index--;
        }
        messages.add(index+1, msg);
    }

    public Message getTopMessage(){
        if(messages.size() == 0){ return null; }
        return messages.get(messages.size() - 1);
    }
}
