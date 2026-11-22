# Validação e checklist

Data da revisão atual: 15/11/2026. Esta é uma versão **alpha para teste**.

## Compatibilidade observada: Ender IO 5.5.1

Em 17/11/2026, dois carregamentos do mundo E2EE com Crop Manager 0.5.0-alpha3 falharam na mesma atualização de vizinhos. O stack trace termina em `crazypants.enderio.base.capability.ItemTools.getExternalInventory`, chamado pelo conduíte de itens do Ender IO 5.5.1. A atualização foi disparada por `ic2.core.block.machine.tileentity.TileEntityStandardMachine.onLoaded`. A exceção é `ClassCastException` (`EnumFacing` para `IItemHandler`).

O relatório identifica a coordenada do bundle Ender IO (-197, 68, 241), mas registra o bloco consultado somente como o bloco genérico `ic2:te`, sem o tipo/posição do TileEntity IC2. O usuário informou que ali havia um Advanced Manager com conduítes de energia e de itens, funcionando antes de atualizar para alpha3. O Manager expõe `CapabilityItemHandler` pela API padrão do Forge; ainda não foi possível reproduzir a exceção nem concluir se alpha3 a desencadeia, então não atribuir causa confirmada ao Crop Manager. Reprodução direcionada pendente: Advanced Manager e conduíte de itens do Ender IO adjacente, com o pacote E2EE/Cleanroom e a versão indicada.

Mitigação para recuperar o mundo: fazer cópia do save e remover temporariamente o conduíte de itens adjacente ao Advanced Manager antes de carregar; restaurá-lo depois que o mundo abrir e confirmar as conexões. Preservar primeiro uma cópia intacta do save.

## Revisão 0.5.0-alpha2 — validação concluída

Build, testes unitários, mundo isolado, QA do cliente nos dois idiomas e inspeção do JAR foram repetidos em 15/11/2026. A primeira inspeção visual encontrou chaves de tradução ausentes nos novos controles; as traduções `en_us` e `pt_br` foram adicionadas e ambos os clientes passaram na repetição.

- Build Java 8 e **31 testes JUnit aprovados**.
- Cleanroom 0.6.11-alpha, Minecraft 1.12.2, IC2 2.8.222-ex112, Java 25: **145 verificações aprovadas**. Incluem publicação após ciclo completo, agrupamento de espécies identificadas/desconhecidas, respeito à análise, agregados ambientais reais, sincronização do monitor, NBT antigo sem levantamento, alteração de área e área vazia.
- Cliente real de desenvolvimento: `en_us` e `pt_br`, escalas padrão e 1, layouts sem rótulos cortados/sobreposições, rede do Manager/monitor/coletor, controles remotos e regra de retomada. Culturas, paginação, seleção e Ambiente passaram no QA local.
- JAR: **71 classes Java 8**, **6 blockstates ativos**, **19 modelos**, **41 sprites nativos 16px**, **219 traduções correspondentes**; recursos resolvidos e sem classes de teste. SHA-256: `39ec5ee51b48937d45c1b0b978aee29e0dcfb6e586c462dca15b9a942f34a69c`.
- Capturas e marcadores específicos desta revisão: `evidence/revision-0.5.0-alpha2`. Resultados da alpha1 abaixo permanecem históricos.

Os marcadores `CROP_MANAGER_RUNTIME_TESTS_PASSED checks=145` e `CROP_MANAGER_CLIENT_QA_PASSED` foram confirmados nos logs. Comandos repetidos:

```powershell
.\gradlew.bat build reobfRuntimeTests
.\tools\smoke_cleanroom.ps1 -RunWorldTests
.\gradlew.bat runClient -PclientQa -PclientQaLang=en_us
.\gradlew.bat runClient -PclientQa -PclientQaLang=pt_br
py tools/verify_artifact.py
```

O QA usou o cliente Forge 1.12.2 + IC2 local, com fixture própria; não comprova a instância E2EE completa. Fundos claros/escuros de resource packs não foram repetidos nesta revisão. A checagem automática de layout e as capturas confirmam telas da fixture em escalas padrão/1, mas não substituem inspeção manual em cada escala 1/2, tema, máquina e configuração de pack. Multiplayer com dois jogadores e TPS de fazenda grande também não foram medidos.

