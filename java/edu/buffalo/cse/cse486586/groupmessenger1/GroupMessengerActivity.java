package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS= {"11108", "11112", "11116","11120","11124"};
    //static final String REMOTE_PORT0 = "11108";
    //static final String REMOTE_PORT1 = "11112";
    //static final String REMOTE_PORT2 = "11116";
    //static final String REMOTE_PORT3 = "11120";
    //static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    int count=0;
    //private final ContentValues[] mContentValues;
     ContentResolver mContentResolver;
     TextView mTextView;



    private final Uri mUri= buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         */



        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){

                        String msg = editText.getText().toString() + "\n";
                        editText.setText(""); // This is one way to reset the input box.
                        TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                        localTextView.append("\t" + msg); // This is one way to display a string.
                        TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                        remoteTextView.append("\n");
                       // mContentResolver= getContentResolver();
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

                    }});

    }






    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            /* Below logic is taken from http://docs.oracle.com/javase/tutorial/networking/sockets/index.html and
            http://developer.android.com/reference/android/os/AsyncTask.html */
            try {


                while(true) {
                    Socket socketS = serverSocket.accept();
                    BufferedReader brReader = new BufferedReader(new InputStreamReader(socketS.getInputStream()));
                    String message;

                    while ((message = brReader.readLine()) != null) {
                        if (message != null) {

                            publishProgress(message);
                        }

                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            ContentValues[] cv = new ContentValues[1];
            cv[0] = new ContentValues();
            cv[0].put("key", Integer.toString(count));
            cv[0].put("value", strReceived);
            getContentResolver().insert(mUri, cv[0]);
            count++;
//            String filename = "SimpleMessengerOutput";
//            String string = strReceived + "\n";
//            FileOutputStream outputStream;
//
//            try {
//                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//                outputStream.write(string.getBytes());
//                outputStream.close();
//            } catch (Exception e) {
//                Log.e(TAG, "File write failed");
//            }

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
//                String remotePort = REMOTE_PORT0;
//                if (msgs[1].equals(REMOTE_PORT0))
//                    remotePort = REMOTE_PORT1;
                for(int i=0; i<5; i++){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));
                    String msgToSend = msgs[0];
                    PrintWriter printOnServer= new PrintWriter(socket.getOutputStream());
                    printOnServer.write(msgToSend);
                    printOnServer.close();
                    socket.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


}
