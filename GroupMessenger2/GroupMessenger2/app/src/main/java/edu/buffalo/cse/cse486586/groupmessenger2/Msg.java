package edu.buffalo.cse.cse486586.groupmessenger2;

public class Msg{
    private int type;
    private int id;
    private double uniqueID;
    private String msg;
    private boolean agree = false;
    public long recievedTime = 0;
    public boolean resent = false;


    public Msg(int type, int id, double uniqueID, String msg) {
        this.type = type;
        this.id = id;
        this.uniqueID = uniqueID;
        this.msg = msg;
    }

    public Msg(String everything) {
        String parts[] = everything.split(" ");
        this.type = Integer.parseInt(parts[0]);
        this.id = Integer.parseInt(parts[1]);
        this.uniqueID = Double.parseDouble(parts[2]);
        this.msg = parts[3];
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUniqueID(double uniqueID) {
        this.uniqueID = uniqueID;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public double getUniqueID() {
        return uniqueID;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return Integer.toString(type) + " " +
                Integer.toString(id) + " " +
                Double.toString(uniqueID) + " " +
                msg ;
    }

    public void agreement(){
        this.agree = true;
    }
    public boolean isAgreed(){
        return this.agree;
    }
//    @Override
//    public int compareTo(Msg another) {
//        return new Double(this.id).compareTo(another.id);
//    }
    @Override
    public boolean equals(Object o){
        Msg a = (Msg) o;
        return a.getMsg().equals(this.msg);
    }
}
