package br.edu.disciplina.servidor;

import br.edu.disciplina.protocolo.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor TCP da POC.
 *
 * Melhorias em relacao a primeira versao:
 *   - Atende varios clientes ao mesmo tempo (uma thread por cliente).
 *   - Protocolo com cinco comandos, alem de SAIR (era so eco).
 *   - Valida cada mensagem recebida e responde com ERRO em vez de travar
 *     ou derrubar o servidor quando o cliente manda algo invalido.
 *   - Um erro em um cliente (numero invalido, queda de conexao) nao afeta
 *     os demais clientes conectados.
 */
public class Servidor {

    private static final AtomicInteger CONTADOR_CLIENTES = new AtomicInteger(0);

    public static void main(String[] args) {
        int porta = Protocolo.PORTA_PADRAO;
        log("Servidor aguardando conexoes na porta " + porta + "...");

        try (ServerSocket servidor = new ServerSocket(porta)) {
            while (true) {
                Socket socketCliente = servidor.accept();
                int id = CONTADOR_CLIENTES.incrementAndGet();
                log("Cliente #" + id + " conectado (" + socketCliente.getInetAddress() + ")");
                Thread thread = new Thread(new ClienteHandler(socketCliente, id), "cliente-" + id);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Nao foi possivel iniciar o servidor na porta " + porta + ": " + e.getMessage());
        }
    }

    /** Log com hora, sincronizado para nao embaralhar linhas de threads diferentes. */
    static synchronized void log(String mensagem) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + hora + "] " + mensagem);
    }

    /**
     * Atende um unico cliente em sua propria thread: le uma linha, valida,
     * processa e responde, ate SAIR ou a conexao cair.
     */
    private static class ClienteHandler implements Runnable {

        private final Socket socketOriginal;
        private final int id;

        ClienteHandler(Socket socket, int id) {
            this.socketOriginal = socket;
            this.id = id;
        }

        @Override
        public void run() {
            try (Socket socket = socketOriginal;
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter saida = new PrintWriter(socket.getOutputStream(), true)) {

                String linha;
                while ((linha = entrada.readLine()) != null) {
                    log("Cliente #" + id + " enviou: \"" + linha + "\"");
                    String resposta = processar(linha.trim());
                    saida.println(resposta);
                    log("Cliente #" + id + " recebeu: \"" + resposta + "\"");
                    if (resposta.equals(Protocolo.OK + " " + Protocolo.SAIR + " TCHAU")) {
                        break;
                    }
                }
            } catch (IOException e) {
                log("Cliente #" + id + " - conexao encerrada com erro: " + e.getMessage());
            } finally {
                log("Cliente #" + id + " desconectado");
            }
        }

        /** Valida e executa um comando, sempre devolvendo uma unica linha de resposta. */
        private String processar(String linhaOriginal) {
            if (linhaOriginal.isEmpty()) {
                return Protocolo.ERRO + " COMANDO_VAZIO";
            }

            String[] partes = linhaOriginal.split("\\s+");
            String comando = partes[0].toUpperCase();

            try {
                switch (comando) {
                    case Protocolo.ECO:
                        return processarEco(linhaOriginal, comando, partes);
                    case Protocolo.HORA:
                        return Protocolo.OK + " " + Protocolo.HORA + " "
                                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    case Protocolo.SOMA:
                        return processarSoma(partes);
                    case Protocolo.MAIOR:
                        return processarMaior(partes);
                    case Protocolo.PAR:
                        return processarPar(partes);
                    case Protocolo.SAIR:
                        return Protocolo.OK + " " + Protocolo.SAIR + " TCHAU";
                    default:
                        return Protocolo.ERRO + " COMANDO_DESCONHECIDO " + comando;
                }
            } catch (NumberFormatException e) {
                return Protocolo.ERRO + " NUMERO_INVALIDO";
            }
        }

        private String processarEco(String linhaOriginal, String comando, String[] partes) {
            if (partes.length < 2) {
                return Protocolo.ERRO + " ARGUMENTOS_INVALIDOS use ECO <mensagem>";
            }
            String mensagem = linhaOriginal.substring(comando.length()).trim();
            return Protocolo.OK + " " + Protocolo.ECO + " " + mensagem.toUpperCase();
        }

        private String processarSoma(String[] partes) {
            if (partes.length != 3) {
                return Protocolo.ERRO + " ARGUMENTOS_INVALIDOS use SOMA <a> <b>";
            }
            double a = Double.parseDouble(partes[1]);
            double b = Double.parseDouble(partes[2]);
            return Protocolo.OK + " " + Protocolo.SOMA + " " + formatar(a + b);
        }

        private String processarMaior(String[] partes) {
            if (partes.length != 3) {
                return Protocolo.ERRO + " ARGUMENTOS_INVALIDOS use MAIOR <a> <b>";
            }
            double a = Double.parseDouble(partes[1]);
            double b = Double.parseDouble(partes[2]);
            return Protocolo.OK + " " + Protocolo.MAIOR + " " + formatar(Math.max(a, b));
        }

        private String processarPar(String[] partes) {
            if (partes.length != 2) {
                return Protocolo.ERRO + " ARGUMENTOS_INVALIDOS use PAR <numero_inteiro>";
            }
            long n = Long.parseLong(partes[1]);
            return Protocolo.OK + " " + Protocolo.PAR + " " + (n % 2 == 0 ? "PAR" : "IMPAR");
        }

        /** Evita mostrar "4.0" quando o resultado eh inteiro. */
        private String formatar(double valor) {
            if (!Double.isInfinite(valor) && !Double.isNaN(valor) && valor == Math.floor(valor)) {
                return String.valueOf((long) valor);
            }
            return String.valueOf(valor);
        }
    }
}