```powershell
.\gradlew.bat build reobfRuntimeTests
.\tools\smoke_cleanroom.ps1 -RunWorldTests
.\gradlew.bat runClient -PclientQa -PclientQaLang=en_us
.\gradlew.bat runClient -PclientQa -PclientQaLang=pt_br
py tools/verify_artifact.py
```

---

Histórico de resultados confirmados:

## Revisão 0.5.0-alpha1

Controle remoto do Manager, recursos absolutos no monitor, regras de parada com retomada somente com Weed-Ex cheio, área ajustável e capinador a biogás funcional. Blocos/itens aposentados foram retirados dos registros e recebem remapeamento/migração ao abrir saves antigos.

- **24 testes JUnit** aprovados, incluindo cinco testes das regras: limite estrito, trava até 100%, condição original, combinação dos tanques e persistência.
- **137 verificações no Cleanroom** aprovadas: regressões anteriores, pausa sem custo de varredura, retomada apenas no tanque cheio, desligamento manual prioritário, comandos remotos validados, limites da área, capinador consumindo biogás real, preservação das estacas/crops valiosos e recuperação de 36 stacks legadas sem perda em nove slots mais fila persistente.
- Cliente real em inglês e português: quantidades e fertilizante, abas de controle/automação, ajuste de área, modelo 3D e comandos reais pela rede. Testes rejeitam janela errada e comandos não permitidos; verificam controle remoto, preset e limites da área. Português completo seguido de revisão das etapas 20–26 após polimento final; inglês completo na revisão final.
- Artefato: **66 classes Java 8**, **6 blockstates**, **19 modelos**, **41 sprites** e **206 traduções correspondentes**. Modelos, variantes e referências resolvidos; nenhuma classe de teste ou modelo aposentado no JAR.

Evidências: `evidence/revision-0.5.0`. Os marcadores `CROP_MANAGER_RUNTIME_TESTS_PASSED checks=137` e `CROP_MANAGER_CLIENT_QA_PASSED` são obrigatórios; saída 0 sozinha não comprova aprovação. Repetir com os comandos abaixo. O cliente QA limita processadores e distância de renderização apenas no cenário de teste para evitar falta de memória nativa no Java 8.

```powershell
.\gradlew.bat build reobfRuntimeTests
.\tools\smoke_cleanroom.ps1 -RunWorldTests
.\gradlew.bat runClient -PclientQa -PclientQaLang=en_us
.\gradlew.bat runClient -PclientQa -PclientQaLang=pt_br
py tools/verify_artifact.py
```

Não houve validação do E2EE completo, sessão com dois jogadores nem medição de TPS de fazenda grande. Tubos de outros mods dependem da implementação de capacidade de fluidos de cada mod; foram testadas as capacidades Forge e recipientes IC2 locais. Faça backup antes da atualização alpha.

---

Histórico de validações anteriores:

## Correção de contraste 0.4.0-alpha2

Correção somente visual: cores dos textos e avisos acompanham a luminosidade da textura IC2 realmente carregada. O registro do listener ocorre na inicialização do cliente, e a cor é recalculada ao recarregar recursos.

Build e 19 testes unitários aprovados; inspeção do artefato aprovada. Teste opt-in de cliente aprovado com um resource pack escuro temporário e posterior remoção/recarga: texto `f0f0f0` no fundo escuro e `202020` no fundo claro. Capturas e marcadores em `evidence/contrast-alpha2`.

A bateria de 102 cenários de servidor descrita abaixo pertence à alpha1; não foi repetida para esta correção visual. As novas ideias de controle remoto e área configurável ainda não fazem parte desta versão.

## Revisão 0.4.0-alpha1

Mantidos como conteúdo ativo Manager avançado, monitor, composteira, sonda e as duas novidades: reservatório/coletor. Registros antigos permanecem ocultos para recuperar dados. A migração do Manager básico ocorre ao fim do tick, evitando alteração da lista de entidades durante a abertura do chunk.

