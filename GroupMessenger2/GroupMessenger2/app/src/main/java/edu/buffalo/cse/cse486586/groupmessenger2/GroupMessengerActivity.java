package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author syed rehman
 *
 */
public class GroupMessengerActivity extends Activity {
    static final Integer ACTUAL_MSG = 0;
    static final Integer ACK = 1;
    static final Integer AGREEMENT = 2;
    static final Integer LOST = 3;
    static final Integer FOUND = 4;
    static final Integer NULLACK = 5;
    static final String RESEND = "RESEND";
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT= {"11108","11112", "11116", "11120" , "11124"};
    static final int SERVER_PORT = 10000;
    static final Uri getUri =
            Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");

    static Integer activeNodes[] = new Integer[5];
    static Object mutext = new Object();
    static double seq = 0;
    static Integer my_pid = -1;
    static Integer key = 0;
    static double maxAckSeq = 0;
    static Object seq_mutex = new Object();
    static PriorityQueue<Msg> pq = new PriorityQueue<Msg>(5, new Comparator<Msg>(){
        @Override
        public int compare(Msg a, Msg b) {
            return Double.compare(a.getUniqueID(), b.getUniqueID());
        }
    });
    static Map<String, Msg> storage = new HashMap<String, Msg>();
    static long lastAgreement = System.currentTimeMillis();
    static Map<String, Msg> finalStorage = new HashMap<String, Msg>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        for(int i =0; i< 5; i++){
            activeNodes[i] = 1;
            if(REMOTE_PORT[i].contains(myPort)){
                my_pid = i;
            }
        }
        Log.e("My pid -----", my_pid.toString());

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try{
            final ServerSocket server = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);

        }catch(IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final Button send_button = (Button) findViewById(R.id.button4);
        send_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.editText1);
                String msgToSend = et.getText().toString();
                et.setText("");
                if(msgToSend != null){
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, myPort);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{ //<param, progress, result>
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            final ServerSocket server = serverSockets[0];
            lastAgreement = System.currentTimeMillis();


            while(true){
                try {
                    server.setSoTimeout(5000);
                    Socket client = server.accept();
                    InputStreamReader stream = new InputStreamReader(client.getInputStream());
                    BufferedReader reader = new BufferedReader(stream);
                    String msg = reader.readLine();
                    if (msg != null) {

                        msg = msg.trim();
                        Log.e("recieved+++", msg);
                        Msg rec = new Msg(msg);
                        rec.recievedTime = System.currentTimeMillis();
                        switch (rec.getType()) {
                            case 0: //this means regular msg
                                pq.add(rec);
                                storage.put(rec.getMsg(), rec);
                                synchronized (seq_mutex) {
                                    if (rec.getUniqueID() > seq) {
                                        int a = (int) rec.getUniqueID();
                                        seq = a + 1;
                                    } else {
                                        seq = (int) seq;
                                        seq += 1;
                                    }
                                }
                                String ack_msg = ACK.toString() + " " + my_pid.toString() + " " + new Double(createId(seq)).toString() + " " + rec.getMsg();
                                send(client, ack_msg);
                                synchronized (this){
                                    wait(50);
                                }
                                break;
                            case 5:
                                if(storage.containsKey(rec.getMsg())){
                                    String ack2 = ACK.toString() + " " + my_pid.toString() + " " + new Double(storage.get(rec.getMsg()).getUniqueID()).toString() + " " + rec.getMsg();
                                    send(client, ack2);
                                    synchronized (this){
                                        wait(50);
                                    }
                                }else{
                                    pq.add(rec);
                                    storage.put(rec.getMsg(), rec);
                                    synchronized (seq_mutex) {
                                        if (rec.getUniqueID() > seq) {
                                            int a = (int) rec.getUniqueID();
                                            seq = a + 1;
                                        } else {
                                            seq = (int) seq;
                                            seq += 1;
                                        }
                                    }
                                    String ack3 = ACK.toString() + " " + my_pid.toString() + " " + new Double(createId(seq)).toString() + " " + rec.getMsg();
                                    send(client, ack3);
                                    synchronized (this){
                                        wait(50);
                                    }
                                }
                                break;
                            case 2:  //this means sender is declaring the final seq.
                                synchronized (seq_mutex) {
                                    if (rec.getUniqueID() > seq) {
                                        int a = (int) rec.getUniqueID();
                                        seq = a + 1;
                                    }
                                }
                                Log.e("agreement %% ", rec.toString());
                                pq.remove(storage.remove(rec.getMsg()));
                                rec.agreement();
                                pq.add(rec);
                                Log.e("peek element in pq", pq.peek().toString());
                                handleInsert();
                                break;
                            case 3:  //lost
                                Msg m2 = finalStorage.get(rec.getMsg());
                                if(m2!= null){
                                    String re = FOUND.toString() + " " + my_pid.toString() + " " + new Double(m2.getUniqueID()).toString() + " " + rec.getMsg();
                                    Socket s = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[rec.getId()]));
                                    send(s, re);
                                }
                                break;
                            case 4:  //found
                                if(!finalStorage.containsKey(rec.getMsg())){
                                    rec.resent = true;
                                    if(pq.peek().isAgreed()){
                                        cashMsg(key, pq.poll());
                                        key++;
                                    }
                                }
                                break;
                        }

                    }else{
                        Log.e("<-----server null", "YYYYYYYYYYYYYYY");
                        send(client, RESEND);
                        handleDrop();
                    }
                }catch (SocketTimeoutException e){
                    handleDrop();
                    Log.e("<-----Server timeout", "XXXXXXXXXXXXX");

                }catch (IOException e){
                    Log.e(TAG, "Server cant listen on the port");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private boolean handleInsert(){
        while (!pq.isEmpty()) {
            if (pq.peek().isAgreed()) {
                cashMsg(key, pq.poll());
                key++;
            } else {
                handleDrop();
                break;
            }
        }
        return false;
    }

    private boolean handleDrop(){
        synchronized (mutext){
            if(pq.isEmpty()){
                return false;
            }
            if(pq.peek().isAgreed()){
                while (!pq.isEmpty()) {
                    if (pq.peek().isAgreed()) {
                        cashMsg(key, pq.poll());
                        key++;
                    } else {
                        break;
                    }
                }
            }else {
                ArrayList<Msg> rmItems = new ArrayList<Msg>();
                ArrayList<Msg> reSend = new ArrayList<Msg>();

                for(Msg m: pq){
                    long now = System.currentTimeMillis();
                    if(!m.isAgreed() && (now - m.recievedTime) > 3000){
                        if(activeNodes[m.getId()] == 0){
                            rmItems.add(m);
                            continue;
                        }
                        if(m.resent){
                            rmItems.add(m);
                        }else{
                            reSend.add(m);
                        }
                    }
                }
                for(Msg m : reSend){
                    ackMissingAgreement(m);
                }
                for(Msg m : rmItems){
                    Log.e("removing", m.toString());
                    pq.remove(m);
                }
            }
        }
        return true;
    }

    private void ackMissingAgreement(Msg msg_){
        Log.e("AAAASSSSSKKKK ", msg_.toString());
        ArrayList<Socket> openedSocket = new ArrayList<Socket>();
        String re = LOST.toString() + " " + my_pid.toString() + " " + new Double(msg_.getUniqueID()).toString() + " " + msg_.getMsg();
        try {
            for (Integer node = 0; node < activeNodes.length; node++) {
                if (activeNodes[node] == 0) {
                    continue;
                }
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[node]));
                String rec = null;
                send(socket, re);
                openedSocket.add(socket);
            }
            synchronized (this){
                wait(35);
            }
            for(Socket o : openedSocket){
                if(!o.isClosed()){
                    o.close();
                }
            }
        }catch(IOException e) {
            Log.e("request task :", e.getStackTrace().toString());
        } catch (InterruptedException e) {
            Log.e("request task :", e.getStackTrace().toString());
        }
    }

    private boolean cashMsg(final Integer k , final Msg sendMsg){
        finalStorage.put(sendMsg.getMsg(), sendMsg);
        final String msg = sendMsg.getMsg();
        final String tr_ = sendMsg.toString();
        Uri uri = null;
        ContentValues newValues = new ContentValues();
        newValues.put("key", k);
        newValues.put("value", msg);
        uri = getContentResolver().insert(getUri, newValues);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView tv = (TextView) findViewById(R.id.textView1);
                tv.append( (k).toString() + "= " + sendMsg.getMsg() + "\t\n");
                //gmp.insert(“content://edu.buffalo.cse.cse486586.groupmessenger1.provider”, )
            }
        });
        return true;
    }

//Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
    private class ClientTask extends AsyncTask<String, Void, Void>{ //<param, progress, result>
        @Override
        protected Void doInBackground(String... msgs) {
            sendMsg(msgs[0]);
            return null;
        }
    }


    private void sendMsg(String msg){
        Double seqNow;
        ArrayList<Socket> openedSocket = new ArrayList<Socket>();
        synchronized (seq_mutex){
            seqNow = createId(seq);
        }
        String msgToSend = ACTUAL_MSG.toString() + " " + my_pid.toString() + " "+ seqNow.toString() + " " + msg;
        try {
            for (Integer node = 0; node < activeNodes.length; node++) {
                if(activeNodes[node] == 0){
                    continue;
                }
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[node]));
                String rec = null;
                try {
                    socket.setSoTimeout(1500);
                    send(socket, msgToSend);    //send msg to node
                    InputStreamReader stream = new InputStreamReader(socket.getInputStream());
                    BufferedReader reader = new BufferedReader(stream);
                    rec = reader.readLine();
                    Log.e("client sending", msgToSend);
                    openedSocket.add(socket);
                }catch (SocketTimeoutException e){
                    Log.e("-----client timeout", " --------->" + node.toString());
                    activeNodes[node] = 0;
                    continue;
                }
                if(rec != null) {
                    if(rec.equals(RESEND)){
                        node--;
                        Log.e(RESEND, "to ||||||||||||||||||||||||||||||||||||||||||||||||||||||||"+node.toString());

                        continue;
                    }
                    Msg m = new Msg(rec);
                    if (m.getUniqueID() > maxAckSeq) {  //maxAckSeq = highest seq received
                        maxAckSeq = m.getUniqueID();
                    }
                }else{
                    Log.e("-----null ack", " --------->" + node.toString());
                    socket.close();
                    try {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[node]));
                        socket.setSoTimeout(1500);
                        msgToSend = NULLACK.toString() + " " + my_pid.toString() + " " + seqNow.toString() + " " + msg;
                        send(socket, msgToSend);    //send msg to node
                        InputStreamReader stream = new InputStreamReader(socket.getInputStream());
                        BufferedReader reader = new BufferedReader(stream);
                        rec = reader.readLine();
                        Log.e("client sending", msgToSend);
                    }catch (SocketTimeoutException e) {
                        Log.e("-----client timeout", " --------->" + node.toString());
                        activeNodes[node] = 0;
                        continue;
                    }
                    if(rec == null){
                        activeNodes[node] = 0;
                    }else{
                        Msg m = new Msg(rec);
                        if (m.getUniqueID() > maxAckSeq) {  //maxAckSeq = highest seq received
                            maxAckSeq = m.getUniqueID();
                        }
                    }
                }
            }
            for(Socket e: openedSocket){
                if(!e.isClosed()){
                    e.close();
                }
            }
            Log.e("-----send --->", " --------->" + msgToSend);
            synchronized (seq_mutex) {
                seq = (int) maxAckSeq;
                seq += 1;
            }
            synchronized (this){
                wait(60);
            }
            sendAgreement(msg);

        }catch(IOException e) {
            Log.e("Clinet task :", e.getStackTrace().toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void send(Socket socket, String msg){
        if(socket.isClosed()){
            Log.e("count not send: ", "socket is closed");
        }
        try{
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.println(msg);
            pw.flush();
        }catch (IOException e){
            Log.e("ACK : " , e.getStackTrace().toString());
        }
    }

    private void sendAgreement(String msg){
        String agr_msg = AGREEMENT.toString() +" " + my_pid.toString()+ " " +
                new Double(maxAckSeq).toString() + " " + msg;
        for (Integer i = 0; i< activeNodes.length; i++) {
            if(activeNodes[i] == 0){
                continue;
            }
            Socket socket;
            try {
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
                Log.e("sending arg to", i.toString());
                send(socket, agr_msg);    //send msg to node
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (this){
            try {
                wait(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private double createId(double sequence){
        double temp = (double)my_pid / 10;
        return sequence + temp;  // ex if seq = 3 and pid = 2, then id = 3.2
    }
}
