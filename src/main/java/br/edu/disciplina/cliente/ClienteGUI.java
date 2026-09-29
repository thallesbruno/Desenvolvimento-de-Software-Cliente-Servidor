package br.edu.disciplina.cliente;

import br.edu.disciplina.protocolo.Protocolo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

/**
 * Cliente com interface grafica (JavaFX) para demonstrar, na pratica, a
 * comunicacao via socket com o Servidor da POC.
 *
 * As dependencias do JavaFX sao gerenciadas pelo Maven.
 * Veja instrucoes de execucao no fim deste arquivo e no README.md.
 *
 * A interface:
 *   - Valida host e porta antes de conectar, e cada campo numerico antes
 *     de montar um comando (nao deixa digitar letra onde se espera numero).
 *   - Roda a leitura do socket em uma thread separada, para a janela nunca
 *     travar esperando o servidor responder.
 *   - Mostra o historico da conversa como bolhas de chat, com timestamp,
 *     diferenciando mensagens enviadas e recebidas.
 *   - Reage a erros de conexao (host invalido, servidor fora do ar, queda
 *     de conexao) com mensagens de status, sem travar ou fechar sozinha.
 */
public class ClienteGUI extends Application {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // --- estado da conexao ---
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private volatile boolean conectado = false;

    // --- controles que mudam de estado ao conectar/desconectar ---
    private TextField campoHost;
    private TextField campoPorta;
    private Button botaoConectar;
    private Circle indicadorStatus;
    private Label rotuloStatus;
    private Label rotuloRodape;
    private VBox painelComandos;
    private VBox areaConversa;
    private ScrollPane scrollConversa;

    @Override
    public void start(Stage stage) {
        BorderPane raiz = new BorderPane();
        raiz.setPadding(new Insets(14));
        raiz.setStyle("-fx-background-color: #F4F7FA;");

        raiz.setTop(criarBarraConexao());

        HBox centro = new HBox(14);
        painelComandos = criarPainelComandos();
        VBox areaDireita = criarAreaConversa();
        HBox.setHgrow(areaDireita, Priority.ALWAYS);
        centro.getChildren().addAll(painelComandos, areaDireita);
        BorderPane.setMargin(centro, new Insets(14, 0, 14, 0));
        raiz.setCenter(centro);

        raiz.setBottom(criarRodape());

        atualizarEstadoControles();

        Scene cena = new Scene(raiz, 980, 620);
        stage.setTitle("Cliente Socket \u2013 POC Cliente-Servidor");
        stage.setScene(cena);
        stage.setOnCloseRequest(e -> desconectar());
        stage.show();
    }

    // ------------------------------------------------------------------
    // Construcao da interface
    // ------------------------------------------------------------------