- **19 testes JUnit** aprovados.
- **102 verificações no Cleanroom 0.6.11-alpha**, com o IC2 auditado: regressões de cuidados/colheita/EU, migração e conservação do inventário, remoção de receitas/criativo, fertilizante IC2 direto, upgrades reais e persistência da composteira, monitor com Crop-Matron original, modos de sinal, lâmpada real, pulso de 100 ticks reais, reservatório via capacidade de fluidos e coletor com inserção parcial/NBT/propriedade.
- Os quatro novos cenários de borda confirmam que mudanças de atividade, busca de entidades, bombeamento e notificações de redstone não carregam o chunk vizinho. Observadores são notificados apenas em posições carregadas.
- Cliente Forge 1.12.2 + IC2: GUIs em inglês/português, escalas 1 e 2, painel recolhível, recuperação dos blocos legados e testes de rede de Manager, monitor e coletor. Comandos para uma janela incorreta são rejeitados. O teste de cliente aguarda a sincronização das entidades do cenário antes de abrir a primeira GUI.
- O Crop-Matron de referência é posicionado pela implementação original `ItemBlockTileEntity.placeTeBlock`, incluindo estado, eventos e rede do IC2.
- Verificador: **178 traduções correspondentes**, **63 classes Java 8**, **8 blockstates** (cinco ativos e três legados), **28 modelos** e **54 sprites RGBA** de largura 16px, com quadros animados de 16×16. Nenhuma classe de teste é distribuída.

Evidências atuais: `docs/evidence/revision-0.4.0`. Capturas completas ficam em `build/client-qa-en_us/screenshots` e `build/client-qa-pt_br/screenshots`; a montagem apenas recorta/organiza imagens do cliente real.

Repetição: `gradlew.bat build reobfRuntimeTests`, `tools/smoke_cleanroom.ps1 -RunWorldTests` e `gradlew.bat runClient -PclientQa -PclientQaLang=en_us` (ou `pt_br`). O cenário é opt-in, usa mundo isolado e encerra o cliente. A aprovação exige `CROP_MANAGER_RUNTIME_TESTS_PASSED` ou `CROP_MANAGER_CLIENT_QA_PASSED`; só o código de saída do processo não comprova o resultado.

A bateria não representa validação no modpack E2EE completo, multiplayer com dois jogadores ou medição de TPS de uma fazenda grande. Os testes reais de rede EU usam BatBox conectada diretamente, sem cabos intermediários. O funcionamento do novo reservatório com tubos de outros mods requer verificação na instância completa.

---

Registro das versões anteriores:

## Expansão 0.3.0-alpha1

Sem crops novos. Implementados composteira elétrica/composto/fertilizante, bancada de comparação de sementes, caderno agrícola paginado e seis categorias de alarmes do monitor com sinal de redstone.

- 19 testes JUnit aprovados, incluindo máscaras independentes de alertas e pouca energia durante pausa por redstone.
- 94 verificações no mundo isolado Cleanroom: todos os cenários anteriores e os novos testes de custo exato de compostagem, saída cheia, automação por faces, save/load, não consumo de sementes, desconhecimento respeitado, registro/persistência/limite do caderno, rotação da bancada, identidade das entidades com estado ativo, redstone e ingredientes/resultados reais da receita de fertilizante.
- Cliente Forge 1.12.2 + IC2: capturas e testes de rede das telas novas nos dois idiomas e temas. Verificados gravação pela bancada, paginação pelo servidor e alteração da máscara de alarmes. O teste completo anterior de GUI/rede também passou em inglês e português.
- Verificador do JAR: 154 chaves correspondentes de tradução, bytecode Java 8, seis blockstates com oito variantes cada, modelos/texturas resolvidos, sprites RGBA nativos de 16px e nenhuma classe de teste no JAR final.
- Evidências resumidas em `evidence/expansion-0.3.0`; capturas são do cliente real de desenvolvimento. As montagens apenas recortam e organizam as capturas, sem redesenhar as GUIs.

Esta validação usa IC2 e Cleanroom locais, não a instância completa E2EE do usuário. Não houve sessão com dois jogadores ou medição de desempenho de grande fazenda. Nomes/atributos/descrições fornecidos por crops externos seguem os dados e traduções disponíveis no mod de origem.

## Painéis recolhíveis, monitor visual e idiomas

