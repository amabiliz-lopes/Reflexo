package com.dpdm.reflexoapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Random;

public class GameViewModel extends AndroidViewModel {
    public enum State { IDLE, WAITING, GO, RESULT, TOO_EARLY }

    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<Long> lastTimeMs = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private long goTimeNanos;
    private final Runnable goRunnable = () -> {
        goTimeNanos = System.nanoTime();
        state.setValue(State.GO);
    };

    public GameViewModel(@NonNull Application app) { super(app); }

    public LiveData<State> getState() { return state; }
    public LiveData<Long> getLastTimeMs() { return lastTimeMs; }

    public void startRound() {
        if (state.getValue() == State.WAITING || state.getValue() == State.GO) return;
        state.setValue(State.WAITING);
        handler.removeCallbacks(goRunnable);
        handler.postDelayed(goRunnable, 2000 + random.nextInt(3000));
    }

    /** Chamado quando o usuário toca na tela. Retorna o tempo em ms, ou -1 se inválido. */
    public long onScreenTap() {
        State s = state.getValue();
        if (s == State.WAITING) {
            handler.removeCallbacks(goRunnable);
            state.setValue(State.TOO_EARLY);
            return -1;
        } else if (s == State.GO) {
            long ms = (System.nanoTime() - goTimeNanos) / 1_000_000;
            lastTimeMs.setValue(ms);
            state.setValue(State.RESULT);
            return ms;
        } else if (s == State.RESULT || s == State.TOO_EARLY) {
            state.setValue(State.IDLE);
            return -1;
        }
        return -1;
    }

    @Override protected void onCleared() { handler.removeCallbacks(goRunnable); }
}
