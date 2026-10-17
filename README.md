# 🏰 Terra Imperium

Um jogo de estratégia 4X por turnos em grade para Android, construído com Kotlin e Jetpack Compose. Conquiste territórios, gerencie colheitas conforme as estações do ano, recrute tropas e enfrente reinos rivais.

---

## 🚀 Integração com GitHub Actions (CI/CD)

O projeto conta com uma esteira de automação contínua configurada em [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml).

### ⚙️ O que a Pipeline Executa:

1. **Testes Unitários & Robolectric**:
   - Roda `./gradlew :app:testDebugUnitTest`.
   - Gera e exporta relatórios de testes (`HTML/XML`) em caso de sucesso ou falha como artefato de download no GitHub (`unit-test-results`).

2. **Compilação do APK**:
   - Constrói o APK Debug (`./gradlew :app:assembleDebug`).
   - Disponibiliza o arquivo `.apk` pronto para download nos artefatos da execução (`terra-imperium-debug-apk`).

3. **Publicação Automática de Releases**:
   - Ao criar e enviar uma tag de versão (ex: `v1.0.0`), a pipeline compila os binários e cria automaticamente uma Release no repositório GitHub com os arquivos para download.

### 📌 Gatilhos de Execução:
- **Push** nas branches `main` e `master`
- **Pull Requests** direcionados para `main` e `master`
- **Tags** no formato `v*` (ex: `v1.0.1`)
- **Execução Manual** via aba **Actions** no GitHub (`workflow_dispatch`)
