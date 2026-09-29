package br.edu.disciplina.protocolo;

/**
 * Constantes do protocolo de aplicacao usado pela POC cliente-servidor.
 *
 * Cada mensagem eh uma linha de texto terminada em quebra de linha.
 * O cliente sempre envia primeiro; o servidor sempre responde uma linha
 * comecando por OK (sucesso) ou ERRO (falha de validacao).
 *
 * Comandos suportados:
 *   ECO <mensagem>        -> OK ECO <mensagem em maiusculas>
 *   HORA                  -> OK HORA <hh:mm:ss>
 *   SOMA <a> <b>          -> OK SOMA <a + b>
 *   MAIOR <a> <b>         -> OK MAIOR <o maior dos dois>
 *   PAR <n>               -> OK PAR PAR  |  OK PAR IMPAR
 *   SAIR                  -> OK SAIR TCHAU   (servidor encerra a conexao)
 *
 * Erros possiveis:
 *   ERRO COMANDO_VAZIO
 *   ERRO COMANDO_DESCONHECIDO <comando>
 *   ERRO ARGUMENTOS_INVALIDOS <uso esperado>
 *   ERRO NUMERO_INVALIDO
 */
public final class Protocolo {

    private Protocolo() {
        // classe utilitaria: apenas constantes, nao deve ser instanciada
    }

    public static final int PORTA_PADRAO = 5050;

    // Comandos
    public static final String ECO = "ECO";
    public static final String HORA = "HORA";
    public static final String SOMA = "SOMA";
    public static final String MAIOR = "MAIOR";
    public static final String PAR = "PAR";
    public static final String SAIR = "SAIR";

    // Prefixos de resposta
    public static final String OK = "OK";
    public static final String ERRO = "ERRO";
}
