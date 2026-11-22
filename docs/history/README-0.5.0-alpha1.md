# Crop Manager 0.5.0-alpha1

Addon para Minecraft 1.12.2 e **IndustrialCraft 2 Experimental 2.8.222-ex112**. Conteúdo ativo: Crop Manager avançado, monitor agrícola, composteira elétrica, reservatório agrícola, coletor agrícola, capinador a biogás e sonda agrícola. Não adiciona crops.

## Instalação e atualização

Copie somente `CropManager-0.5.0-alpha1.jar` para `mods`, substituindo o JAR antigo. Cliente e servidor precisam da mesma versão. IC2 continua obrigatório; não instale os JARs de desenvolvimento/testes. Faça uma cópia do mundo antes de atualizar uma versão alpha.

O Manager básico antigo migra automaticamente para o avançado após o carregamento do chunk, preservando inventário, EU, tanques, orientação, área e controles. A conversão não duplica drops nem abastece energia gratuitamente. Até 32 conversões são feitas por tick.

Classificador e bancada de sementes foram removidos dos registros de blocos/itens. Mundos antigos os convertem em coletores agrícolas, preservando o inventário. O excedente dos nove slots fica salvo numa fila e entra nos slots conforme houver espaço; quebrar devolve também a fila. Itens antigos são remapeados: Manager básico para avançado, classificador/bancada para coletor, cartão para papel, caderno para livro e composto para terra. A sonda é o único item independente do addon.

O Crop-Matron e o Crop Harvester originais do IC2 permanecem disponíveis.

## Interface e idiomas

Todas as telas usam o fundo cinza, slots e ícone azul originais do IC2, com botões no mesmo estilo. Não há tema escuro ou botão de tema. O ícone azul lista apenas os upgrades aceitos. GUIs de suporte usam largura clássica de 176px; Manager/monitor têm painéis laterais recolhíveis, sem mover o inventário ou alterar a janela de rede.

Inglês (`en_us`) é principal e fallback; português brasileiro (`pt_br`) também é fornecido. O idioma segue a configuração do Minecraft. Nomes e atributos de crops externos seguem as traduções disponíveis no mod de origem.

## Crop Manager avançado

Receita horizontal: **Crop Harvester + circuito eletrônico avançado + Crop-Matron**, usando os itens reais do IC2.

Une cuidados e colheita em uma varredura. Base: tier 2, 100.000 EU e tanques de 20.000 mB de água e Weed-Ex. Os quatro controles alternam colheita, adubo, água e Weed-Ex. Redstone alterna sempre / com sinal / sem sinal. Overclocker, armazenamento e transformador têm seus efeitos nativos. Saída de produtos por baixo; entradas pelas outras faces.

A área padrão é 9×9 em três camadas. A aba Área permite ajustar o raio de 1 até o limite do servidor (8 por padrão: 3×3 até 17×17), selecionar as camadas inferior/central/superior e restaurar o padrão configurado. Sempre permanece ao menos uma camada. Mostrar/ocultar área só altera o contorno local. A seleção compara genética analisada e destaca plantas fora dos critérios; não modifica, cruza, planta ou destrói sementes.

O controle principal liga/desliga a máquina, separado das quatro funções. Desligar manualmente prevalece sobre a retomada automática. Regras de tanque são executadas e salvas no próprio Manager, mesmo sem um monitor carregado.

O diagnóstico publica um relatório após uma varredura completa. Mostra plantas, maduras, ervas, não analisadas, fora do filtro e posições indisponíveis, além da idade do relatório. Não carrega chunks.

## Sonda e monitor agrícola

Receita da sonda: ` G  / ICI /  R `, com vidro, ferro, circuito eletrônico IC2 e redstone.

1. Segure a sonda, agache e use no Manager, no capinador ou no **Crop-Matron original do IC2**.
2. Use a mesma sonda no monitor para concluir o vínculo.
3. Abra o monitor para ver o alvo e os recursos. Alcance máximo: 32 blocos na mesma dimensão, com o alvo carregado.

A sonda também inspeciona crops. Ela respeita os níveis de análise do IC2 e não revela genética de crops desconhecidos.

