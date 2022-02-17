package model;

import java.util.ArrayList;
import java.util.HashMap;

public class Profile {
    private String fname;
    private String lname;
    private String email;
    private String key;
    private String password; // use hashed
    private String telephone;
    public String imagePath;
    private ArrayList<String> uploads;
    private HashMap<String, Integer> bids;

    public Profile(){;}

    public Profile(String fname, String lname, String email, String password, String telephone){
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.password = password;
        this.telephone = telephone;
        uploads = new ArrayList<>();
        bids = new HashMap<>();
        imagePath = null;
    }

    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getFName() {
        return fname;
    }
    public String getLName() {
        return lname;
    }
    public String getName(){ return fname + " " + lname; }
    public String getId(){ return email.replace('.', '@'); }
    public String getTelephone(){ return telephone; }
    public HashMap<String, Integer> getBids(){ return bids; }
    public ArrayList<String> getUploads(){ return uploads; }
    public void addUpload(String id){
        if(uploads == null){
            uploads = new ArrayList<>();
        }
        uploads.add(id);
    }
    public void removeUpload(String itemId){
        if(uploads == null){ return; }
        uploads.remove(itemId);
    }

    public void setEmail(String mEmail) {
        this.email = mEmail; // use EmailUtils
    }
    public void setPassword(String mPassword) {
        this.password = mPassword;
    }
    public void setFName(String mName) {
        this.fname = mName;
    }
    public void setLName(String mName) {
        this.lname = mName;
    }
    public void setTelephone(String telephone){ this.telephone = telephone; }
    public void setBids(HashMap<String, Integer> bids) { this.bids = bids; }
    public void addBid(String itemId, int bid){
        if(bids == null){
            bids = new HashMap<>();
        }
        bids.put(itemId, bid);
    }
    public Integer getBidForItem(String itemId){
        return bids.get(itemId);
    }
    public void setUploads(ArrayList<String> uploads){ this.uploads = uploads; }

}
