package com.miapp;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;

public class cliente extends JFrame {
    private final String[] SIMBOLOS = {
            "banana", "cherries", "dollar", "lemon", "orange", "potato", "tomato"
    };

    private JLabel lblImagen, lblEstado;
    private JPanel panelPrincipal, panelImagen, panelEstado;

    private Socket socket;
    private BufferedReader in;

    private Timer animacionTimer;
    private int indiceActual = 0;

    public cliente() {
        setTitle("Rodillo - Cliente");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null);

        inicializarComponentes();
        configurarLayout();
        iniciarAnimacionGiro();
        conectarAlServidor();
    }

    private void inicializarComponentes() {
        panelPrincipal = new JPanel(new BorderLayout());

        panelImagen = new JPanel();
        panelImagen.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lblImagen = new JLabel();
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);

        panelEstado = new JPanel(new BorderLayout());
        lblEstado = new JLabel("Conectando al servidor...");
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void configurarLayout() {
        panelImagen.add(lblImagen);
        panelEstado.add(lblEstado, BorderLayout.CENTER);

        panelPrincipal.add(panelImagen, BorderLayout.CENTER);
        panelPrincipal.add(panelEstado, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void iniciarAnimacionGiro() {
        animacionTimer = new Timer(100, e -> {
            mostrarSimbolo(SIMBOLOS[indiceActual]);
            indiceActual = (indiceActual + 1) % SIMBOLOS.length;
        });
        animacionTimer.start();
    }

    private void mostrarSimbolo(String simbolo) {
        String archivo = simbolo + ".png";
        URL url = getClass().getResource("/assets/" + archivo);
        if (url != null) {
            lblImagen.setIcon(new ImageIcon(url));
        } else {
            lblImagen.setText("❌ Imagen no encontrada");
        }
    }

    private void mostrarResultadoFinal(String simboloFinal, String estadoJuego) {
        animacionTimer.stop();
        mostrarSimbolo(simboloFinal);
        lblEstado.setText("Resultado: " + simboloFinal + " | " + estadoJuego);

        // Mantener el símbolo visible por 5 segundos
        Timer retomar = new Timer(5000, e -> animacionTimer.start());
        retomar.setRepeats(false);
        retomar.start();
    }

    private void conectarAlServidor() {
        new Thread(() -> {
            try {
                socket = new Socket("192.168.0.12", 5000); // Cambia IP a la del servidor real
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                SwingUtilities.invokeLater(() -> lblEstado.setText("Conectado al servidor"));

                while (true) {
                    String mensaje = in.readLine();
                    if (mensaje != null && mensaje.startsWith("RESULTADO:")) {
                        String[] partes = mensaje.split(":");
                        if (partes.length >= 3) {
                            String simbolo = partes[1];
                            String estado = partes[2];
                            SwingUtilities.invokeLater(() -> mostrarResultadoFinal(simbolo, estado));
                        }
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> lblEstado.setText("❌ Error de conexión"));
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            SwingUtilities.invokeLater(() -> new cliente().setVisible(true));
        } catch (Exception e) {
            System.out.println("No se pudo cargar FlatLaf");
        }
    }
}