Monitor: receita `GGG / CRC / III`, com vidro, circuito eletrônico, redstone e ferro. Não precisa de EU nem aceita upgrades. Mostra conexão, coordenadas, energia e líquidos com valores atuais/capacidade em mB, além do ícone real do fertilizante com sua quantidade. Diagnóstico de plantas está disponível quando o alvo é o Manager avançado; o Crop-Matron original fornece seus recursos reais. Para o capinador, exibe biogás e ervas removidas.

Na aba **Ctrl.**, é possível ligar/desligar o Manager, alternar colheita, adubo, água e Weed-Ex e mudar seu modo de redstone. No capinador, ficam disponíveis ligar/desligar e modo de redstone; o Crop-Matron original fornece apenas leitura. O botão **Redstone** do monitor alterna:

- **Desligada:** sem sinal automático; diagnóstico visual continua ativo.
- **Alarme:** intensidade 15 quando existir uma falha aplicável ou parada automática.
- **Normal:** intensidade 15 somente com vínculo válido, alvo habilitado/sem pausa e sem falhas.

A antiga seleção de falhas foi removida. O alarme considera recursos das funções habilitadas, saída cheia, vínculo perdido e falta de biogás no capinador. Pouca EU significa energia insuficiente para a varredura mínima. O Crop-Matron não tem colheita e não produz alerta de saída de produtos.

Na aba **Auto.**, configure regras de parada do Manager: desligadas, qualquer condição selecionada ou todas as condições selecionadas. Água e Weed-Ex têm limites independentes: desativado, vazio (0) ou abaixo de uma porcentagem em passos de 5%. O preset **Tanques vazios** seleciona ambos vazios. Exemplo: Weed-Ex abaixo de 20% pausa os cuidados/colheita; recipientes e tubos continuam abastecendo. A pausa persiste após salvar/reabrir o mundo. **Só retoma quando o Weed-Ex estiver 100% cheio e a condição que causou a parada estiver resolvida.** Desligar as regras cancela essa trava; desligar manualmente a máquina continua impedindo sua operação.

**Testar 5 s** força sinal 15 por 100 ticks, inclusive no modo desligado; não fica salvo ao reiniciar o mundo. **Desvincular** limpa alvo e teste. Um monitor nunca vinculado não alarma; um vínculo perdido pode alarmar. Atualização de recursos a cada segundo, sem carregar chunks. A saída é redstone fraca nas faces; não há saída forte através de blocos sólidos.

## Capinador a biogás

Receita `IBI / FCF / III`: ferro, balde vazio, fornalhas e circuito eletrônico IC2. Modelo 3D de um bloco com chassi baixo, tanque octogonal, motor e dentes; texturas nativas 16×16 na paleta IC2. A lâmpada acende e o cabeçote muda de posição ao remover ervas. Colisão acompanha as partes principais, em vez de um cubo cheio.

Não usa EU nem upgrades. Tanque de **16.000 mB de biogás IC2**, abastecido por recipientes ou capacidade Forge de fluidos. O slot de entrada recebe o recipiente e o de saída devolve o resultado. A automação extrai apenas a saída.

Busca duas posições por tick: **1 mB por posição carregada examinada, mais 20 mB por erva removida**. Consome biogás também ao procurar numa área sem ervas; use área menor ou redstone para economizar. Remove apenas a erva daninha real do IC2, preservando as estacas e crops valiosos de crescimento alto. Não colhe, não replanta e não usa Weed-Ex. Não carrega chunks; combustível insuficiente não apaga a planta.

A GUI permite ligar/desligar, escolher sempre/com sinal/sem sinal, ajustar raio/camadas e visualizar o alcance. Biogás, controles, contador de ervas e posição da varredura persistem. Ao quebrar, devolve os recipientes; o biogás interno é perdido.

## Composteira elétrica

Receita `III / FCF / III`: ferro, fornalhas e circuito eletrônico IC2. Tier 1, capacidade inicial 20.000 EU.

**8 vegetais iguais → 1 fertilizante IC2**, em 200 ticks e 5 EU/t: 1.000 EU por lote a 20 TPS. Sem item intermediário. Aceita trigo e sementes vanilla, cenoura, batata, batata venenosa, beterraba, cana, maçã, melancia, folhas, mudas, vinhas, grama alta e cacto. Não aceita sacos de sementes IC2 nem produtos minerais.

