package com.dpdm.reflexoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor ac;
    private LocationManager locationManager;
    private GameViewModel viewModel;
    private BancoDados bancoDados;

    private TextView tvBest;
    private TextView tvSensorHint;
    private Button btnHistory;
    private Button btnStart;
    private ReactionView reactionView;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private float lastAccelX = 0f;
    private float lastAccelY = 0f;
    private float lastAccelZ = 0f;
    private long lastShakeTime = 0;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bancoDados = new BancoDados(this);
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        tvBest = findViewById(R.id.tvBest);
        tvSensorHint = findViewById(R.id.tvSensorHint);
        btnHistory = findViewById(R.id.btnHistory);
        btnStart = findViewById(R.id.btnStart);
        reactionView = findViewById(R.id.reactionView);

        atualizarRecorde();

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            ac = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (ac == null) {
            tvSensorHint.setText("Este dispositivo não possui sensor de aceleração.");
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        } else {
            iniciarGPS();
        }

        viewModel.getState().observe(this, state -> {
            if (state == null) return;

            switch (state) {
                case IDLE:
                    reactionView.setState(GameViewModel.State.IDLE, "Toque ou mexa para iniciar");
                    tvSensorHint.setText("Mexa o celular ou toque em Iniciar");
                    btnStart.setEnabled(true);
                    break;
                case WAITING:
                    reactionView.setState(GameViewModel.State.WAITING, "Aguarde...");
                    tvSensorHint.setText("Aguarde a cor mudar...");
                    btnStart.setEnabled(false);
                    break;
                case GO:
                    reactionView.setState(GameViewModel.State.GO, "TOQUE AGORA!");
                    tvSensorHint.setText("Toque o mais rápido possível!");
                    btnStart.setEnabled(false);
                    break;
                case RESULT:
                    Long timeMs = viewModel.getLastTimeMs().getValue();
                    String msg = (timeMs != null) ? timeMs + " ms" : "Resultado";
                    reactionView.setState(GameViewModel.State.RESULT, msg);
                    tvSensorHint.setText("Toque ou mexa para tentar novamente");
                    btnStart.setEnabled(true);
                    break;
                case TOO_EARLY:
                    reactionView.setState(GameViewModel.State.TOO_EARLY, "Muito cedo!");
                    tvSensorHint.setText("Você tocou antes da hora!");
                    btnStart.setEnabled(true);
                    break;
            }
        });

        reactionView.setOnClickListener(v -> processarToqueNaTela());

        btnStart.setOnClickListener(v -> {
            GameViewModel.State currentState = viewModel.getState().getValue();
            if (currentState == GameViewModel.State.IDLE || currentState == GameViewModel.State.RESULT || currentState == GameViewModel.State.TOO_EARLY) {
                viewModel.startRound();
            }
        });

        btnHistory.setOnClickListener(v -> abrirHistorico());
    }

    private void processarToqueNaTela() {
        GameViewModel.State currentState = viewModel.getState().getValue();
        if (currentState == GameViewModel.State.IDLE || currentState == GameViewModel.State.RESULT || currentState == GameViewModel.State.TOO_EARLY) {
            viewModel.startRound();
        } else if (currentState == GameViewModel.State.WAITING || currentState == GameViewModel.State.GO) {
            long ms = viewModel.onScreenTap();
            if (ms > 0) {
                String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                bancoDados.salvarEvento(lastAccelX, lastAccelY, lastAccelZ, ms, currentLatitude, currentLongitude, dateStr);
                atualizarRecorde();
            }
        }
    }

    private void atualizarRecorde() {
        long recorde = bancoDados.getMelhorTempo();
        if (recorde > 0) {
            tvBest.setText(String.format(Locale.getDefault(), "Recorde: %d ms", recorde));
        } else {
            tvBest.setText("Recorde: --");
        }
    }

    private void abrirHistorico() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.historico);
        dialog.setTitle("Histórico de Resultados");

        ListView listView = dialog.findViewById(R.id.listView);
        Button btnClear = dialog.findViewById(R.id.btnClear);

        carregarListaHistorico(listView);

        btnClear.setOnClickListener(v -> {
            bancoDados.limparHistorico();
            carregarListaHistorico(listView);
            atualizarRecorde();
        });

        dialog.show();
    }

    private void carregarListaHistorico(ListView listView) {
        List<Map<String, String>> historico = bancoDados.obterHistorico();
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                historico,
                R.layout.resultado_por_item,
                new String[]{"tempo", "data", "localizacao"},
                new int[]{R.id.tvTime, R.id.tvDate, R.id.tvLocation}
        );
        listView.setAdapter(adapter);
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void iniciarGPS() {
        if (hasLocationPermission()) {
            if (locationManager != null) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, locationListener);

                    Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Location bestLoc = lastGps != null ? lastGps : lastNet;
                    if (bestLoc != null) {
                        currentLatitude = bestLoc.getLatitude();
                        currentLongitude = bestLoc.getLongitude();
                    }
                } catch (Exception ignored) {
                    tvSensorHint.setText("Não foi possível obter a localização do dispositivo.");
                }
            }
        } else {
            tvSensorHint.setText("Localização negada. O jogo continua funcionando sem GPS.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event != null && event.values != null && event.values.length >= 3) {
            lastAccelX = event.values[0];
            lastAccelY = event.values[1];
            lastAccelZ = event.values[2];

            float gX = lastAccelX / SensorManager.GRAVITY_EARTH;
            float gY = lastAccelY / SensorManager.GRAVITY_EARTH;
            float gZ = lastAccelZ / SensorManager.GRAVITY_EARTH;
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            long now = System.currentTimeMillis();
            if (gForce > 2.5f) { // Detecção de sacudida
                if (now - lastShakeTime > 1200) {
                    lastShakeTime = now;
                    GameViewModel.State currentState = viewModel.getState().getValue();
                    if (currentState == GameViewModel.State.IDLE || currentState == GameViewModel.State.RESULT || currentState == GameViewModel.State.TOO_EARLY) {
                        viewModel.startRound();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (hasLocationPermission()) {
                iniciarGPS();
            } else {
                tvSensorHint.setText("Permissão de localização negada. O jogo segue funcionando sem GPS.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sm != null && ac != null) {
            sm.registerListener(this, ac, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (hasLocationPermission()) {
            iniciarGPS();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sm != null) {
            sm.unregisterListener(this);
        }
        if (locationManager != null && hasLocationPermission()) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
