package com.example.claudine.simplemusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.example.claudine.simplemusicplayer.Song;
import java.util.ArrayList;

public class SetTags extends AppCompatActivity {
    private ArrayList<Song> songList;
    private ArrayList<String> beaconIds;
    private int index;
    Intent setTags;
    TextView songName;
    private ListView curTagView;
    private ListView newTagView;
    ArrayAdapter<String> curTagAdapter;
    ArrayAdapter<String> newTagAdapter;
    Button saveTagsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settags); //Sets the view based on activity_main

        songName = findViewById(R.id.song_title);
        setTags = getIntent();
        songList = (ArrayList<Song>) setTags.getSerializableExtra("songList");
        beaconIds = (ArrayList<String>) setTags.getSerializableExtra("beaconIds");
        index = setTags.getIntExtra("index",0);
        songName.setText(songList.get(index).getTitle());
        curTagView = findViewById(R.id.curTagView);
        newTagView = findViewById(R.id.newTagView);
        curTagAdapter = new ArrayAdapter<String>(this, R.layout.tagview, songList.get(index).getTagList());
        newTagAdapter = new ArrayAdapter<String>(this, R.layout.tagview, beaconIds);
        curTagView.setAdapter(curTagAdapter);
        newTagView.setAdapter(newTagAdapter);

        saveTagsButton = (Button)findViewById(R.id.saveTagButton);

        newTagView.setClickable(true);
        newTagView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                int tagIndex = position;
                songList.get(index).getTagList().add(beaconIds.get(tagIndex));
                curTagAdapter.notifyDataSetChanged();
            }
        });

        curTagView.setClickable(true);
        curTagView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                int tagIndex = position;
                songList.get(index).getTagList().remove(tagIndex);
                curTagAdapter.notifyDataSetChanged();
            }
        });

        saveTagsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent returnIntent = new Intent();
                returnIntent.putExtra("songList", songList);
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });

    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
