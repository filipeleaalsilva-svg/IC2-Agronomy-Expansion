# Evidências 0.5.0-alpha2

Cliente Minecraft 1.12.2 de desenvolvimento com IC2 2.8.222-ex112; screenshots são capturas reais. A fixture `clientQa` validou layout, rede e survey local em en_us e pt_br. Os dois logs terminam com `CROP_MANAGER_CLIENT_QA_PASSED`; os XMLs registram 31 testes unitários sem falhas.

- `en_us/` e `pt_br/`: capturas da rodada final, incluindo Manager, monitor, Ambiente, culturas, seleção de espécie, paginação e GUI scale 1.
- `runtime-markers.txt`: 145 verificações do servidor Cleanroom 0.6.11-alpha.
- `client-*-markers.txt`: marcadores dos QA de cliente.
- `TEST-*.xml`: resultados JUnit.

Verificação do artefato: 71 classes Java 8, 6 blockstates ativos, 19 modelos, 41 sprites 16px e 219 traduções correspondentes. SHA-256 do JAR: `39ec5ee51b48937d45c1b0b978aee29e0dcfb6e586c462dca15b9a942f34a69c`.