A revisão adiciona setas locais para recolher/expandir o painel, sem deslocar o inventário, trocar a janela de rede ou apagar filtros/aba. O monitor foi reduzido a 342px de largura, mostra valores próximos dos rótulos e barras para recursos e maturidade. Indicadores de crops dependem de um relatório válido.

Foram executados 17 testes automatizados, verificações visuais nos dois idiomas/temas, ciclos de recolhimento/expansão e testes reais de comandos da GUI. O verificador exige chaves idênticas, únicas e não vazias em en-US e pt-BR. en-US é principal/fallback; o idioma do jogador é preservado. Repetição: `./gradlew.bat runClient -PclientQa -PclientQaLang=en_us` ou `-PclientQaLang=pt_br`; saídas em `build/client-qa-en_us` e `build/client-qa-pt_br`.

## Expansão 0.2.0-alpha1

Adicionados Manager avançado MV, redstone, diagnóstico e filtros informativos, classificador, sonda, cartão e monitor com tela renderizada. Mantidos IDs e dados do Manager básico; cliente e servidor exigem a mesma versão nova.

- 17 testes JUnit: limites e persistência de filtros, perfis de energia, redstone e regressões anteriores.
- 67 verificações no mundo descartável Cleanroom 0.6.11-alpha: IC2 real, conservação de sementes/NBT/EU, saída cheia, save/load, cartão entre tiers, vínculo pela sonda, análise respeitada, marcadores e ausência de carregamento de chunks pelo monitor e pela máquina.
- Cliente de desenvolvimento Forge 1.12.2 + IC2: capturas reais dos temas, abas, classificador, monitor, sonda, itens e contorno em `build/client-qa/screenshots`. A fixture é opt-in e não entra no JAR de distribuição.
- Verificação de modelos, sprites RGBA 16px e metadados das animações pelo verificador de artefato. O importador também exige paleta nativa e contornos da carcaça iguais à referência.
- Revisão visual conforme a pasta `textures` fornecida: carcaça clara IC2, GUIs com fundo/slots originais, botões no estilo IC2 e ícone azul original em todas as telas.
- Testes pela conexão cliente-servidor real verificam redstone, alteração de filtro, amostra sem consumo e rejeição de comandos para a janela incorreta.

A inspeção no cliente de desenvolvimento não equivale a validar a expansão visualmente dentro do modpack E2EE/Cleanroom completo. Ainda não houve sessão multiplayer com dois jogadores. Para repetir as capturas isoladas: `./gradlew.bat runClient -PclientQa`; um mundo de desenvolvimento próprio será criado/reutilizado e o cliente encerrará após os testes. Não usar o JAR de testes na instância de jogo.

## Interface alpha3

GUI compacta baseada no desenho fornecido pelo usuário, com todos os 26 slots da máquina preservados. Adicionados tema claro/escuro com preferência local, tooltips, botão azul de upgrades e contorno local da área após fechar a GUI. Removidos os ajustes de área da interface; áreas de máquinas existentes permanecem salvas.

Validação desta revisão: 13 testes JUnit sem falhas; 37 verificações no mundo descartável Cleanroom 0.6.11-alpha, incluindo falta de energia estável entre ticks e recuperação após recarga. A inspeção do JAR confirma bytecode Java 8 e os recursos. Não houve inspeção visual da nova GUI ou do contorno no cliente; conferir escalas de GUI, tooltips, temas e restauração dos estados de renderização no pack.

## Revisão alpha2

Revisão independente da máquina, interface/rede e ferramentas, com três agentes e consolidação dos resultados. Corrigidos carregamento indireto de chunks por notificações de comparadores ao salvar a máquina e suas crops; perda de alterações por cliques concorrentes; acesso ao container após mudança de dimensão; custos `NaN` e energia NBT não finita. O protocolo agora exige a mesma versão em cliente e servidor.

Os scripts também foram corrigidos para PowerShell 5.1, caminhos relativos/cache Gradle, cópia do IC2 sobre si mesmo e resultados falsamente aprovados por logs antigos. A pasta isolada de teste remove somente versões reconhecidas do addon antes de instalar a atual. O verificador passou a conferir também o modelo do item e a textura opcional `service`.

## Compilação e inspeção

