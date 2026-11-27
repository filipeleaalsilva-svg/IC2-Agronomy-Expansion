# IC2: Agronomy Expansion

**IC2: Agronomy Expansion** é um addon completo de automação agrícola, genética, logística e energia sem fio para **Minecraft 1.12.2** e **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.

Ele transforma o cultivo e o cruzamento de plantas do IC2 em um sistema industrial coeso: cuidado e colheita em área 3D, telemetria remota com regras de histerese, hidroponia em ambiente fechado, química de Weed-Ex, ecossistema movido a Biogás, multiblocos `3×5×3`, rede Wi-Fi de EU, armazenamento indexado de 500 sementes e **15 espécies customizadas de crops IC2**.

---

## Requisitos

* **Minecraft:** `1.12.2`
* **Mod Loader:** Minecraft Forge (ou Cleanroom `1.12.2`)
* **Dependência Obrigatória:** **IndustrialCraft 2 Experimental** (`ic2@[2.8.222-ex112]`)
* **Integração Opcional:** **HEI / JEI** (pesquisa de `Seed Bags` por espécie + aba de cultivos do mod)
* **Java:** Java 8+

---

## Principais Funcionalidades

### 1. Automação Agrícola 3D e Monitoramento Remoto
* **Crop Manager Avançado:** máquina Tier 2 (`100.000 EU`, tanques de `20.000 mB` de Água e Weed-Ex) que aduba, hidrata, aplica Weed-Ex, remove ervas daninhas (com o **Upgrade Weed Killer**) e colhe cultivos IC2 em área configurável de `3×3` até `17×17` (em até 3 camadas verticais + deslocamento 3D). Inclui censo ambiental ao vivo e filtro genético.
* **Monitor Agrícola e Sonda Agrícola:** vincule máquinas com a **Sonda** (alcance base `32` blocos, até `512` com **Upgrades de Alcance Wireless**). Controle operações à distância, transfira colheitas sem fio, distribua **EU Wireless**, emita alarmes Redstone e configure regras automáticas de parada com trava de histerese em `100%` de Weed-Ex.

### 2. Irrigação, Estufas e Câmara Hidropônica
* **Regadores Agrícolas (Mk1–Mk3) e Regador Diagonal:** regadores 3D sem EU cobrindo `9×9` (`Mk1/Mk2`), `17×17` (`Mk3`, velocidade `4×`) ou 4 quadrantes `9×9` independentes com corredor seco em cruz (**Regador Diagonal**).
* **Vidro de Estufa e Decorativos:** o **Vidro de Estufa** acelera cultivos jovens abaixo quando há luz `>= 9`. Inclui **Armação de Estufa**, **Treliça de Jardim** e 4 luminárias nível 15 (**Lanterna Agrícola**, **Arandela de Madeira**, **Lanterna Industrial** e **Luminária de Vidro**).
* **Câmara Hidropônica:** máquina Tier 2 EU (`50.000 EU`, `16 EU/t`, tanque `16.000 mB`) que cultiva `IC2 Seed Bags` em ambiente fechado (`VirtualCropTile`, contendo a radiação da **Erva-Rádio**). Exige Vidro de Estufa acima e subsolo válido abaixo, com bônus opcionais de cúpula `3×3` (`+25%`) e calor IC2 adjacente (`+50%`).

### 3. Química, Compostagem e Ecossistema a Biogás
* **Composteiras Manual e Elétrica:** a **Composteira Manual** (0 EU, `2×` com água) gera **Composto Orgânico** no início de jogo; a **Composteira Elétrica** converte itens orgânicos (incluindo `Weed` IC2, `Bio-Fibra` e `Essência Nitro`) + água em **Fertilizante IC2**.
* **Linha de Produção de Weed-Ex:** **Moedor de Grin** (com **Lâmina de Moagem** de `256` usos) → **Extrator de Grin** → **Misturador de Weed-Ex** (`IC2 Weed-Ex`).
* **Máquinas a Biogás e Calor:**
  * **Capinador a Biogás:** remove ervas daninhas sem EU (`16.000 mB` Biogás) e gera o item `Weed` IC2.
  * **Pulverizador a Biogás:** hidrata e aduba em área `9×9` sem EU.
  * **Queimador Térmico a Biogás e Tubo Térmico:** fonte `IHeatSource` de `32 HU/t` e tubos térmicos de até `100 HU/t`.
  * **Gerador a Biogás Agrícola:** multibloco oco `3×3×3` que gera `32 EU/t` (Tier 1) a partir de Biogás IC2.

