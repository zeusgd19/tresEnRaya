package juego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteTresEnRaya extends JFrame implements ActionListener {

    private static final String SERVIDOR_IP = "localhost";
    private static final int SERVIDOR_PUERTO = 12345;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    private JPanel panel = new JPanel();
    private JButton[] botones = new JButton[9];
    private String[][] tablero;

    private static final int ROWS = 3;
    private static final int COLUMNS = 3;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 100;

    private int turno = 0;

    private boolean gameOver = false;

    public ClienteTresEnRaya() {
        super("Tres En Raya");
        this.setSize(COLUMNS * BUTTON_WIDTH + 16, ROWS * BUTTON_HEIGHT + 39);
        this.setResizable(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        panel.setBackground(Color.blue);
        panel.setLayout(null);
        botones = new JButton[ROWS * COLUMNS];
        tablero = new String[ROWS][COLUMNS];

        for (int i = 0; i < botones.length; i++) {
            botones[i] = new JButton();
            botones[i].setBounds((i % COLUMNS) * BUTTON_WIDTH, (i / ROWS) * BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);
            botones[i].addActionListener(this);
            botones[i].setBackground(Color.gray);
            panel.add(botones[i]);
            tablero[i / ROWS][i % COLUMNS] = "";
        }
        this.add(panel);
        this.setVisible(true);

        conectarAServidor();
        jugar();
    }

    private void conectarAServidor() {
        try {
            socket = new Socket(SERVIDOR_IP, SERVIDOR_PUERTO);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void jugar() {
        Thread thread = new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println("Mensaje recibido del servidor: " + mensaje);
                    if (mensaje.startsWith("INICIO")) {
                        // Actualizar interfaz con el estado inicial del juego
                        actualizarInterfaz(mensaje);
                    } else if (mensaje.startsWith("ESTADO")) {
                        // Actualizar interfaz con el estado actual del juego
                        actualizarInterfaz(mensaje);
                    } else if (mensaje.equals("TURNO")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "¡Es tu turno!"));
                    } else if (mensaje.equals("ESPERA")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Esperando al otro jugador..."));
                    } else if (mensaje.equals("GANADOR")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "¡Felicidades! ¡Has ganado!"));
                    } else if (mensaje.equals("PERDEDOR")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "¡Has perdido! ¡Inténtalo de nuevo!"));
                    } else if (mensaje.equals("EMPATE")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "¡Es un empate!"));
                    }
                }
            } catch (IOException e) {
                // Error de E/S, probablemente la conexión se cerró
                System.err.println("Error de E/S en la comunicación con el servidor: " + e.getMessage());
            } finally {
                // Cerrar conexión y recursoss
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    // Error al cerrar la conexión
                    System.err.println("Error al cerrar la conexión con el servidor: " + e.getMessage());
                }
            }
        });
        thread.start();
    }

    private void actualizarInterfaz(String mensaje) {
        String[] partes = mensaje.split(",");
        if (partes.length == 10) {
            for (int i = 0; i < botones.length; i++) {
                botones[i].setText(partes[i + 1]);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        JButton botonPulsado = (JButton) e.getSource();
        int indice = -1;
        for (int i = 0; i < botones.length; i++) {
            botones[i].setFont(new Font("Arial", 3, 40));
            if (botones[i] == botonPulsado) {
                indice = i;
            }
        }

        if (!tablero[indice / ROWS][indice % COLUMNS].isEmpty()) {
            return;
        }

        if (turno == 0) {
            botonPulsado.setText("X");
        } else {
            botonPulsado.setText("O");
        }

        tablero[indice / ROWS][indice % COLUMNS] = botonPulsado.getText();

        // Enviar la jugada al servidor
        salida.println("JUGADA," + (indice / ROWS) + "," + (indice % COLUMNS));

        turno = (turno + 1) % 2;
    }

    public static void main(String[] args) {
        new ClienteTresEnRaya();
    }
}