- Gradle Wrapper 9.7.0 com checksum; RetroFuturaGradle 2.0.3.
- Gradle executado com Azul JDK 25; compilador/toolchain Azul JDK 8 provisionado pelo Gradle.
- Minecraft 1.12.2, API Forge 14.23.5.2847, mappings stable_39.
- Dependência local: IC2 2.8.222-ex112, remapeada de SRG para MCP no desenvolvimento; SHA-256 validado no build.
- JAR de distribuição reobfuscado para SRG. Não contém classes do IC2, bibliotecas de terceiros ou o addon de testes.
- Todas as classes do addon têm bytecode Java 8 (major 52).
- Oito variantes de blockstate e três modelos JSON conferidos por script. Os oito PNGs foram comparados byte a byte com o ZIP original: 16×16, RGBA, sem alteração. Nenhuma imagem de preview foi incluída.

Comandos: `gradlew.bat test build reobfRuntimeTests` e `py tools/verify_artifact.py caminho-do-zip-original`.

O projeto do ZIP de fontes também foi extraído em uma segunda pasta e compilado independentemente, com o mesmo IC2 e toolchain. O JAR reconstruído apresentou o mesmo SHA-256 do JAR de entrega. Essa comparação comprova a reprodução nesse ambiente; não promete identidade binária com qualquer toolchain futura.

## Testes automatizados fora do jogo

Onze testes JUnit, todos aprovados. Usam lógica de produção e objetos reais Minecraft/Forge, sem stubs de Minecraft:

1. Percurso completo sem duplicações para todos os raios 1–8 e máscaras de camadas 1–7.
2. Velocidade base, velocidade com período reduzido e teto por tick.
3. Rejeição de configurações fora dos limites ou sem camadas.
4. Persistência NBT de energia, água, inventário, configurações, cursor, progresso e produtos pendentes acima de 127 itens.
5. Regras de inserção/extração por face.
6. Conservação da sobra quando uma inserção só cabe parcialmente.
7. Recuperação de energia NBT não finita e reparo de custos `NaN` em um arquivo de configuração Forge real.
8. Ações concorrentes/cliques rápidos sem perda de alterações, limites dos controles e manutenção da última camada.

Esses testes **não são prova de funcionamento dentro do jogo**. O bootstrap de registros vanilla é limitado ao processo de teste; a integração completa é verificada separadamente.

## Testes no runtime real do Minecraft

Foi iniciado um **servidor dedicado real**, em mundo plano descartável `cropmanager-test-world`, separado da instância Prism e dos mundos do usuário. O teste adicional fica em um JAR separado, nunca no JAR distribuído.

Baseline: Cleanroom **0.6.11-alpha**, sua API Forge **14.23.5.2864**, IC2 **2.8.222-ex112**, Java **25.0.3**. O servidor registrou o bloco, entidade e receita e executou 35 verificações aprovadas:

- Resolução do bloco crop e da CropCard de trigo reais do IC2.
- Ausência de ações e consumo de insumos sem EU.
- Colheita original preservando a planta e reduzindo seu estágio.
- Fertilizante: um item, 90 nutrientes pelo método nativo.
- Água: transferência de 200 mB uma única vez.
- Weed-Ex: transferência de 150 mB uma única vez.
- Custo combinado de nove consultas e quatro ações: 59 EU.
- Planta já atendida não consome mais fluidos.
- Saída cheia bloqueia colheita antes de alterar a planta.
- Capabilities reais rejeitam inserção na saída, extração de insumos e fertilizante inválido.
- NBT preserva EU, os dois tanques e a fila de produtos.
- Fila de 200 itens permanece íntegra após inserções parciais e distribuição em vários slots.
- Máquina com funções desligadas não gasta EU de busca.
- Overclockers aplicam fatores nativos de tempo e demanda.
- Storage e Transformer modificam capacidade e tier.
- Varredura 17×17 não carrega o chunk vizinho inicialmente descarregado.
- Saída de recipientes cheia impede drenagem do recipiente de entrada.
- Balde de água transfere 1.000 mB uma vez e conserva o balde vazio.
- Quebra da máquina entrega os produtos uma única vez.
- Salvamento da máquina e de uma crop atendida junto a um bloco sólido na borda não carrega o segundo chunk vizinho.
- BatBox original transfere EU pela rede durante ticks normais do servidor.
- Entrada EU continua após trocar tier/armazenamento e substituir a entidade a partir do NBT.
- O chunk é realmente descarregado e recarregado; os upgrades persistem e a entrada EU retorna.

