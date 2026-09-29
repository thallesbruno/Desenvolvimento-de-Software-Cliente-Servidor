# Desenvolvimento de Software Cliente-Servidor

Aplicação Java que demonstra comunicação TCP com um servidor, um cliente de console e um cliente gráfico JavaFX. O Maven gerencia o build e as dependências.

## Requisitos

- JDK 17 ou superior (o projeto gera classes para Java 17).
- Maven instalado e disponível no terminal.
- Internet no primeiro build para baixar plugins e dependências, incluindo JavaFX.

Confira `java -version` e `mvn -version`: ambos devem utilizar um JDK compatível. Não é necessário instalar o SDK do JavaFX manualmente.

## Estrutura

```text
pom.xml
README.md
.gitignore
slides/                            Material das aulas
src/
  main/
    java/br/edu/disciplina/
      cliente/                     ClienteConsole.java e ClienteGUI.java
      servidor/                    Servidor.java
      protocolo/                   Protocolo.java
    resources/                     Recursos da aplicação (vazio inicialmente)
  test/
    java/                          Testes (vazio inicialmente)
    resources/                     Recursos dos testes (vazio inicialmente)
target/                            Classes e JAR gerados pelo Maven
```

As pastas vazias são pontos de extensão; o Git só as preserva quando contêm arquivos. O diretório `target/` é ignorado pelo Git e recriado pelo build.

## Compilar

Execute todos os comandos a partir da raiz do projeto, onde está o `pom.xml`:

```powershell
mvn clean verify
```

O artefato será gerado em `target/poc-cliente-servidor-1.0.jar`. Ainda não há testes automatizados no projeto. O JAR não inclui as dependências nem define uma única classe principal; use os comandos abaixo para executar cada componente.

## Executar

Em um terminal, inicie o servidor:

```powershell
java -cp target/classes br.edu.disciplina.servidor.Servidor
```

Em outro terminal, inicie o cliente de console:

```powershell
java -cp target/classes br.edu.disciplina.cliente.ClienteConsole
```

Para abrir o cliente gráfico, execute em um terminal separado:

```powershell
mvn javafx:run
```

O servidor atende na porta `5050`. O cliente de console conecta a `localhost:5050`; na interface gráfica, preencha o host e a porta e clique em **Conectar**.

Exemplos de comandos para o console ou o campo de comando livre da interface:

```text
ECO ola mundo
HORA
SOMA 2 3
MAIOR 7 4
PAR 9
SAIR
```

`SAIR` encerra a conexão do cliente. Use `Ctrl+C` no terminal do servidor para encerrá-lo.