### 4. Genética, Banco de 500 Sementes e 15 Cultivos Customizados
* **Analisador de Sementes e Ferramenta de Cruzamento:** analisa `Seed Bags` até nível `4/4` com visor OLED e permite colher e rearmar estacas duplas em um clique.
* **Colmeia Polinizadora e Pó Mutagênico:** acelera cruzamentos em estacas duplas (`3×` mais rápida quando carregada).
* **Baú de Dados de Cultivos e Database Portátil:** `500` slots com busca instantânea, ordenação, Favoritos (`Botão do Meio` / `Shift+R`), coleta magnética e vínculo interdimensional via **Upgrade Linker**.
* **Manual Agrícola:** guia ilustrado in-game com busca e receitas clicáveis.
* **15 Espécies Customizadas de Crops IC2:**
  * *Suporte e Recursos:* **Erva-Grin**, **Cana-Biomassa** (`Bio-Fibra` plantável direto nas estacas), **Nitro-Bulbo**, **Lúmen-Flor**, **Erva-Enxofre**, **Cana-Resina**, **Lótus-Gelo**.
  * *Catalisados (exigem **Fertilizante Catalítico** no estágio 2+):* **Flor Lazúli**, **Quartzo-Cana**, **Musgo de Cobre** (`ore:blockCopper`), **Erva de Estanho** (`ore:blockTin`), **Lítio-Vinha**, **Cana-Volt** (`Bloco de Redstone`), **Erva-Rádio** (`Terra Radioativa` + radiação IC2) e **Musgo de Irídio**.

### 5. Multiblocos `3×5×3` e Rede Wi-Fi de EU
* **Reservatório Agrícola:** multibloco `3×5×3` com 2 tanques, energia Tier 1, filtros e módulos de expansão.
* **Reservatório de Fluido Industrial e Silo Agrícola de Grãos:** armazenamento em massa sem EU (`128.000` base até `384.000` com 8 expansões), portas por face, auto-extração Redstone e comparador nas carcaças.
* **Tubo de Fluidos:** transporte sem EU com equalização segura entre tanques.
* **Rede Wi-Fi de EU e Antena Parabólica:** `1.000` canais públicos/privados (`100.000 EU` de buffer, saída `T1–T4`) e distribuição sem fio por área para **Upgrades Wireless EU** (compatíveis com máquinas do mod e do IC2).

---

## Resource Pack Opcional (Tema Escuro)

O JAR principal do mod traz a interface clara clássica do IC2 como padrão. Prefere interfaces industriais escuras (`#1C1C1D`)? Baixe **`IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip`** (`pack_format: 3`, `"IC2: Agronomy Expansion - Dark Theme (#1C1C1D)"`, ícone `pack.png` estilizado e 20 texturas de GUI remapeadas) na aba **Additional Files**, coloque em `resourcepacks/` e ative no jogo — a cor dos textos se adapta automaticamente.

---

## Instalação e Migração

1. Instale **Minecraft 1.12.2** com Forge (ou Cleanroom) e **IC2 Experimental `2.8.222-ex112`**.
2. Coloque apenas `IC2-Agronomy-Expansion-0.5.0-alpha11.jar` na pasta `mods/`.
3. Cliente e servidor devem usar exatamente a mesma versão.
4. Faça backup do mundo antes de atualizar versões alpha; blocos legados migram ou devolvem itens automaticamente com segurança.

---

## Links e Idiomas

* **Wiki Oficial:** [GitHub Wiki](https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion/wiki)
* **Código-Fonte e Issues:** [Repositório GitHub](https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion)
* **Idiomas:** Português - Brasil (`pt_br`) e Inglês (`en_us`)
* **Licença:** MIT
