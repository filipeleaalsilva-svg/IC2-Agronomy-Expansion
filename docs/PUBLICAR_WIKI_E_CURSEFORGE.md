# Guia de Publicação: GitHub Wiki + CurseForge

Este guia reúne os passos exatos para publicar a documentação gerada em `docs/github-wiki/` e criar/atualizar a página em `docs/curseforge/`.

---

## Parte 1 — Publicar a GitHub Wiki Nativa (`.wiki.git`)

> **Pré-requisito único no GitHub:** se a aba **Wiki** do repositório `IC2-Agronomy-Expansion` ainda nunca foi usada, abra `https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion/wiki` no navegador e clique em **"Create the first page"** → **Save Page**. Isso inicializa o repositório Git `.wiki.git` no servidor do GitHub.

Depois disso, execute no PowerShell a partir da raiz do projeto:

```powershell
# 1. Clonar o repositório da Wiki em uma pasta temporária fora do repo principal
git clone https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion.wiki.git ..\IC2-Agronomy-Expansion-wiki

# 2. Copiar todas as páginas modulares (.md) para o clone da Wiki
Copy-Item -Path "docs\github-wiki\*.md" -Destination "..\IC2-Agronomy-Expansion-wiki\" -Force

# 3. Enviar para o GitHub Wiki
cd ..\IC2-Agronomy-Expansion-wiki
git add .
git commit -m "docs: publicar wiki completa v0.5.0-alpha11"
git push origin master
```

> **Dica:** sempre que atualizar alguma página em `docs/github-wiki/`, repita os passos `2` e `3` (e faça `git pull` antes caso tenha editado algo direto pelo navegador).

---

## Parte 2 — Criar / Publicar a Página no CurseForge

Siga o passo a passo completo em [`docs/curseforge/CHECKLIST_CURSEFORGE.md`](curseforge/CHECKLIST_CURSEFORGE.md), utilizando:
- **Logo `512×512`:** [`docs/curseforge/curseforge-logo-512.png`](curseforge/curseforge-logo-512.png) (ou `docs/artwork/curseforge/logo/curseforge_avatar_512.png` / `1024.png` junto com `banner/`, `gallery/` e `sections/`)
- **Descrição Principal (Inglês):** [`docs/curseforge/CURSEFORGE_DESCRIPTION_EN.md`](curseforge/CURSEFORGE_DESCRIPTION_EN.md) (ou template ilustrado [`docs/curseforge/CURSEFORGE_PAGE_TEMPLATE.md`](curseforge/CURSEFORGE_PAGE_TEMPLATE.md))
- **Descrição PT-BR (opcional abaixo da EN):** [`docs/curseforge/CURSEFORGE_DESCRIPTION_PT_BR.md`](curseforge/CURSEFORGE_DESCRIPTION_PT_BR.md)
- **Changelog da versão:** [`docs/curseforge/CURSEFORGE_CHANGELOG.md`](curseforge/CURSEFORGE_CHANGELOG.md)
- **Artefato principal (GUI padrão clara do IC2):** `build/libs/IC2-Agronomy-Expansion-0.5.0-alpha11.jar`
- **Additional File (Tema Escuro `#1C1C1D`, `pack_format: 3`, 20 GUIs):** `build/libs/IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip` *(também disponível na raiz e em `dist/`)*
