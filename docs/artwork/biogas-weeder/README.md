# Capinador a Biogás — pacote de arte

**Atualização 0.5.0:** a arte foi integrada como bloco funcional, com receita, GUI, biogás, remoção segura de ervas, colisão por partes e variante ativa. A aparência foi conferida no Minecraft. O texto abaixo e o ZIP de arte registram a entrega original anterior à integração; as fontes atuais do mod são a referência para uso no jogo. O importador reproduz a base visual; o modelo ativo e `motor_active.png` complementam essa base nos recursos do mod.

Modelo de aparência para uma máquina agrícola estacionária de um bloco. Chassi baixo com cantos chanfrados, motor frontal, cabeçote com três dentes, cilindro traseiro octogonal, válvula, tubulação e pequeno escape. Sem rodas, faixas futuristas ou chamas.

## Arquivos prontos

- `src/main/resources/assets/cropmanager/textures/blocks/biogas_weeder/`: nove PNGs RGBA, todos com **16×16 pixels reais**. Cada pixel pertence à paleta IC2 auditada em `docs/artwork/ic2-palette.json`.
- `src/main/resources/assets/cropmanager/models/block/biogas_weeder.json`: modelo vanilla Minecraft 1.12.2 com 32 elementos, sem OBJ, sem dependência de renderizador especial. Frente = norte; todo o modelo cabe no volume 0–16 de um bloco.
- `src/main/resources/assets/cropmanager/models/item/biogas_weeder.json`: aparência de inventário herdando o modelo do bloco, ainda sem item registrado.
- `model-preview.png`: render de três vistas do JSON e dos PNGs finais. Renderizador ortográfico por triângulos, z-buffer e amostragem de texel por vizinho mais próximo.
- `texture-sheet.png`: faces ampliadas apenas para inspeção; os PNGs usados pelo modelo continuam 16×16.
- `inventory-render.png`: render transparente de referência para inventário, não um sprite que substitua o modelo.
- `validation.json`: resolução, modo RGBA, hashes e validação da paleta/geometria.

## Fonte e reprodução

A folha-base foi criada com a ferramenta integrada `image_gen`, usando o prompt em `generation-prompt.txt`. A imagem original está em `generated-material-atlas.png`. Ela é uma fonte de arte, não uma textura pronta para carregar no Minecraft.

O importador `tools/create_biogas_weeder_art.py` reduz as células por vizinho mais próximo, quantiza pela paleta nativa e preserva o contorno IC2 nas duas faces de carcaça. Também produz o JSON, renderiza as prévias a partir dos arquivos finais e valida os PNGs. Requer Python, Pillow e NumPy, a paleta do projeto e o IC2 auditado local em `libs/industrialcraft-2-2.8.222-ex112.jar` para a carcaça. O JAR do IC2 não faz parte deste pacote de arte.

```powershell
py tools/create_biogas_weeder_art.py
```

As sombras e o fundo das prévias não são parte dos assets. As nove texturas finais são opacas; os vazios da silhueta são definidos pela geometria do modelo, não por pixels de transparência.

## Limite desta entrega

Arte pronta e modelo de prévia, **sem registro de bloco/item, receita, GUI, lógica de biogás, remoção de ervas ou animação de funcionamento**. Nenhuma máquina existente e nenhum JAR de distribuição foram alterados. A prévia foi inspecionada no renderizador do modelo; ainda requer conferência no cliente Minecraft quando o novo bloco for implementado. A colisão deverá acompanhar a forma da máquina na futura implementação; o JSON só define a aparência.
