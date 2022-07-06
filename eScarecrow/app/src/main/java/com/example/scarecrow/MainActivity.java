package com.example.scarecrow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.scarecrow.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {

    PreviewView previewView;
    private ImageCapture imageCapture;
    private ActivityMainBinding binding;
    private int rotacion;
    private MediaPlayer catSoundMediaPlayer;
    public final int numClases = 10;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener pantalla encendida


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
        }

        Toast.makeText(this,getResources().getString(R.string.rOrientation),Toast.LENGTH_SHORT).show();


        catSoundMediaPlayer = MediaPlayer.create(this, R.raw.cat_meow);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    public void selectItem(View view){
        switch(view.getId()){
            case(R.id.btnGuardarEspecies):
                guardarPreferencias();
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Display display = ((WindowManager)
                getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        handler.postDelayed( runnable = new Runnable() {
            public void run() {

                capturePhoto();

                handler.postDelayed(runnable, 15000);
            }
        }, 15000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    public void iniciarCamara(){
        previewView = findViewById(R.id.previewView);
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    @SuppressLint("RestrictedApi")
    public void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //bind to lifecycle:
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }


    public void guardarPreferencias(){
        SharedPreferences prefs = getSharedPreferences("especies", Context.MODE_PRIVATE);
        int[] elementos = {R.id.checkBox0, R.id.checkBox1, R.id.checkBox2,R.id.checkBox3,R.id.checkBox4,R.id.checkBox5,
                R.id.checkBox6,R.id.checkBox7,R.id.checkBox8,R.id.checkBox9};
        SharedPreferences.Editor editor = prefs.edit();
        for(int i = 0; i<numClases;i++){
            boolean isChecked = ((CheckBox) findViewById(elementos[i])).isChecked();
            editor.putBoolean(Integer.toString(i),isChecked);
        }

        editor.commit(); //Guardamos cambios

        Map<String, ?> allPrefs = prefs.getAll();
        Set<String> set = allPrefs.keySet();
        for(String s : set){ //Imprime las claves y sus valores
            Log.d("", s + "<" + allPrefs.get(s).getClass().getSimpleName() +"> =  "
                    + allPrefs.get(s).toString());
        }
    }

    public void capturePhoto() {


        imageCapture.takePicture(getExecutor(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(ImageProxy image) {
                ImageInfo info = image.getImageInfo();
                switch(info.getRotationDegrees()){ //Obtenemos rotación para luego mandarla al servidor
                    case 0:
                        rotacion = 0;
                        break;
                    case 90:
                        rotacion = 1;
                        break;
                    case 180:
                        rotacion = 2;
                        break;
                    case 270:
                        rotacion = 3;
                        break;
                }

                Log.d("testeo","Rotacion: "+info.getRotationDegrees());

                ByteBuffer bb = image.getPlanes()[0].getBuffer();
                byte[] buf = new byte[bb.remaining()];
                bb.get(buf);

                new ClientThread().execute(buf);
                image.close();
            }
        });
    }

    public class ClientThread extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... voids) {

            try {
                Log.d("testeo", "C: Connecting...");
                Socket s = new Socket("13.38.225.80", 8050);
                OutputStream out = s.getOutputStream();

                DataOutputStream dataOutputStream=new DataOutputStream(out);
                dataOutputStream.writeUTF(String.valueOf(rotacion));
                dataOutputStream.write(voids[0],0,voids[0].length);
                s.shutdownOutput();

                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String inputLine;
                inputLine = reader.readLine();
                Log.d("testeo", "message: "+inputLine);

                int respuesta = Integer.parseInt(inputLine);
                if(respuesta!=99){ //Pájaro detectado si != 99
                    SharedPreferences prefs = getSharedPreferences("especies", Context.MODE_PRIVATE);
                    if(prefs.getBoolean(inputLine, false)){ //Comprobamos que pájaro no esté permitido
                        catSoundMediaPlayer.start();
                        MainActivity.this.runOnUiThread(new Runnable() { //Enseñamos un mensaje indicando detección
                            public void run() {
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.detected),Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                reader.close();
                out.close();
                s.close();
            }catch(Exception e){Log.e("testeo", "C: Error", e);}finally {
                return null;
            }

        }
    }

}