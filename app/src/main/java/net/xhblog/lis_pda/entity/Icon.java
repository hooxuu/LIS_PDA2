package net.xhblog.lis_pda.entity;

public class Icon {
    private int id;
    private String iname;

    public Icon() {
    }

    public Icon(int id, String iname) {
        this.id = id;
        this.iname = iname;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIname() {
        return iname;
    }

    public void setIname(String iname) {
        this.iname = iname;
    }
}
