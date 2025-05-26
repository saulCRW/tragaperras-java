package com.miapp;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class servidor extends JFrame {
    private static final int MAX_CLIENTES = 3;
    private ServerSocket serverSocket;
    private final Map<Integer, Socket> clientes = new HashMap<>();
    private final Map<Integer, PrintWriter> salidas = new HashMap<>();
    private final Map<Integer, String> resultados = new HashMap<>();

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
        panelControles.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        panelEstado = new JPanel(new BorderLayout());
        lblEstado = new JLabel("Esperando conexiones...");
        lblCreditos = new JLabel("Créditos: " + creditos);
        lblCreditos.setHorizontalAlignment(SwingConstants.RIGHT);

        panelStatus = new JPanel(new BorderLayout());
        lblStatus = new JLabel("Esperando acción...");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        lblControles = new JLabel();
        cambiarImagenPalanca(0);
        lblControles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        panelInferior = new JPanel(new FlowLayout());
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
                        bajarPalanca();
                        juegoEnCurso = true;
                        resultados.clear();
                        lblStatus.setText("Jugando con apuesta de " + apuestaActual + " créditos...");
                        salidas.values().forEach(out -> out.println("INICIAR_JUEGO"));
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

    private void bajarPalanca() {
        cambiarImagenPalanca(1);
    }

    private void subirPalanca() {
        cambiarImagenPalanca(0);
    }

    private void cambiarImagenPalanca(int indice) {
        String nombreArchivo = ESTADOSPALANCA[indice] + ".png";
        URL url = getClass().getResource("/assets/" + nombreArchivo);
        if (url != null) {
            lblControles.setIcon(new ImageIcon(url));
        } else {
            System.out.println("❌ Imagen no encontrada: " + nombreArchivo);
        }
    }

    private void actualizarCreditos() {
        lblCreditos.setText("Créditos: " + creditos);
    }

    private void iniciarServidor() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                int contadorClientes = 0;

                while (contadorClientes < MAX_CLIENTES) {
                    Socket socket = serverSocket.accept();
                    int idRodillo = contadorClientes + 1;
                    clientes.put(idRodillo, socket);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    salidas.put(idRodillo, out);
                    int finalIdRodillo = idRodillo;

                    new Thread(() -> manejarCliente(socket, finalIdRodillo)).start();
                    contadorClientes++;
                    lblEstado.setText("Clientes conectados: " + contadorClientes + "/" + MAX_CLIENTES);
                }

            } catch (IOException e) {
                lblEstado.setText("❌ Error al iniciar servidor.");
                e.printStackTrace();
            }
        }).start();
    }

    private void manejarCliente(Socket socket, int idRodillo) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                String mensaje = in.readLine();
                if (mensaje != null && mensaje.startsWith("RESULTADO:")) {
                    String simbolo = mensaje.split(":")[1];
                    resultados.put(idRodillo, simbolo);
                    System.out.println("Rodillo " + idRodillo + ": " + simbolo);

                    if (resultados.size() == MAX_CLIENTES) {
                        SwingUtilities.invokeLater(this::evaluarResultado);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Cliente desconectado: rodillo " + idRodillo);
        }
    }

    private void evaluarResultado() {
        juegoEnCurso = false;
        subirPalanca();

        List<String> simbolos = new ArrayList<>(resultados.values());
        int ganancia = calcularGanancia(simbolos);
        creditos += ganancia;
        actualizarCreditos();

        if (ganancia > 0) {
            lblStatus.setText("🎉 Ganaste " + ganancia + " créditos con: " + simbolos);
        } else {
            lblStatus.setText("😢 No ganaste. Resultado: " + simbolos);
        }
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