    private HBox criarBarraConexao() {
        Label rotuloHost = new Label("Servidor:");
        campoHost = new TextField("localhost");
        campoHost.setPrefWidth(140);

        Label rotuloPorta = new Label("Porta:");
        campoPorta = new TextField(String.valueOf(Protocolo.PORTA_PADRAO));
        campoPorta.setPrefWidth(70);
        campoPorta.setTextFormatter(criarFormatadorInteiro());

        botaoConectar = new Button("Conectar");
        botaoConectar.setOnAction(e -> {
            if (conectado) {
                desconectar();
            } else {
                conectar();
            }
        });

        indicadorStatus = new Circle(7, Color.web("#9AA6B2"));
        rotuloStatus = new Label("Desconectado");
        rotuloStatus.setStyle("-fx-font-weight: bold;");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox barra = new HBox(10,
                rotuloHost, campoHost, rotuloPorta, campoPorta, botaoConectar,
                espaco, indicadorStatus, rotuloStatus);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(0, 0, 10, 0));
        return barra;
    }

    private VBox criarPainelComandos() {
        VBox painel = new VBox(12);
        painel.setPrefWidth(320);
        painel.setPadding(new Insets(14));
        painel.setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);");

        Label titulo = new Label("Funcionalidades");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        painel.getChildren().addAll(titulo, new Separator(),
                criarLinhaEco(),
                new Separator(),
                criarLinhaHora(),
                new Separator(),
                criarLinhaDoisNumeros("Soma", Protocolo.SOMA),
                new Separator(),
                criarLinhaDoisNumeros("Maior valor", Protocolo.MAIOR),
                new Separator(),
                criarLinhaPar(),
                new Separator(),
                criarLinhaComandoLivre());

        return painel;
    }

    private VBox criarLinhaEco() {
        Label rotulo = new Label("Eco (envia texto, recebe em maiusculas)");
        rotulo.setWrapText(true);
        TextField campo = new TextField();
        campo.setPromptText("mensagem");
        Button botao = new Button("Enviar");
        botao.setOnAction(e -> {
            String texto = campo.getText().trim();
            if (texto.isEmpty()) {
                mostrarErroValidacao("Digite uma mensagem antes de enviar o eco.");
                return;
            }
            enviarComando(Protocolo.ECO + " " + texto);
            campo.clear();
        });
        HBox linha = new HBox(8, campo, botao);
        HBox.setHgrow(campo, Priority.ALWAYS);
        return new VBox(4, rotulo, linha);
    }

    private VBox criarLinhaHora() {
        Label rotulo = new Label("Hora atual do servidor");
        Button botao = new Button("Consultar HORA");
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setOnAction(e -> enviarComando(Protocolo.HORA));
        return new VBox(4, rotulo, botao);
    }

    private VBox criarLinhaDoisNumeros(String titulo, String comando) {
        Label rotulo = new Label(titulo);
        TextField campoA = criarCampoNumerico("a");
        TextField campoB = criarCampoNumerico("b");
        Button botao = new Button("Calcular");
        botao.setOnAction(e -> {
            Double a = lerNumero(campoA, titulo);
            if (a == null) return;
            Double b = lerNumero(campoB, titulo);
            if (b == null) return;
            enviarComando(comando + " " + formatarEntrada(a) + " " + formatarEntrada(b));
        });
        HBox linhaCampos = new HBox(6, campoA, campoB, botao);
        campoA.setPrefWidth(70);
        campoB.setPrefWidth(70);
        HBox.setHgrow(botao, Priority.ALWAYS);
        return new VBox(4, rotulo, linhaCampos);
    }

    private VBox criarLinhaPar() {
        Label rotulo = new Label("Par ou impar");
        TextField campo = criarCampoInteiro();
        campo.setPromptText("numero inteiro");
        Button botao = new Button("Verificar");
        botao.setOnAction(e -> {
            String texto = campo.getText().trim();
            if (texto.isEmpty() || texto.equals("-")) {
                mostrarErroValidacao("Digite um numero inteiro para verificar PAR/IMPAR.");
                return;
            }
            enviarComando(Protocolo.PAR + " " + texto);
        });
        HBox linha = new HBox(8, campo, botao);
        HBox.setHgrow(campo, Priority.ALWAYS);
        return new VBox(4, rotulo, linha);
    }

    private VBox criarLinhaComandoLivre() {
        Label rotulo = new Label("Comando livre (avancado)");
        Tooltip.install(rotulo, new Tooltip(
                "Envia qualquer texto direto ao servidor. Util para mostrar\n"
                        + "como o protocolo responde a comandos invalidos."));
        TextField campo = new TextField();
        campo.setPromptText("ex.: BANANA, ou SAIR");
        Button botao = new Button("Enviar");
        botao.setOnAction(e -> {
            String texto = campo.getText().trim();
            if (texto.isEmpty()) {
                mostrarErroValidacao("Digite um comando antes de enviar.");
                return;
            }
            enviarComando(texto);
            campo.clear();
        });
        HBox linha = new HBox(8, campo, botao);
        HBox.setHgrow(campo, Priority.ALWAYS);
        return new VBox(4, rotulo, linha);
    }

    private VBox criarAreaConversa() {
        areaConversa = new VBox(8);
        areaConversa.setPadding(new Insets(12));

        scrollConversa = new ScrollPane(areaConversa);
        scrollConversa.setFitToWidth(true);
        scrollConversa.setStyle("-fx-background-color: white; -fx-background: white;"
                + " -fx-background-radius: 10;");
        VBox.setVgrow(scrollConversa, Priority.ALWAYS);

        Label titulo = new Label("Conversa com o servidor");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button botaoLimpar = new Button("Limpar historico");
        botaoLimpar.setOnAction(e -> areaConversa.getChildren().clear());

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalho = new HBox(titulo, espaco, botaoLimpar);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(8, cabecalho, scrollConversa);
        VBox.setVgrow(container, Priority.ALWAYS);
        HBox.setHgrow(container, Priority.ALWAYS);
        return container;
    }

    private HBox criarRodape() {
        rotuloRodape = new Label(" ");
        rotuloRodape.setStyle("-fx-text-fill: #C8503C; -fx-font-size: 12px;");
        HBox rodape = new HBox(rotuloRodape);
        rodape.setPadding(new Insets(8, 0, 0, 0));
        return rodape;
    }

    // ------------------------------------------------------------------
    // Validacao de entrada
    // ------------------------------------------------------------------

    private TextField criarCampoNumerico(String prompt) {
        TextField campo = new TextField();
        campo.setPromptText(prompt);
        campo.setTextFormatter(criarFormatadorDecimal());
        return campo;
    }

    private TextField criarCampoInteiro() {
        TextField campo = new TextField();
        campo.setTextFormatter(criarFormatadorInteiro());
        return campo;
    }

    /** Bloqueia, na digitacao, qualquer caractere que nao forme um inteiro valido (com sinal). */
    private TextFormatter<String> criarFormatadorInteiro() {
        UnaryOperator<TextFormatter.Change> filtro = mudanca -> {
            String textoNovo = mudanca.getControlNewText();
            if (textoNovo.isEmpty() || textoNovo.matches("-?\\d*")) {
                return mudanca;
            }
            return null;
        };
        return new TextFormatter<>(filtro);
    }

    /** Bloqueia, na digitacao, qualquer caractere que nao forme um decimal valido (com sinal e ponto). */
    private TextFormatter<String> criarFormatadorDecimal() {
        UnaryOperator<TextFormatter.Change> filtro = mudanca -> {
            String textoNovo = mudanca.getControlNewText();
            if (textoNovo.isEmpty() || textoNovo.matches("-?\\d*(\\.\\d*)?")) {
                return mudanca;
            }
            return null;
        };
        return new TextFormatter<>(filtro);
    }

    /** Le e valida um campo numerico ja filtrado; ainda cobre casos como "" ou "-" sozinho. */
    private Double lerNumero(TextField campo, String contexto) {
        String texto = campo.getText().trim();
        if (texto.isEmpty() || texto.equals("-") || texto.equals(".")) {
            mostrarErroValidacao("Preencha os dois numeros para " + contexto + ".");
            return null;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            mostrarErroValidacao("Numero invalido em " + contexto + ".");
            return null;
        }
    }

    private String formatarEntrada(double valor) {
        if (valor == Math.floor(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }

    private void mostrarErroValidacao(String mensagem) {
        rotuloRodape.setText(mensagem);
    }

    // ------------------------------------------------------------------
    // Rede: conectar, desconectar, enviar, ler em background
    // ------------------------------------------------------------------

    private void conectar() {
        String host = campoHost.getText().trim();
        String textoPorta = campoPorta.getText().trim();

        if (host.isEmpty()) {
            mostrarErroValidacao("Informe o endereco do servidor.");
            return;
        }
        int porta;
        try {
            porta = Integer.parseInt(textoPorta);
            if (porta < 1 || porta > 65535) {
                mostrarErroValidacao("A porta deve estar entre 1 e 65535.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarErroValidacao("Porta invalida.");
            return;
        }

        mostrarErroValidacao(" ");
        definirStatus("Conectando...", "#F2A541");
        botaoConectar.setDisable(true);

        Thread threadConexao = new Thread(() -> {
            try {
                Socket novoSocket = new Socket(host, porta);
                BufferedReader novaEntrada = new BufferedReader(
                        new InputStreamReader(novoSocket.getInputStream()));
                PrintWriter novaSaida = new PrintWriter(novoSocket.getOutputStream(), true);

                this.socket = novoSocket;
                this.entrada = novaEntrada;
                this.saida = novaSaida;
                this.conectado = true;

                Platform.runLater(() -> {
                    definirStatus("Conectado", "#2E9E6B");
                    botaoConectar.setText("Desconectar");
                    botaoConectar.setDisable(false);
                    campoHost.setDisable(true);
                    campoPorta.setDisable(true);
                    atualizarEstadoControles();
                    adicionarMensagemSistema("Conectado a " + host + ":" + porta + ".");
                });

                lerRespostasContinuamente();

            } catch (UnknownHostException e) {
                Platform.runLater(() -> falharConexao("Endereco desconhecido: " + host));
            } catch (ConnectException e) {
                Platform.runLater(() -> falharConexao(
                        "Nao foi possivel conectar a " + host + ":" + porta + ". O servidor esta ativo?"));
            } catch (IOException e) {
                Platform.runLater(() -> falharConexao("Erro ao conectar: " + e.getMessage()));
            }
        }, "thread-conexao");
        threadConexao.setDaemon(true);
        threadConexao.start();
    }

    /** Loop de leitura do socket; roda inteiro fora da UI thread. */
    private void lerRespostasContinuamente() {
        try {
            String linha;
            while (conectado && (linha = entrada.readLine()) != null) {
                String resposta = linha;
                Platform.runLater(() -> adicionarMensagem(resposta, false));
            }
        } catch (IOException e) {
            if (conectado) {
                Platform.runLater(() -> adicionarMensagemSistema(
                        "Conexao perdida: " + e.getMessage()));
            }
        } finally {
            Platform.runLater(this::resetarAposDesconexao);
        }
    }

    private void enviarComando(String comando) {
        if (!conectado || saida == null) {
            mostrarErroValidacao("Conecte-se ao servidor antes de enviar comandos.");
            return;
        }
        mostrarErroValidacao(" ");
        saida.println(comando);
        adicionarMensagem(comando, true);
    }

    private void desconectar() {
        if (conectado) {
            try {
                if (saida != null) saida.println(Protocolo.SAIR);
            } catch (Exception ignorada) {
                // o socket pode ja estar em processo de fechamento; seguimos para liberar os recursos
            }
        }
        fecharRecursos();
        resetarAposDesconexao();
    }

    private void falharConexao(String mensagem) {
        definirStatus("Erro de conexao", "#C8503C");
        mostrarErroValidacao(mensagem);
        botaoConectar.setDisable(false);
        fecharRecursos();
        atualizarEstadoControles();
    }

    private void resetarAposDesconexao() {
        boolean estavaConectado = conectado;
        fecharRecursos();
        campoHost.setDisable(false);
        campoPorta.setDisable(false);
        botaoConectar.setText("Conectar");
        botaoConectar.setDisable(false);
        definirStatus("Desconectado", "#9AA6B2");
        atualizarEstadoControles();
        if (estavaConectado) {
            adicionarMensagemSistema("Conexao encerrada.");
        }
    }

    private void fecharRecursos() {
        conectado = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignorada) {
            // ja estamos encerrando a conexao; nada a fazer com esse erro
        }
        socket = null;
        entrada = null;
        saida = null;
    }

    private void atualizarEstadoControles() {
        painelComandos.setDisable(!conectado);
    }

    private void definirStatus(String texto, String corHex) {
        rotuloStatus.setText(texto);
        indicadorStatus.setFill(Color.web(corHex));
    }

    // ------------------------------------------------------------------
    // Historico de conversa (bolhas de chat)
    // ------------------------------------------------------------------

    private void adicionarMensagem(String texto, boolean enviada) {
        String hora = LocalTime.now().format(HORA_FMT);
        Label bolha = new Label(texto);
        bolha.setWrapText(true);
        bolha.setMaxWidth(520);
        bolha.setPadding(new Insets(8, 12, 8, 12));

        Label rotuloHora = new Label(hora + (enviada ? "  \u00b7  voce" : "  \u00b7  servidor"));
        rotuloHora.setStyle("-fx-font-size: 10px; -fx-text-fill: #5B6B7A;");

        VBox coluna = new VBox(2, bolha, rotuloHora);

        if (enviada) {
            bolha.setStyle("-fx-background-color: #1C7293; -fx-text-fill: white;"
                    + " -fx-background-radius: 12;");
            coluna.setAlignment(Pos.CENTER_RIGHT);
            rotuloHora.setAlignment(Pos.CENTER_RIGHT);
        } else {
            boolean erro = texto.startsWith(Protocolo.ERRO);
            String fundo = erro ? "#FDEAE5" : "#EEF2F5";
            String corTexto = erro ? "#C8503C" : "#16304A";
            bolha.setStyle("-fx-background-color: " + fundo + "; -fx-text-fill: " + corTexto + ";"
                    + " -fx-background-radius: 12;");
            coluna.setAlignment(Pos.CENTER_LEFT);
            rotuloHora.setAlignment(Pos.CENTER_LEFT);
        }

        HBox linha = new HBox(coluna);
        linha.setAlignment(enviada ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        areaConversa.getChildren().add(linha);
        rolarParaFinal();
    }

    private void adicionarMensagemSistema(String texto) {
        Label rotulo = new Label(texto);
        rotulo.setStyle("-fx-font-size: 11px; -fx-text-fill: #5B6B7A; -fx-font-style: italic;");
        HBox linha = new HBox(rotulo);
        linha.setAlignment(Pos.CENTER);
        areaConversa.getChildren().add(linha);
        rolarParaFinal();
    }

    private void rolarParaFinal() {
        Platform.runLater(() -> scrollConversa.setVvalue(1.0));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
 * COMO EXECUTAR ESTE CLIENTE
 * ---------------------------------------------------------------
 * Na raiz do projeto (pasta que contem o pom.xml), execute:
 *
 *    mvn clean verify
 *
 * Inicie o servidor em um terminal separado:
 *
 *    java -cp target/classes br.edu.disciplina.servidor.Servidor
 *
 * Depois, inicie o cliente grafico:
 *
 *    mvn javafx:run
 *
 * Use JDK 17 ou superior. O Maven baixa as dependencias do JavaFX;
 * nao eh necessario instalar o SDK do JavaFX manualmente.
 */