Os cenários de cultivo chamam `update()` diretamente na thread real do servidor, usando mundo, entidades, inventários e métodos originais do IC2, com energia abastecida programaticamente. Depois deles, uma segunda fase usa ticks normais, uma BatBox original e a rede EU real, incluindo descarregamento/recarga de chunk. Esses testes **não medem TPS de uma plantação durante jogo normal** e a BatBox é conectada diretamente, sem cabos intermediários.

Resumo de evidências preservado em `docs/evidence/cleanroom-baseline.txt`. Os logs completos, mundo descartável e relatórios JUnit ficam em `build/` e são removidos por `gradlew clean`.

A mesma bateria de **35 verificações também passou com CropAccelerator 0.1.0-alpha1 instalado e Java 25.0.3**, mantendo Cleanroom 0.6.11-alpha e o IC2 fornecido. O log confirma as transformações do CropAccelerator nas duas máquinas originais. Evidência: `docs/evidence/cleanroom-with-accelerator.txt`. Isso confirma a coexistência nesse servidor mínimo, sem representar validação de todos os mods do E2EE.

## Repetir os testes reais

Requer bibliotecas locais do Cleanroom 0.6.11-alpha já instaladas pelo Relauncher. Ajuste `-CleanroomRoot` e `-JavaHome` conforme seu computador. A ferramenta não baixa nem modifica a instalação do Cleanroom.

O script encontra o cache do Gradle pelo ambiente, cache padrão ou cache local do projeto. Use `-GradleUserHome` caso ele esteja em outro local. Os testes exigem os custos e limites padrão na configuração do servidor isolado.

```powershell
.\gradlew.bat test build reobfRuntimeTests
.\tools\smoke_cleanroom.ps1
```

A primeira execução termina na exigência da EULA. Após ler e aceitar a EULA, coloque `eula=true` em `build/cleanroom-smoke/eula.txt` e execute:

```powershell
.\tools\smoke_cleanroom.ps1 -RunWorldTests
.\tools\smoke_cleanroom.ps1 -RunWorldTests -WithAccelerator
```

O segundo comando exige o JAR alpha em `$env:USERPROFILE/Biblioteca/CropAccelerator-0.1.0-alpha1.jar`. O addon de teste exige o nome específico do mundo e a propriedade de execução, executa as verificações e encerra o servidor. O script acusa falha quando o marcador final de aprovação não aparece.

## Ainda exige validação manual no pack

- [ ] Iniciar a instância E2EE completa, com seus scripts, configurações e demais mods.
- [ ] Conferir a receita no HEI e fabricar a máquina na bancada do pack.
- [ ] Verificar as quatro orientações, frente ativa, texturas e ausência de modelo roxo/preto.
- [ ] Abrir a interface nas escalas de GUI usadas pelo jogador; conferir cliques, shift-click, slots e textos PT-BR/inglês.
- [ ] Alimentar por fontes e cabos EU reais nas seis faces; testar tiers permitidos e comportamento de sobretensão do IC2 numa cópia de mundo.
- [ ] Usar tubos/hoppers e conexões de fluido dos mods do pack.
- [ ] Conectar dois jogadores, alterar configurações simultaneamente e conferir sincronização e distância de acesso.
- [ ] Testar raio e camadas em uma plantação real e com crops adicionais do pack, incluindo drops com NBT.
- [ ] Salvar, sair completamente do jogo, reabrir e verificar todas as reservas e a retomada da varredura.
- [ ] Medir desempenho com várias máquinas, Overclockers e áreas máximas durante ticks normais.
- [ ] Conferir as máquinas IC2 originais e plantas sobrepostas com o CropAccelerator.

Não houve inspeção visual do cliente Minecraft, sessão multiplayer com jogadores ou teste do modpack completo. Não se declara compatibilidade universal com versões futuras de Cleanroom/E2EE. A recuperação após encerramento abrupto durante salvamento não é transacional entre máquina e planta.
