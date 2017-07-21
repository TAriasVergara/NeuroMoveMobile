package com.example.tomas.speechprocessingapp;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class RecordingsList extends AppCompatActivity {
    File[] FilesRec = null;
    ListView listView;
    int pos =0; //Position of selected wav file

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings_list);
        Intent intent = getIntent();
        String WAVspath = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        //intentDR = new Intent(this, DisplayResults.class);
        File pathRec = new File(WAVspath);
        FilesRec = pathRec.listFiles();

        listView = (ListView) findViewById(R.id.listView);
        final List<String> fname = new ArrayList<String>();
        for (int i = 0; i < FilesRec.length; i++) {
            fname.add(FilesRec[i].getName());
        }
        final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, fname);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                pos = position;
                dresult();
/*                view.animate().setDuration(2000).alpha(0).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                //fname.remove(item);
                                adapter.notifyDataSetChanged();
                                view.setAlpha(1);
                            }
                        });*/
            }

        });
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    private void dresult(){
        //Get selected wav file
        String SelPathFile = FilesRec[pos].getPath();
        Intent intent = new Intent(this,DisplayResults.class);
        intent.putExtra(EXTRA_MESSAGE,SelPathFile);
        startActivity(intent);
    }
}