Quatro slots à direita aceitam Overclocker, armazenamento e transformador. A duração usa o multiplicador real do IC2, arredondada para cima e limitada a um tick; demanda também arredonda para cima. Muitos Overclockers podem exigir mais EU por tick do que a capacidade disponível. Trocar upgrades preserva a proporção do trabalho concluído. Armazenamento aumenta a capacidade; transformador aumenta o tier de entrada.

Entrada vegetal pelas faces superior/laterais; extração do fertilizante por baixo. Upgrades são instalados manualmente. Falta de EU/material ou saída cheia pausam sem consumir itens. Progresso, inventário, upgrades e EU persistem. Ao quebrar, itens são devolvidos e progresso/energia são perdidos.

## Reservatório agrícola

Receita `GBG / BCB / III`: vidro, baldes vazios, circuito eletrônico IC2 e ferro. Tier 1, 20.000 EU, dois tanques separados de **32.000 mB**.

Armazena água e Weed-Ex recebidos por recipientes ou pela capacidade de fluidos do Forge. A cada segundo, tenta abastecer um Manager ou Crop-Matron adjacente, alternando os destinos. Transfere até **1.000 mB de cada fluido** por visita, gastando **20 EU por transferência efetiva**. Não gera líquidos nem gasta EU quando o alvo está cheio ou indisponível. Sem upgrades.

Os dois slots superiores recebem recipientes; os inferiores devolvem recipientes resultantes. Automação insere apenas líquidos corretos pelos recipientes e extrai apenas os resultados. As conexões de fluidos permitem inserir e retirar reservas. Não distribui para outros reservatórios automaticamente. Ao quebrar, os recipientes caem; energia e fluidos internos são perdidos.

## Coletor agrícola

Receita `IHI / ICI / IRI`: ferro, funil, circuito eletrônico IC2 e redstone. Tier 1, 20.000 EU e nove slots de saída.

A cada dez ticks, tenta recolher uma pilha de itens soltos em **9×9×3**, gastando **20 EU apenas se algum item entrar**. Não colhe plantas nem acessa inventários externos. Aceita itens de qualquer tipo, respeitando propriedade e bloqueio de coleta. Entradas parciais deixam o restante no chão com o mesmo NBT; inventário cheio ou falta de EU não apagam drops.

Botões para ligar/desligar, escolher sempre/com sinal/sem sinal e mostrar/ocultar a área. Automação só extrai do inventário. Sem upgrades. Inventário e controles persistem; quebrar devolve os itens e perde a energia.

## Construção e validação

Coloque o IC2 auditado em `libs/industrialcraft-2-2.8.222-ex112.jar`. O checksum é validado pelo build. Use o Gradle Wrapper com o JDK local; o compilador gera bytecode Java 8.

```powershell
.\gradlew.bat build reobfRuntimeTests
py tools\verify_artifact.py
```

O JAR reobfuscado está em `build/libs/CropManager-0.5.0-alpha1.jar`. Para o mundo isolado Cleanroom e capturas opt-in, consulte `docs/VALIDACAO.md`. O ZIP de fontes inclui código, recursos, testes, documentação e Wrapper, sem dependências nem dados do jogo. A importação de arte usa `py tools/import_farm_atlas.py`, com faces de 16×16 e a paleta auditada do IC2.

O alvo é IC2 2.8.222-ex112 em Minecraft 1.12.2, com verificações no Cleanroom local e cliente de desenvolvimento. Isso não equivale a validar todos os scripts, mods e receitas do E2EE completo.

## Correção de contraste alpha2

A fonte agora acompanha a luminosidade do fundo real do IC2 carregado pelo jogo, incluindo resource packs. Fundos escuros usam texto claro; fundos claros usam texto escuro. A paleta é recalculada ao recarregar recursos, sem botão de tema. A correção também abrange os textos das outras GUIs e os avisos do Manager.

Validação visual isolada: `py tools/prepare_contrast_test.py` e `gradlew.bat runClient -PclientQa -PclientQaLang=pt_br -PclientQaContrast`. O pacote escuro é temporário, fica em `build/contrast-pack.zip` e não é distribuído como parte do mod. São verificadas a fonte clara, a recarga para o fundo claro e capturas reais de ambos os estados.

A 0.5.0 mantém a adaptação de contraste da alpha2. A verificação de versão do addon exige a mesma versão no cliente e no servidor.
