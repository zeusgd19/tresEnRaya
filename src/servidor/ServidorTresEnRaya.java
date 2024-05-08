package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorTresEnRaya {

    private static final int PUERTO = 12345;
    private static final int NUMERO_JUGADORES = 2;

    private Socket[] jugadores = new Socket[NUMERO_JUGADORES];
    private PrintWriter[] salidas = new PrintWriter[NUMERO_JUGADORES];
    private BufferedReader[] entradas = new BufferedReader[NUMERO_JUGADORES];
    private String[][] tablero;
    private int turno = 0;

    public ServidorTresEnRaya() {
        tablero = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = "";
            }
        }
        iniciarServidor();
    }

    private void iniciarServidor() {
        try {
            ServerSocket serverSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor iniciado. Esperando jugadores...");

            for (int i = 0; i < NUMERO_JUGADORES; i++) {
                jugadores[i] = serverSocket.accept();
                salidas[i] = new PrintWriter(jugadores[i].getOutputStream(), true);
                entradas[i] = new BufferedReader(new InputStreamReader(jugadores[i].getInputStream()));
                System.out.println("Jugador " + (i + 1) + " conectado.");

                if (i == NUMERO_JUGADORES - 1) {
                    salidas[i].println("INICIO");
                } else {
                    salidas[i].println("ESPERA");
                }

                new Thread(new ManejadorCliente(i)).start();
            }

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ManejadorCliente implements Runnable {
        private int jugadorID;

        public ManejadorCliente(int jugadorID) {
            this.jugadorID = jugadorID;
        }

        @Override
        public void run() {
            try {
                String entrada;
                while ((entrada = entradas[jugadorID].readLine()) != null) {
                    if (entrada.startsWith("JUGADA")) {
                        String[] partes = entrada.split(",");
                        int fila = Integer.parseInt(partes[1]);
                        int columna = Integer.parseInt(partes[2]);
                        if (realizarJugada(fila, columna, jugadorID)) {
                            int estado = verificarEstado();
                            enviarEstadoJuego();
                            if (estado != -1) {
                                if (estado == 0) {
                                    enviarMensajeATodos("EMPATE");
                                } else {
                                    enviarMensajeATodos("GANADOR");
                                }
                            }
                        } else {
                            salidas[jugadorID].println("INVALIDO");
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error de E/S en el cliente " + jugadorID + ": " + e.getMessage());
            } finally {
                try {
                    if (jugadores[jugadorID] != null) {
                        jugadores[jugadorID].close();
                    }
                } catch (IOException e) {
                    System.err.println("Error al cerrar la conexión del cliente " + jugadorID + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean realizarJugada(int fila, int columna, int jugadorID) {
        if (tablero[fila][columna].isEmpty()) {
            tablero[fila][columna] = jugadorID == 0 ? "X" : "O";
            return true;
        }
        return false;
    }

    private int verificarEstado() {
        // Verificar filass
        for (int i = 0; i < 3; i++) {
            if (!tablero[i][0].isEmpty() && tablero[i][0].equals(tablero[i][1]) && tablero[i][0].equals(tablero[i][2])) {
                return tablero[i][0].equals("X") ? 0 : 1; // Si X gana, retorna 0; si O gana, retorna 1
            }
        }

        // Verificar columnas
        for (int j = 0; j < 3; j++) {
            if (!tablero[0][j].isEmpty() && tablero[0][j].equals(tablero[1][j]) && tablero[0][j].equals(tablero[2][j])) {
                return tablero[0][j].equals("X") ? 0 : 1;
            }
        }

        // Verificar diagonales
        if (!tablero[0][0].isEmpty() && tablero[0][0].equals(tablero[1][1]) && tablero[0][0].equals(tablero[2][2])) {
            return tablero[0][0].equals("X") ? 0 : 1;
        }
        if (!tablero[0][2].isEmpty() && tablero[0][2].equals(tablero[1][1]) && tablero[0][2].equals(tablero[2][0])) {
            return tablero[0][2].equals("X") ? 0 : 1;
        }

        // Verificar empate
        boolean empate = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j].isEmpty()) {
                    empate = false;
                    break;
                }
            }
            if (!empate) {
                break;
            }
        }
        return empate ? 2 : -1; // Si hay empate, retorna 2; de lo contrario, retorna -1 (no hay resultado aún)
    }

    private void enviarEstadoJuego() {
        StringBuilder estado = new StringBuilder("ESTADO");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                estado.append(",").append(tablero[i][j]);
            }
        }
        enviarMensajeATodos(estado.toString());
    }

    private void enviarMensajeATodos(String mensaje) {
        for (PrintWriter salida : salidas) {
            if (salida != null) {
                salida.println(mensaje);
            }
        }
    }

    public static void main(String[] args) {
        new ServidorTresEnRaya();
    }
}
