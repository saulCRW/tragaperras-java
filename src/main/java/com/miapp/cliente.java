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

    private JLabel lblImagen;
    private JPanel panelPrincipal, panelImagen;

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
        mostrarSimbolo(SIMBOLOS[0]); // símbolo inicial estático
        conectarAlServidor();
        prepararAnimacion();
    }

    private void inicializarComponentes() {
        panelPrincipal = new JPanel(new BorderLayout());

        panelImagen = new JPanel();
        panelImagen.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lblImagen = new JLabel();
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void configurarLayout() {
        panelImagen.add(lblImagen);
        panelPrincipal.add(panelImagen, BorderLayout.CENTER);
        add(panelPrincipal);
    }

    private void prepararAnimacion() {
        animacionTimer = new Timer(100, e -> {
            mostrarSimbolo(SIMBOLOS[indiceActual]);
            indiceActual = (indiceActual + 1) % SIMBOLOS.length;
        });
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

    private void mostrarResultadoFinal(String simboloFinal) {
        animacionTimer.stop();
        mostrarSimbolo(simboloFinal);
        // Nada de textos
    }

    private void conectarAlServidor() {
        new Thread(() -> {
            try {
                socket = new Socket("192.168.1.65", 5000); // Cambia esta IP por la del servidor real
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (true) {
                    String mensaje = in.readLine();
                    if (mensaje == null) continue;

                    if (mensaje.equals("GIRAR")) {
                        SwingUtilities.invokeLater(() -> animacionTimer.start());
                    } else if (mensaje.startsWith("RESULTADO:")) {
                        String[] partes = mensaje.split(":");
                        if (partes.length >= 2) {
                            String simbolo = partes[1];
                            SwingUtilities.invokeLater(() -> mostrarResultadoFinal(simbolo));
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Error de conexión con el servidor");
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
