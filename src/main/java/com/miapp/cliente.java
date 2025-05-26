package com.miapp;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Random;

public class cliente extends JFrame {
    private final String[] SIMBOLOS = {
            "banana", "cherries", "dollar", "lemon", "orange", "potato", "tomato"
    };

    private JLabel lblImagen, lblEstado;
    private JPanel panelPrincipal, panelImagen, panelEstado;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final Random random = new Random();

    public cliente() {
        setTitle("Rodillo - Cliente");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null);

        inicializarComponentes();
        configurarLayout();
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

    private void conectarAlServidor() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                SwingUtilities.invokeLater(() -> lblEstado.setText("Conectado al servidor. Esperando juego..."));

                while (true) {
                    String mensaje = in.readLine();
                    if (mensaje != null && mensaje.equalsIgnoreCase("INICIAR_JUEGO")) {
                        SwingUtilities.invokeLater(this::iniciarJuego);
                    }
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> lblEstado.setText("❌ Error de conexión con servidor"));
                e.printStackTrace();
            }
        }).start();
    }

    private void iniciarJuego() {
        // Selecciona símbolo aleatorio
        int index = random.nextInt(SIMBOLOS.length);
        String simbolo = SIMBOLOS[index];

        // Muestra imagen
        mostrarSimbolo(simbolo);

        // Envía resultado al servidor
        if (out != null) {
            out.println("RESULTADO:" + simbolo);
        }

        lblEstado.setText("Símbolo: " + simbolo);
    }

    private void mostrarSimbolo(String simbolo) {
        String nombreArchivo = simbolo + ".png";
        URL url = getClass().getResource("/assets/" + nombreArchivo);
        if (url != null) {
            lblImagen.setIcon(new ImageIcon(url));
        } else {
            lblImagen.setText("❌ Imagen no encontrada");
        }
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
