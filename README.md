# OkoChat Bridge for NeoForge

おこ鯖のVelocity版OkoChat（プラグイン名LunaChat）へ、NeoForgeサーバーの発言を接続するサーバー専用MODです。

Minecraft **26.2** / NeoForge **26.2.0.88** / Java **25** を対象にしています。クライアントへの追加MODは不要です。

## 機能

- チャット本文、表示名と色・装飾、接頭辞・接尾辞、UUID、ワールド、ブロック座標を、Paper版OkoChatBridgeと共通の通信形式で送信します。
- チャンネル、クイック発言、個別メッセージ、日本語変換、権限・ミュートの判定、Discord連携は、現在稼働しているVelocity側の処理と設定を使います。これらの機能をこのMODで再実装しません。
- 接頭辞・接尾辞は、既存OkoChatのLuckPerms連携コードで取得します。LuckPermsがない場合は空欄です。
- 転送できないときは送信者にエラーを表示し、サーバー内の通常チャットへは配信しません。
- 別MODが既にキャンセルした発言は転送しません。

## 導入

1. 対象サーバーを停止して、`OkoChatBridge-NeoForge-0.1.0.jar` を `mods/` へ置きます。
2. 現在のおこ鯖と同じ、OkoChat-Legacyが稼働するVelocityから接続します。専用チャンネルは `okochat:messaging` です。
3. 称号もmainと揃える場合は、対象のMinecraft版に対応するLuckPermsのNeoForge版を別途導入し、mainと同じユーザー・グループのメタデータと、適切なサーバーコンテキストを利用できるよう設定します。Vault由来の独自表示や別プラグインが変更する表示名は、その生成元の対応も必要です。
4. 対象サーバーを起動し、通常発言、チャンネル切替、個別メッセージ、日本語変換、mainとDiscord双方への到達、重複配信がないことを確認します。

Velocityのプラグインチャンネルが登録されていない接続では、発言の転送を拒否してログに理由を出します。NeoForgeへの直接接続だけではこのMODのチャット機能は使えません。

Paper版のPlaceholderAPI用 `%okochat_default_channel%` / `%okochat_version%` はPaper側の他プラグイン向け機能です。このMODはPlaceholderAPIやmainのスコアボードを移植しません。チャンネルの状態とコマンドはVelocity側を共用します。

## ビルド

```sh
git clone --recurse-submodules https://github.com/okocraft/OkoChatBridge-NeoForge.git
cd OkoChatBridge-NeoForge
./gradlew build
```

Windowsでは `gradlew.bat build` を使い、`JAVA_HOME` にJDK 25を指定してください。成果物は `build/libs/` に生成されます。

既にclone済みでサブモジュールが空の場合は `git submodule update --init --recursive` を実行してください。

## 既存コードの利用

`upstream/OkoChat` は元リポジトリを変更せずに参照するGitサブモジュールです。`bridge-protocol`、`integration`、`integration-luckperms` のJavaソースをそのままコンパイルします。NeoForge固有のイベント、Component変換、パケットの受け渡しだけをこのリポジトリに実装しています。

Adventureはjar内に同梱されます。LuckPermsと既存Velocityプラグインは同梱されません。ライセンスはGPL-3.0です。帰属は [NOTICE.md](NOTICE.md) を参照してください。
