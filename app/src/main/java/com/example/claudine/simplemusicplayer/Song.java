package com.example.claudine.simplemusicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Song implements Serializable{
    private long id;
    private String title;
    private String artist;
    private ArrayList<String> tagList = new ArrayList<String>();

    public Song(long songID, String songTitle, String songArtist) {
        id = songID;
        title = songTitle;
        artist = songArtist;
    }

    public long getID(){return id;}
    public String getTitle() {return title;}
    public String getArtist() {return artist;}
    public ArrayList<String> getTagList() {return tagList;}

}