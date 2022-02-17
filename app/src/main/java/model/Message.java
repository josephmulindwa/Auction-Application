package model;

import java.util.Date;

public class Message implements Comparable{
    private long timeStamp;
    private String content; // sortByTime
    private String sender;
    private boolean read;

    public Message(){ }

    @Override
    public int compareTo(Object o){
        Date date1 = new Date(this.timeStamp);
        long ts2 = ((Message) o).getTimeStamp();
        if(this.timeStamp == ts2){ return 0; }
        Date date2 = new Date(ts2);
        return date1.after(date2) ? 1 : -1;
    }

    public Message(String content) {
        this.content = content;
        timeStamp = new Date().getTime();
        sender = null;
        read = false;
    }

    public Message(String content, String sender) {
        this.content = content;
        timeStamp = new Date().getTime();
        this.sender = sender;
        read = false;
    }

    public boolean getRead(){ return read; }
    public void setRead(boolean read){ this.read = read; }

    public String getSender(){ return sender; }
    public void setSender(String sender){ this.sender = sender; }

    public long getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
    }

}
