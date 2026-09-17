package com.example.aquaplay;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    // --- CONSTANTES ---
    private static final int TOTAL_ESFERAS = 20;
    private static final float GRAVIDADE = 0.5f;
    private static final float FORCA_JATO = -19f;
    private static final float RESISTENCIA_AGUA = 0.98f;
    private static final int ESFERA_TAMANHO = 30;
    private static final int CHAO_OFFSET = 80;

    // --- UI ---
    private TextView txtPontos, txtTempo, txtMensagemFim;
    private LinearLayout layoutFimJogo;
    private RelativeLayout telaTitulo, telaJogo;
    private Button btnReiniciar, btnSair;
    private ImageView imgCesta, imgCesta2;

    // --- ESTADO ---
    private int pontos = 0, tempoRestante, nivelAtual = 1;
    private boolean jogoAtivo = false;
    private final Random geradorAleatorio = new Random();
    private final Handler manipulador = new Handler(Looper.getMainLooper());

    // --- ÁUDIO ---
    private SoundPool poolSons;
    private int somInicio, somVitoria, somDerrota;
    private MediaPlayer reprodutorMusica;

    // --- ENTIDADES ---
    private static class Bolha {
        ImageView view; float y, velY, alfa = 1.0f;
        Bolha(ImageView v, float y) { this.view = v; this.y = y; this.velY = -(3f + new Random().nextFloat() * 5f); }
    }
    private static class Particula {
        ImageView view; float x, y, velX, velY, alfa = 1.0f;
        Particula(ImageView v, float x, float y, float vx, float vy) { this.view = v; this.x = x; this.y = y; this.velX = vx; this.velY = vy; }
    }
    private static class Esfera {
        ImageView view; float x, y, velX, velY, angulo, velRot; boolean naCesta = false;
        Esfera(ImageView v) { this.view = v; }
    }

    private final List<Bolha> bolhas = new ArrayList<>();
    private final List<Particula> particulas = new ArrayList<>();
    private final List<Esfera> esferas = new ArrayList<>();

    private int direcaoCesta = 1, direcaoCestaY = 1;
    private float anguloCesta = 0f, tempoAnimacao = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        configurarTelaCheia();
        inicializarComponentes();
        configurarEventos();
        inicializarEsferas();
        inicializarAudio();
    }

    private void inicializarComponentes() {
        txtPontos = findViewById(R.id.txtPontos);
        txtTempo = findViewById(R.id.txtTempo);
        txtMensagemFim = findViewById(R.id.txtMensagemFim);
        layoutFimJogo = findViewById(R.id.FimDeJogo);
        btnReiniciar = findViewById(R.id.btnReiniciar);
        btnSair = findViewById(R.id.btnSair);
        telaJogo = findViewById(R.id.telaJogo);
        imgCesta = findViewById(R.id.imgCesta);
        imgCesta2 = findViewById(R.id.imgCesta2);
        telaTitulo = findViewById(R.id.telaTitulo);
        findViewById(R.id.imgEsfera).setVisibility(View.GONE);
    }

    private void configurarEventos() {
        findViewById(R.id.btnJogar).setOnClickListener(v -> {
            telaTitulo.setVisibility(View.GONE);
            mostrarTelaInicio();
        });
        btnReiniciar.setOnClickListener(v -> reiniciarJogo());
        btnSair.setOnClickListener(v -> finish());
        telaJogo.setOnClickListener(v -> dispararJato());
    }

    private void inicializarAudio() {
        reprodutorMusica = MediaPlayer.create(this, R.raw.musica_fundo);
        if (reprodutorMusica != null) {
            reprodutorMusica.setLooping(true);
            reprodutorMusica.setVolume(0.5f, 0.5f);
            reprodutorMusica.start();
        }
        poolSons = new SoundPool.Builder().setMaxStreams(5)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build();
        somInicio = poolSons.load(this, R.raw.som_inicio, 1);
        somVitoria = poolSons.load(this, R.raw.som_vitoria, 1);
        somDerrota = poolSons.load(this, R.raw.som_derrota, 1);
    }

    private void mostrarTelaInicio() {
        jogoAtivo = false;
        int[] intros = {R.string.msg_inicio, R.string.msg_nivel2_intro, R.string.msg_nivel3_intro, 
                        R.string.msg_nivel4_intro, R.string.msg_nivel5_intro, R.string.msg_nivel6_intro, 
                        R.string.msg_nivel7_intro, R.string.msg_nivel8_intro, R.string.msg_nivel8_intro, R.string.msg_nivel8_intro};
        
        txtMensagemFim.setText(intros[Math.min(nivelAtual - 1, 9)]);
        btnReiniciar.setText(nivelAtual == 1 ? R.string.btn_comecar : R.string.btn_proximo_nivel);
        btnSair.setVisibility(nivelAtual == 1 ? View.GONE : View.VISIBLE);
        layoutFimJogo.setVisibility(View.VISIBLE);
        
        for (Esfera e : esferas) e.view.setVisibility(View.INVISIBLE);
        imgCesta2.setVisibility(nivelAtual >= 8 ? View.VISIBLE : View.GONE);
        
        if (nivelAtual == 1 && poolSons != null) poolSons.play(somInicio, 1, 1, 0, 0, 1);
    }

    private void inicializarEsferas() {
        int[] cores = {Color.RED, Color.rgb(255, 140, 0), Color.rgb(255, 69, 0), 
                       Color.rgb(255, 215, 0), Color.YELLOW, Color.rgb(255, 20, 147), 
                       Color.MAGENTA, Color.rgb(255, 105, 180)};
        for (int i = 0; i < TOTAL_ESFERAS; i++) {
            ImageView v = new ImageView(this);
            v.setImageResource(R.drawable.visual_esfera);
            v.setLayoutParams(new RelativeLayout.LayoutParams(ESFERA_TAMANHO, ESFERA_TAMANHO));
            v.setColorFilter(cores[geradorAleatorio.nextInt(cores.length)], PorterDuff.Mode.MULTIPLY);
            telaJogo.addView(v);
            esferas.add(new Esfera(v));
        }
    }

    private void reiniciarJogo() {
        pontos = 0; tempoRestante = 60; jogoAtivo = true;
        layoutFimJogo.setVisibility(View.GONE);
        prepararCestasParaFase();
        prepararEsferasParaFase();
        atualizarInterface();
        manipulador.removeCallbacksAndMessages(null);
        manipulador.post(cicloJogo);
        manipulador.post(cicloTempo);
    }

    private void prepararCestasParaFase() {
        float dens = getResources().getDisplayMetrics().density;
        imgCesta.setRotation(0); anguloCesta = 0; imgCesta2.setRotation(0); tempoAnimacao = 0;
        if (telaJogo.getWidth() == 0) return;

        if (nivelAtual == 8) {
            imgCesta.setX(telaJogo.getWidth() / 4f - imgCesta.getWidth() / 2f);
            imgCesta2.setX(3 * telaJogo.getWidth() / 4f - imgCesta2.getWidth() / 2f);
            imgCesta.setY(150 * dens); imgCesta2.setY(150 * dens);
        } else if (nivelAtual >= 9) {
            imgCesta.setX(0); imgCesta2.setX(telaJogo.getWidth() - imgCesta2.getWidth());
            imgCesta.setY(80 * dens); imgCesta2.setY(telaJogo.getHeight() - 250 * dens);
        } else {
            imgCesta.setX((telaJogo.getWidth() - imgCesta.getWidth()) / 2f);
            imgCesta.setY(150 * dens);
        }
    }

    private void prepararEsferasParaFase() {
        for (Esfera e : esferas) {
            e.naCesta = false; e.view.setVisibility(View.VISIBLE);
            e.x = geradorAleatorio.nextFloat() * (telaJogo.getWidth() > 0 ? telaJogo.getWidth() - ESFERA_TAMANHO : 500);
            e.y = telaJogo.getHeight() > 0 ? telaJogo.getHeight() - 150 : 800;
            e.velX = 0; e.velY = 0; e.angulo = geradorAleatorio.nextFloat() * 360;
            e.view.setX(e.x); e.view.setY(e.y); e.view.setRotation(e.angulo);
        }
    }

    private void atualizarInterface() {
        txtPontos.setText(getString(R.string.pontos_label, pontos));
        txtTempo.setText(getString(R.string.tempo_label, tempoRestante));
    }

    private void dispararJato() {
        if (!jogoAtivo) return;
        for (int i = 0; i < 5; i++) {
            ImageView v = new ImageView(this); v.setImageResource(R.drawable.visual_bolha);
            int t = 15 + geradorAleatorio.nextInt(20); v.setLayoutParams(new RelativeLayout.LayoutParams(t, t));
            float px = geradorAleatorio.nextFloat() * (telaJogo.getWidth() > 0 ? telaJogo.getWidth() : 500);
            float py = telaJogo.getHeight() > 0 ? telaJogo.getHeight() - 50 : 800;
            v.setX(px); v.setY(py); telaJogo.addView(v);
            bolhas.add(new Bolha(v, py));
        }
        for (Esfera e : esferas) {
            if (e.naCesta) continue;
            e.velY = FORCA_JATO * (0.8f + geradorAleatorio.nextFloat() * 0.4f);
            e.velX = geradorAleatorio.nextFloat() * 40 - 20; e.velRot = geradorAleatorio.nextFloat() * 40 - 20;
        }
    }

    private final Runnable cicloJogo = new Runnable() {
        @Override public void run() {
            if (jogoAtivo) { 
                atualizarMovimentoCesta(); atualizarMovimentoEsferas(); 
                atualizarEfeitos(); verificarColisoes(); 
                manipulador.postDelayed(this, 20); 
            }
        }
    };

    private final Runnable cicloTempo = new Runnable() {
        @Override public void run() {
            if (jogoAtivo && tempoRestante-- > 0) {
                atualizarInterface();
                if (tempoRestante == 0) encerrarJogo(false);
                else manipulador.postDelayed(this, 1000);
            }
        }
    };

    private void atualizarMovimentoCesta() {
        if (telaJogo.getWidth() == 0) return;
        if (nivelAtual >= 2 && nivelAtual != 8) {
            float speed = (nivelAtual == 3 || nivelAtual == 5) ? 15f : 7f;
            float nx1 = imgCesta.getX() + (speed * direcaoCesta);
            if (nx1 < 0 || nx1 > telaJogo.getWidth() - imgCesta.getWidth()) direcaoCesta *= -1;
            imgCesta.setX(nx1);
            if (nivelAtual >= 9) imgCesta2.setX(imgCesta2.getX() + (speed * -direcaoCesta));
        }
        if (nivelAtual == 4 || nivelAtual == 5 || nivelAtual == 7 || nivelAtual == 10) {
            anguloCesta = (anguloCesta + (nivelAtual == 5 ? 4f : (nivelAtual == 7 ? 3f : 2f))) % 360;
            imgCesta.setRotation(anguloCesta);
            if (nivelAtual == 10) imgCesta2.setRotation(-anguloCesta);
        } else { imgCesta.setRotation(0); imgCesta2.setRotation(0); }
        if (nivelAtual == 6 || nivelAtual == 7) {
            float ny = imgCesta.getY() + (4f * direcaoCestaY);
            if (ny < 100 || ny > telaJogo.getHeight() / 2f) direcaoCestaY *= -1;
            imgCesta.setY(ny);
        }
        if (nivelAtual == 8) {
            tempoAnimacao += 0.05f; float range = telaJogo.getHeight() / 6f;
            float baseY = 200 * getResources().getDisplayMetrics().density;
            imgCesta.setY(baseY + (float) Math.sin(tempoAnimacao) * range);
            imgCesta2.setY(baseY + (float) Math.cos(tempoAnimacao * 1.2f) * range);
        }
    }

    private void atualizarMovimentoEsferas() {
        for (Esfera e : esferas) {
            if (e.naCesta) continue;
            e.velY += GRAVIDADE; e.y += e.velY;
            if (e.y > telaJogo.getHeight() - CHAO_OFFSET) { e.y = telaJogo.getHeight() - CHAO_OFFSET; e.velY *= -0.3f; }
            if (e.y < 0) { e.y = 0; e.velY = 0; }
            e.velX *= RESISTENCIA_AGUA; e.x += e.velX;
            if (e.x < 0 || e.x > telaJogo.getWidth() - ESFERA_TAMANHO) { 
                e.velX *= -0.6f; e.x = Math.max(0, Math.min(e.x, telaJogo.getWidth() - ESFERA_TAMANHO)); 
            }
            verificarColisaoCesta(e, imgCesta);
            if (nivelAtual >= 8) verificarColisaoCesta(e, imgCesta2);
            e.velRot *= RESISTENCIA_AGUA; e.angulo += e.velRot;
            e.view.setX(e.x); e.view.setY(e.y); e.view.setRotation(e.angulo);
        }
    }

    private void atualizarEfeitos() {
        Iterator<Bolha> itB = bolhas.iterator();
        while (itB.hasNext()) {
            Bolha b = itB.next(); b.y += b.velY; b.alfa -= 0.02f;
            if (b.alfa <= 0 || b.y < -50) { telaJogo.removeView(b.view); itB.remove(); }
            else { b.view.setY(b.y); b.view.setAlpha(b.alfa); }
        }
        Iterator<Particula> itP = particulas.iterator();
        while (itP.hasNext()) {
            Particula p = itP.next();
            p.velY += 0.3f; p.x += p.velX; p.y += p.velY; p.alfa -= 0.015f;
            if (p.alfa <= 0 || p.y > telaJogo.getHeight() + 50) { telaJogo.removeView(p.view); itP.remove(); }
            else { p.view.setX(p.x); p.view.setY(p.y); p.view.setAlpha(p.alfa); }
        }
    }

    private void criarFogoArtificio(float x, float y) {
        int[] cores = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.WHITE};
        int cor = cores[geradorAleatorio.nextInt(cores.length)];
        for (int i = 0; i < 40; i++) {
            ImageView v = new ImageView(this); v.setImageResource(R.drawable.visual_confete);
            v.setLayoutParams(new RelativeLayout.LayoutParams(6, 6));
            v.setColorFilter(cor, PorterDuff.Mode.SRC_IN);
            v.setX(x); v.setY(y); telaJogo.addView(v);
            double ang = Math.toRadians(geradorAleatorio.nextFloat() * 360);
            float forca = 5f + geradorAleatorio.nextFloat() * 12f;
            particulas.add(new Particula(v, x, y, (float)Math.cos(ang) * forca, (float)Math.sin(ang) * forca));
        }
    }

    private void verificarColisaoCesta(Esfera e, ImageView cesta) {
        float eCX = e.x + 15, eCY = e.y + 15;
        float cCX = cesta.getX() + cesta.getWidth() / 2f, cCY = cesta.getY() + cesta.getHeight() / 2f;
        float dx = eCX - cCX, dy = eCY - cCY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 45 && !verificarGol(e, cesta)) {
            float nx = dx / dist, ny = dy / dist;
            e.x = cCX + nx * 45 - 15; e.y = cCY + ny * 45 - 15;
            e.velX = nx * Math.abs(e.velX + 2) * 0.8f; e.velY = ny * Math.abs(e.velY + 2) * 0.8f;
        }
    }

    private boolean verificarGol(Esfera e, ImageView cesta) {
        float eCX = e.x + 15, eCY = e.y + 15;
        float cCX = cesta.getX() + cesta.getWidth() / 2f, cCY = cesta.getY() + cesta.getHeight() / 2f;
        double rad = Math.toRadians(cesta.getRotation());
        float offset = (cesta.getHeight() / 2f) - 15; 
        float golX = cCX + (float) (Math.sin(rad) * offset), golY = cCY - (float) (Math.cos(rad) * offset);
        float dx = eCX - golX, dy = eCY - golY;
        float distParaGol = (float) Math.sqrt(dx * dx + dy * dy);
        float cos = (float) Math.cos(-rad), sin = (float) Math.sin(-rad);
        float distLateral = Math.abs(dx * cos - dy * sin); 
        float limiteG = (nivelAtual == 1) ? 60f : 45f, limiteL = (nivelAtual == 1) ? 35f : 22f;
        return distParaGol < limiteG && distLateral < limiteL;
    }

    private void verificarColisoes() {
        for (int i = 0; i < esferas.size(); i++) {
            Esfera e1 = esferas.get(i); if (e1.naCesta) continue;
            for (int j = i + 1; j < esferas.size(); j++) {
                Esfera e2 = esferas.get(j); if (e2.naCesta) continue;
                float dx = e2.x - e1.x, dy = e2.y - e1.y, dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < 30 && dist > 0) {
                    float push = (30 - dist) / 2;
                    e1.x -= (dx/dist) * push; e1.y -= (dy/dist) * push;
                    e2.x += (dx/dist) * push; e2.y += (dy/dist) * push;
                    float tx = e1.velX, ty = e1.velY; e1.velX = e2.velX * 0.7f; e1.velY = e2.velY * 0.7f; e2.velX = tx * 0.7f; e2.velY = ty * 0.7f;
                }
            }
        }
        for (Esfera e : esferas) {
            if (!e.naCesta && (verificarGol(e, imgCesta) || (nivelAtual >= 8 && verificarGol(e, imgCesta2)))) {
                e.naCesta = true; e.view.setVisibility(View.INVISIBLE); pontos++; atualizarInterface();
                if (pontos >= TOTAL_ESFERAS) { encerrarJogo(true); return; }
            }
        }
    }

    private void encerrarJogo(boolean venceu) {
        jogoAtivo = false;
        if (venceu && nivelAtual < 10) {
            if (poolSons != null) poolSons.play(somVitoria, 1, 1, 0, 0, 1);
            nivelAtual++; mostrarTelaInicio(); return;
        }
        if (poolSons != null) poolSons.play(venceu ? somVitoria : somDerrota, 1, 1, 0, 0, 1);
        if (!venceu) nivelAtual = 1;
        else if (nivelAtual == 10) {
            manipulador.postDelayed(() -> criarFogoArtificio(telaJogo.getWidth() * 0.2f, telaJogo.getHeight() * 0.4f), 500);
            manipulador.postDelayed(() -> criarFogoArtificio(telaJogo.getWidth() * 0.8f, telaJogo.getHeight() * 0.3f), 1200);
            manipulador.postDelayed(() -> criarFogoArtificio(telaJogo.getWidth() * 0.5f, telaJogo.getHeight() * 0.2f), 1900);
            jogoAtivo = true;
            manipulador.postDelayed(() -> { jogoAtivo = false; layoutFimJogo.setVisibility(View.VISIBLE); nivelAtual = 1; }, 5000);
            txtMensagemFim.setText(R.string.msg_venceu_final); btnReiniciar.setText(R.string.de_novo);
            btnSair.setVisibility(View.VISIBLE); return;
        }
        txtMensagemFim.setText(venceu ? R.string.msg_venceu_final : R.string.msg_fim_tempo);
        btnReiniciar.setText(R.string.de_novo); btnSair.setVisibility(View.VISIBLE); layoutFimJogo.setVisibility(View.VISIBLE);
    }

    @Override protected void onDestroy() {
        super.onDestroy(); manipulador.removeCallbacksAndMessages(null);
        if (poolSons != null) poolSons.release();
        if (reprodutorMusica != null) { reprodutorMusica.stop(); reprodutorMusica.release(); }
    }

    private void configurarTelaCheia() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController ctrl = getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
