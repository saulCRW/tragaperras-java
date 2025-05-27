package com.miapp;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;


public class servidor extends JFrame {
    private static final int MAX_CLIENTES = 3;
    private final String[] SIMBOLOS = {
            "banana", "cherries", "dollar", "lemon", "orange", "potato", "tomato"
    };

    private ServerSocket serverSocket;
    private final Map<Integer, PrintWriter> salidas = new HashMap<>();
    private final Map<Integer, Socket> clientes = new HashMap<>();

    private boolean juegoEnCurso = false;
    private int creditos = 5;
    private int apuestaActual = 1;

    private final String[] ESTADOSPALANCA = { "palancaUP", "palancaDOWN" };
    private final Integer[] APUESTAS = { 1, 2, 5, 10 };

    private JPanel panelPrincipal, panelStatus, panelControles, panelEstado, panelInferior;
    private JLabel lblStatus, lblEstado, lblControles, lblCreditos;
    private JComboBox<Integer> cmbApuesta;
    private JButton btnInsertarCredito;

    public servidor() {
        setTitle("Máquina Tragaperras - Servidor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        inicializarComponentes();
        configurarLayout();
        configurarEventos();
        iniciarServidor();
    }

    private void inicializarComponentes() {
        panelPrincipal = new JPanel(new BorderLayout());
        panelControles = new JPanel(new BorderLayout());
        panelEstado = new JPanel(new BorderLayout());
        panelStatus = new JPanel(new BorderLayout());
        panelInferior = new JPanel(new FlowLayout());

        lblEstado = new JLabel("Esperando conexiones...");
        lblCreditos = new JLabel("Créditos: " + creditos);
        lblCreditos.setHorizontalAlignment(SwingConstants.RIGHT);

        lblStatus = new JLabel("Esperando acción...");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        lblControles = new JLabel();
        cambiarImagenPalanca(0);
        lblControles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cmbApuesta = new JComboBox<>(APUESTAS);
        btnInsertarCredito = new JButton("Insertar crédito");
    }

    private void configurarLayout() {
        panelPrincipal.add(panelEstado, BorderLayout.NORTH);
        panelPrincipal.add(panelStatus, BorderLayout.CENTER);
        panelPrincipal.add(panelControles, BorderLayout.EAST);
        panelPrincipal.add(panelInferior, BorderLayout.SOUTH);

        panelEstado.add(lblEstado, BorderLayout.WEST);
        panelEstado.add(lblCreditos, BorderLayout.EAST);
        panelStatus.add(lblStatus, BorderLayout.CENTER);
        panelControles.add(lblControles, BorderLayout.CENTER);

        panelInferior.add(new JLabel("Apuesta:"));
        panelInferior.add(cmbApuesta);
        panelInferior.add(btnInsertarCredito);

        add(panelPrincipal);
    }

    private void configurarEventos() {
        lblControles.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!juegoEnCurso && clientes.size() == MAX_CLIENTES) {
                    apuestaActual = (int) cmbApuesta.getSelectedItem();
                    if (creditos >= apuestaActual) {
                        creditos -= apuestaActual;
                        actualizarCreditos();
                        cambiarImagenPalanca(1);
                        juegoEnCurso = true;
                        lblStatus.setText("Jugando con apuesta de " + apuestaActual + " créditos...");
                        enviarResultadosAleatorios();
                    } else {
                        lblStatus.setText("⚠️ Créditos insuficientes para esa apuesta.");
                    }
                }
            }
        });

        btnInsertarCredito.addActionListener(e -> {
            creditos++;
            actualizarCreditos();
            lblStatus.setText("Crédito insertado.");
        });
    }

    private void cambiarImagenPalanca(int indice) {
        String archivo = ESTADOSPALANCA[indice] + ".png";
        URL url = getClass().getResource("/assets/" + archivo);
        if (url != null) {
            lblControles.setIcon(new ImageIcon(url));
        } else {
            System.out.println("❌ Imagen no encontrada: " + archivo);
        }
    }

    private void actualizarCreditos() {
        lblCreditos.setText("Créditos: " + creditos);
    }

    private void iniciarServidor() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                int id = 1;
                while (id <= MAX_CLIENTES) {
                    Socket socket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    clientes.put(id, socket);
                    salidas.put(id, out);
                    int currentId = id;
                    new Thread(() -> escucharCliente(socket, currentId)).start();
                    lblEstado.setText("Conectado: " + clientes.size() + "/" + MAX_CLIENTES);
                    id++;
                }
            } catch (IOException e) {
                e.printStackTrace();
                lblEstado.setText("❌ Error en el servidor");
            }
        }).start();
    }

    private void escucharCliente(Socket socket, int id) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                in.readLine(); // lectura pasiva
            }
        } catch (IOException e) {
            System.out.println("❌ Cliente " + id + " desconectado");
        }
    }

    private void enviarResultadosAleatorios() {
        // Enviar señal de inicio
        for (int i = 1; i <= MAX_CLIENTES; i++) {
            if (salidas.get(i) != null) {
                salidas.get(i).println("GIRAR");
            }
        }

        new Thread(() -> {
            try {
                Thread.sleep(3000); // espera de giro
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Random random = new Random();
            List<String> resultados = new ArrayList<>();
            for (int i = 1; i <= MAX_CLIENTES; i++) {
                String simbolo = SIMBOLOS[random.nextInt(SIMBOLOS.length)];
                resultados.add(simbolo);
            }

            int ganancia = calcularGanancia(resultados);
            String mensajeFinal;

            if (ganancia > 0) {
                creditos += ganancia;
                mensajeFinal = "🎉 Ganaste " + ganancia + " créditos.\n✅ Juego finalizado.";
            } else {
                mensajeFinal = "😢 Perdiste esta vez.\n✅ Juego finalizado.";
            }
            actualizarCreditos();

            String estado = ganancia > 0 ? "Ganaste" : "Perdiste";
            for (int i = 1; i <= MAX_CLIENTES; i++) {
                String simbolo = resultados.get(i - 1);
                if (salidas.get(i) != null) {
                    salidas.get(i).println("RESULTADO:" + simbolo + ":" + estado);
                }
            }

            SwingUtilities.invokeLater(() -> {
                juegoEnCurso = false;
                cambiarImagenPalanca(0);
                lblStatus.setText("<html>" + mensajeFinal.replace("\n", "<br>") + "</html>");
            });
        }).start();
    }

    private int calcularGanancia(List<String> simbolos) {
        Map<String, Integer> conteo = new HashMap<>();
        for (String s : simbolos) {
            conteo.put(s, conteo.getOrDefault(s, 0) + 1);
        }
        if (conteo.containsValue(3)) return apuestaActual * 5;
        if (conteo.containsValue(2)) return apuestaActual * 2;
        return 0;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            SwingUtilities.invokeLater(() -> new servidor().setVisible(true));
        } catch (Exception e) {
            System.out.println("No se pudo cargar FlatLaf");
        }
    }
}
