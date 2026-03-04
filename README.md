# 📱 Telegram Video Streamer

App Android em Kotlin que permite fazer login no Telegram, navegar pelos seus chats e fazer **streaming de vídeos** diretamente do aplicativo.

[![Build Android APK](https://github.com/deivid22srk/TelegramVideoStreamer/actions/workflows/build.yml/badge.svg)](https://github.com/deivid22srk/TelegramVideoStreamer/actions/workflows/build.yml)

---

## ✨ Funcionalidades

- **Login com número de telefone** — Autenticação real via MTProto (TDLib)
- **Código de verificação** — O Telegram envia o código por SMS ou pelo próprio app
- **Senha de dois fatores (2FA)** — Suporte completo à verificação em duas etapas
- **Lista de chats** — Exibe todos os chats, grupos e canais do usuário
- **Pesquisa de chats** — Filtra conversas pelo nome
- **Grade de vídeos** — Lista todos os vídeos de um chat com thumbnail e duração
- **Streaming de vídeo** — Reprodução progressiva com ExoPlayer (Media3)
- **Controles completos** — Play/pause, seek, avançar/retroceder 10s, tela cheia
- **Paginação** — Carregamento incremental de vídeos ao rolar a lista

---

## 🏗️ Arquitetura

O projeto segue a arquitetura **MVVM** com as seguintes camadas:

```
app/
├── data/
│   ├── model/          # Modelos de domínio (ChatItem, VideoItem)
│   └── repository/     # TelegramClient (TDLib) + TelegramRepository
├── di/                 # Módulos Hilt para injeção de dependências
├── ui/
│   ├── login/          # LoginFragment, CodeFragment, PasswordFragment
│   ├── chats/          # ChatsFragment + ChatsAdapter
│   ├── videos/         # VideosFragment + VideosAdapter
│   └── player/         # PlayerFragment + VideoPlayerService
└── util/               # Formatadores de data, tamanho e duração
```

### Stack tecnológico

| Tecnologia | Uso |
|---|---|
| **Kotlin** | Linguagem principal |
| **TDLib (org.drinkless:tdlib)** | API oficial do Telegram |
| **Hilt** | Injeção de dependências |
| **Navigation Component** | Navegação entre telas |
| **ExoPlayer (Media3)** | Streaming de vídeo |
| **Coroutines + Flow** | Programação assíncrona |
| **Glide** | Carregamento de imagens/thumbnails |
| **ViewBinding** | Binding de views |
| **Material Design 3** | Interface do usuário |

---

## ⚙️ Configuração inicial (OBRIGATÓRIA)

Antes de compilar o app, você precisa obter suas credenciais da API do Telegram:

### 1. Obter API_ID e API_HASH

1. Acesse [https://my.telegram.org](https://my.telegram.org)
2. Faça login com seu número de telefone
3. Clique em **"API development tools"**
4. Crie um novo aplicativo preenchendo os campos
5. Copie o **`api_id`** e o **`api_hash`**

### 2. Configurar no projeto

Edite o arquivo `app/build.gradle.kts` e substitua os valores:

```kotlin
buildConfigField("int", "TELEGRAM_API_ID", "\"SEU_API_ID\"")
buildConfigField("String", "TELEGRAM_API_HASH", "\"SEU_API_HASH\"")
```

Ou, de forma mais segura, crie um arquivo `local.properties` na raiz:

```properties
telegram.api_id=SEU_API_ID
telegram.api_hash=SEU_API_HASH
```

> ⚠️ **NUNCA** commite suas credenciais no repositório!

---

## 🚀 Como compilar

### Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android SDK 34

### Compilação local

```bash
# Clone o repositório
git clone https://github.com/deivid22srk/TelegramVideoStreamer.git
cd TelegramVideoStreamer

# Build Debug
./gradlew assembleDebug

# Build Release
./gradlew assembleRelease

# O APK estará em:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

---

## 🤖 GitHub Actions — CI/CD

O projeto inclui um workflow completo em `.github/workflows/build.yml`.

### Triggers automáticos

| Evento | Ação |
|---|---|
| Push em `main` | Build Debug + Release |
| Push em `develop` | Build Debug |
| Pull Request para `main` | Build Debug + Lint |

### Build manual

1. Acesse a aba **Actions** no repositório
2. Selecione o workflow **"Build Android APK"**
3. Clique em **"Run workflow"**
4. Configure as opções:
   - **Tipo de build**: `debug`, `release` ou `both`
   - **Criar Release**: marque para publicar uma release no GitHub
   - **Tag da release**: ex: `v1.0.0`

### Configurar assinatura do APK Release

Para gerar APKs Release assinados, adicione os seguintes **Secrets** no repositório:

1. Vá em **Settings → Secrets and variables → Actions**
2. Adicione os secrets:

| Secret | Descrição |
|---|---|
| `KEYSTORE_BASE64` | Keystore em Base64: `base64 -w 0 release.jks` |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |

---

## 📱 Telas do aplicativo

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Login         │    │   Verificação   │    │   Chats         │
│                 │    │                 │    │                 │
│  [Logo Telegram]│    │  [Ícone código] │    │  🔍 Pesquisar   │
│                 │    │                 │    │                 │
│  +55 11 99999   │───▶│  [ 1 2 3 4 5 ] │───▶│  Chat 1    14:32│
│                 │    │                 │    │  Chat 2    13:15│
│  [Enviar Código]│    │  [Verificar]    │    │  Chat 3    12:00│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
┌─────────────────┐    ┌─────────────────────────────────────────┐
│   Player        │    │   Vídeos do Chat                        │
│                 │    │                                         │
│  ┌───────────┐  │    │  ┌──────────┐  ┌──────────┐            │
│  │           │  │◀───│  │ 🎬 02:34 │  │ 🎬 15:20 │            │
│  │  Vídeo    │  │    │  │  45.3 MB │  │ 120.1 MB │            │
│  │           │  │    │  └──────────┘  └──────────┘            │
│  └───────────┘  │    │  ┌──────────┐  ┌──────────┐            │
│  ◀◀ ⏸ ▶▶       │    │  │ 🎬 08:45 │  │ 🎬 01:12 │            │
└─────────────────┘    └─────────────────────────────────────────┘
```

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## ⚠️ Aviso legal

Este aplicativo utiliza a API oficial do Telegram (TDLib) para fins educacionais e pessoais. Respeite os [Termos de Serviço do Telegram](https://telegram.org/tos) ao usar este aplicativo.
