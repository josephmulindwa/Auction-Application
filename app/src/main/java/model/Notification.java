package model;

public class Notification {
    public enum KEY{ BUY, CONTACT, FAIL, NONE };
    public String id;
    public String key;
    public String target;
    public String title;
    public String value;
    public boolean seen;
    public long startTimeStamp;
    public long endTimeStamp;

    public Notification(){}

    public Notification(String id, KEY key, String target, String title, String value){
        this.id = id;
        this.key = key.toString();
        this.title = title;
        this.value = value;
        this.target = target;
        this.seen = false;
    }
}
