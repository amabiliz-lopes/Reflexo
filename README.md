# Reflexo App

Aplicativo Android de teste de tempo de reação desenvolvido para o trabalho final da disciplina de Desenvolvimento para Dispositivos Móveis.

## Objetivo

O projeto simula um jogo de reação em que o usuário deve aguardar a mudança de cor e tocar o mais rápido possível. A aplicação registra o melhor tempo, salva o histórico em SQLite e coleta dados do acelerômetro e da localização do dispositivo.

## Funcionalidades implementadas

- View customizada com feedback visual em tempo real
- Jogo de reação com estados: idle, aguardando, pronto e resultado
- Detecção de movimento por acelerômetro
- Geolocalização com permissões do Android
- Armazenamento do histórico em SQLite
- Exibição do melhor tempo e histórico de registros
- Botão de limpar histórico
- Uso de layouts XML nativos do Android

## Requisitos atendidos

- View customizada: implementada em `ReactionView`
- Banco de dados: SQLite em `BancoDados.java`
- Geolocalização: uso de `LocationManager` e permissões em `AndroidManifest.xml`
- Sensores: leitura do sensor de aceleração em `MainActivity.java`
- GitHub Actions: workflow adicionado em `.github/workflows/android.yml`

## Como executar

1. Abra o projeto no Android Studio.
2. Sincronize o Gradle.
3. Conecte um dispositivo Android ou use um emulador.
4. Execute a aplicação no modo debug.

## Estrutura principal

- `app/src/main/java/com/dpdm/reflexoapp/MainActivity.java` — navegação, sensores, GPS e eventos do jogo
- `app/src/main/java/com/dpdm/reflexoapp/ReactionView.java` — view customizada do jogo
- `app/src/main/java/com/dpdm/reflexoapp/GameViewModel.java` — lógica do estado do jogo
- `app/src/main/java/com/dpdm/reflexoapp/BancoDados.java` — persistência com SQLite
- `app/src/main/res/layout/` — layouts XML das telas
- `.github/workflows/android.yml` — compilação automática e geração do APK

## Observações

- O aplicativo continua funcionando mesmo quando a permissão de localização é negada.
- O sensor de acelerômetro é opcional conforme o hardware do dispositivo.
- O histórico pode ser limpo diretamente pela interface.

## Validação e APK

O workflow de GitHub Actions compila o projeto em modo debug e publica o APK como artefato do pipeline.

> A geração do APK depende do ambiente do CI e da presença do SDK do Android configurado corretamente no workflow.

