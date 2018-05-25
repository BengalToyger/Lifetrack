package com.example.claudine.simplemusicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.claudine.simplemusicplayer.MusicService.MusicBinder;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class LifetrackMain extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "LifetrackMain";
    private BeaconManager beaconManager;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int UPDATE_TAGS_REQUEST = 111;

    private ArrayList<Song> songList; //List of our song class for references songs
    private ArrayList<String> beaconIds = new ArrayList<String>();
    private ListView songView; //List in the View that displays the songs
    private MusicService musicSrv; //Service that handles playing music
    private Intent playIntent; //Starts and gives data to music service class
    private boolean musicBound=false; //Tells us if service is bound
    private String currentPlayingTag = "none";
    private String previousTag = "none";
    private TextView playingSongDisp;
    private TextView closestBeaconDisp;
    private String closestBeaconID = "No Beacons in Range";
    private String currentPlayingSong = "None";

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(LifetrackMain.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu); //Populates menu
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //Sets the view based on activity_main

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
            }
        }
        //Gets the text views for the playing song and closest beacon
        playingSongDisp = findViewById(R.id.playingSong);
        closestBeaconDisp = findViewById(R.id.closestBeacon);

        songView = (ListView)findViewById(R.id.song_list); //Finds the song view in
        //the view and puts ID in variable
        songList = new ArrayList<Song>(); //Instantiates the song list
        getSongList();
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList); //Adapts song list into the list view for display
        songView.setAdapter(songAdt); //Links list and songs
        Toolbar myToolbar = findViewById(R.id.my_toolbar); //Creates toolbar
        setSupportActionBar(myToolbar);
        checkLocationPermission();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.bind(this);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound=false; //unbinds when service disconnected
        }
    };

    //Initializes play intent and start music service
    @Override
    protected void onStart(){
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this,MusicService.class);
            bindService(playIntent,musicConnection,Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    //Starts the SetTags activity for a particular song when clicked
    public void songPicked(View view){
        Intent setTags;
        //Gets the index of the song clicked
        int index = Integer.parseInt(view.getTag().toString());
        //Sets up the intent to start SetTags
        setTags = new Intent(this, SetTags.class);
        //Adds the data to the intent
        setTags.putExtra("songList",songList);
        setTags.putExtra("index",index);
        setTags.putExtra("beaconIds",beaconIds);
        //Starts the activity, with the expectation of getting data returned
        startActivityForResult(setTags,UPDATE_TAGS_REQUEST);
    }

    //Gets the songlist passed back from the closed SetTags activity and sets it
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == UPDATE_TAGS_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                songList = (ArrayList)intent.getSerializableExtra("songList");
            }
        }
    }

    //handles clicks on menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //stops service when app is destroyed
    @Override
    protected void onDestroy(){
        stopService(playIntent);
        musicSrv = null;
        beaconManager.unbind(this);
        super.onDestroy();
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver(); //Let's us find music
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Cursor that iterates through the resolved music URIs?
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Beacon closestBeacon = null;
                boolean match = false;
                beaconIds.clear();
                double minDistance = 100;
                int songIndex = 0;
                Log.i(TAG,"Beacons Detected: " + beacons.size());
                //Look through all the beacons and find the closest beacon
                for (Beacon curBeacon : beacons) {
                    beaconIds.add(curBeacon.getId1().toString());
                        if (curBeacon.getDistance() < minDistance) {
                            minDistance = curBeacon.getDistance();
                            closestBeacon = curBeacon;
                        }
                    Log.i(TAG, "Beacon " + curBeacon.getId1().toString() + " is about " + curBeacon.getDistance() + " away.");
                }
                //If there are beacons
                if (!beaconIds.isEmpty()) {
                    //Get the ID of the closest beacon
                    closestBeaconID = closestBeacon.getId1().toString();
                    //Look through all the songs
                    for (Song curSong : songList){
                        //Look through all the tags in a song
                        for (String curTag : curSong.getTagList()) {
                            //If a tag in the tag list matches
                            if (curTag.equals(closestBeacon.getId1().toString())) {
                                //Set the songIndex to the index of this song, and the currentPlayingTag to this tag
                                songIndex = songList.indexOf(curSong);
                                currentPlayingTag = closestBeacon.getId1().toString();
                                match = true;
                            }
                        }
                        //Results in the last song with a matching tag being the songIndex
                    }
                    //Update the playing song if there is a matched tag and either the song is different or not playing
                    if ((!currentPlayingTag.equals(previousTag) || !musicSrv.isPlaying()) && match){
                        Log.i(TAG, "Playing something new!");
                        musicSrv.setSong(songIndex);
                        musicSrv.playSong();
                        previousTag = currentPlayingTag;
                        currentPlayingSong = songList.get(songIndex).getTitle();
                    }
                    Log.i(TAG, "currentPlayingTag: " + currentPlayingTag);
                    Log.i(TAG, "previousTag: " + previousTag);
                    Log.i(TAG, "songIndex: " + songIndex);
                } else {
                        closestBeaconID = "No Beacons in Range";
                        currentPlayingSong = "None";
                        //Pauses song if there are no beacons in range
                        if (musicSrv.isPlaying()) {
                            musicSrv.pauseSong();
                        }
                }
                //Updates the UI based on new info
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playingSongDisp.setText(currentPlayingSong);
                        closestBeaconDisp.setText(closestBeaconID);
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}


