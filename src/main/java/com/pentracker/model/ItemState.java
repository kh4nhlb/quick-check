package com.pentracker.model;

public class ItemState {
    private boolean done;
    private String note;

    public ItemState() { this.note = ""; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public String getNote() { return note != null ? note : ""; }
    public void setNote(String note) { this.note = note; }
}
