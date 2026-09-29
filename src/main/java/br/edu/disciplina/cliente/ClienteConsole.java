package br.edu.disciplina.cliente;

import br.edu.disciplina.protocolo.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente de console, mantido como opcao simples para testar o servidor
 * sem depender do JavaFX. Envie comandos como:
 *   ECO ola mundo
 *   HORA
 *   SOMA 2 3
 *   MAIOR 7 4
 *   PAR 9
 *   SAIR
 */
public class ClienteConsole {

    public static void main(String[] args) {
        String host = "localhost";
        int porta = Protocolo.PORTA_PADRAO;

        try (Socket socket = new Socket(host, porta);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado a " + host + ":" + porta
                    + ". Comandos: ECO, HORA, SOMA, MAIOR, PAR, SAIR");
            String msg;
            do {
                System.out.print("> ");
                msg = teclado.nextLine();
                saida.println(msg);
                String resposta = entrada.readLine();
                if (resposta == null) {
                    System.out.println("Servidor encerrou a conexao.");
                    break;
                }
                System.out.println("Servidor: " + resposta);
            } while (!msg.equalsIgnoreCase(Protocolo.SAIR));

        } catch (ConnectException e) {
            System.err.println("Nao foi possivel conectar a " + host + ":" + porta
                    + ". O servidor esta em execucao?");
        } catch (IOException e) {
            System.err.println("Erro de comunicacao: " + e.getMessage());
        }
    }
}
