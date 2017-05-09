package com.example.nuttun.mbprogchat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ObjectInputStream;
import java.util.HashMap;

/**
 * Created by Nuttun on 09/05/2017.
 */

public class updateDbTask extends AsyncTask<Object, Void, Void> {

    @Override
    protected Void doInBackground(Object... objects) {
        //Read db, get lasted seqno from db
        String lasted_seqno = "1";
        MessageDbHelper dbHelper = new MessageDbHelper((Context) objects[0]);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {MessageContract.MessageEntry.COLUMN_NAME_SEQNO};
        Cursor cursor = db.query(MessageContract.MessageEntry.TABLE_NAME,
                projection,
                null,null,null,null,
                MessageContract.MessageEntry.COLUMN_NAME_SEQNO + " DESC"
        );
        String lasted_seqno_in_db = "1";
        if(cursor.moveToNext()) {
            lasted_seqno = cursor.getString(0);
            lasted_seqno_in_db = lasted_seqno;
        }
        cursor.close();

        //Load all message from API
        HTTPHelper helper = new HTTPHelper();
        HashMap<String,String> hm = new HashMap<>();
        hm.put("sessionid",objects[1].toString().trim());
        JSONArray messageToBeUpdate = new JSONArray();
        while (true){
            hm.put("seqno",lasted_seqno);
            String result = helper.POST("https://mis.cp.eng.chula.ac.th/mobile/service.php?q=api/getMessage",hm);
            try {
                JSONObject jsonObject = new JSONObject(result);
                String type = jsonObject.getString("type");
                if(type.equalsIgnoreCase("error")){
                    Toast.makeText((Context) objects[0],"Error, try re-login",Toast.LENGTH_SHORT).show();
                    return null;
                }
                JSONArray content = jsonObject.getJSONArray("content");
                if(content.length()==0) break;
                lasted_seqno = content.getJSONObject(content.length()-1).getString("seqno");
                if(Integer.parseInt(lasted_seqno) > Integer.parseInt(lasted_seqno_in_db)){
                    for(int i=0; i<content.length(); i++){
                        int k = Integer.parseInt(content.getJSONObject(i).getString("seqno"));
                        if(k <= Integer.parseInt(lasted_seqno_in_db)) continue;
                        messageToBeUpdate.put(content.getJSONObject(i));
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        //Update db
        db = dbHelper.getWritableDatabase();
        for(int i=0; i<messageToBeUpdate.length(); i++){
            String seqno = null;
            String datetime = null;
            String from = null;
            String to = null;
            String message = null;
            try {
                seqno = messageToBeUpdate.getJSONObject(i).getString("seqno");
                datetime = messageToBeUpdate.getJSONObject(i).getString("datetime");
                from = messageToBeUpdate.getJSONObject(i).getString("from");
                to = messageToBeUpdate.getJSONObject(i).getString("to");
                message = messageToBeUpdate.getJSONObject(i).getString("message");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ContentValues values = new ContentValues();
            values.put(MessageContract.MessageEntry.COLUMN_NAME_SEQNO, seqno);
            values.put(MessageContract.MessageEntry.COLUMN_NAME_DATETIME, datetime);
            values.put(MessageContract.MessageEntry.COLUMN_NAME_FROM, from);
            values.put(MessageContract.MessageEntry.COLUMN_NAME_TO, to);
            values.put(MessageContract.MessageEntry.COLUMN_NAME_MESSAGE, message);
            db.insert(MessageContract.MessageEntry.TABLE_NAME, null, values);
        }

        return null;
    }
}