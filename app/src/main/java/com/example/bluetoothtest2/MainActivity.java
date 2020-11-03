package com.example.bluetoothtest2;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button button_client;
    private Button button_server;
    private ListView listView;
    private List list = new ArrayList();
    private ArrayAdapter<String> adapter;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> set;
    private BluetoothSocket clientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        button_client.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                set = bluetoothAdapter.getBondedDevices();

                list.clear();
                for (BluetoothDevice devices : set) {
                    list.add(devices.getName() + "+" + devices.getAddress());
                }
                adapter.notifyDataSetChanged();

            }
        });
    }

    private void initView() {
        button_client = (Button) findViewById(R.id.button_client);
        button_server = (Button) findViewById(R.id.button_server);
        listView = (ListView) findViewById(R.id.listview);

        button_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                (new AcceptThread()).start();
            }
        });

        adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                List<BluetoothDevice> list = new ArrayList<BluetoothDevice>(set);
                BluetoothDevice device = list.get(i);

                try {
                    clientSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(API.UUID));
                    clientSocket.connect();
                    if (clientSocket.isConnected() == true) {
                        Log.d("MainActivity", "客户端连接成功");
                    }
                    final InputStream inputStream = clientSocket.getInputStream();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                byte[] bytes = new byte[1024];
                                int n;
                                while ((n = inputStream.read(bytes)) != -1) {
                                    String s = new String(bytes, 0, n, "UTF-8");
                                    Log.d("MainActivity", "客户端接收到数据" + s);
                                }
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    //服务端线程
    private class AcceptThread extends Thread {

        BluetoothServerSocket serverSocket;
        private OutputStream outputStream;
        public AcceptThread() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("miangezuishuai", UUID.fromString(API.UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                BluetoothSocket socket = serverSocket.accept();
                if (socket.isConnected()) {
                    Log.d("MainActivity", "连接成功");
                }
                outputStream = socket.getOutputStream();
                sendSocketData("发送给客户端的数据");
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendSocketData(String message) {
            if (outputStream != null) {
                try {
                    //指定发送的数据已经数据编码，编码统一，不然会乱码
                    outputStream.write(message.getBytes("UTF-8"));
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
