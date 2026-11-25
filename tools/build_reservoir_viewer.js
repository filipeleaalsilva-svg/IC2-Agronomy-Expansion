const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const objPath = path.join(rootDir, 'src/main/resources/assets/cropmanager/models/block/agricultural_reservoir.obj');
const mtlPath = path.join(rootDir, 'src/main/resources/assets/cropmanager/models/block/agricultural_reservoir.mtl');
const texDir = path.join(rootDir, 'src/main/resources/assets/cropmanager/textures/blocks');

const objContent = fs.readFileSync(objPath, 'utf8');
const mtlContent = fs.readFileSync(mtlPath, 'utf8');

const textures = [
  'storage_steel.png',
  'sheetmetal_copper.png',
  'storage_copper.png',
  'water_tower_wood.png',
  'water_tower_glass.png',
  'water_tower_brass.png',
  'water_tower_controller.png',
  'water_tower_copper.png',
  'water_tower_iron_plate.png'
];

const texBase64 = {};
for (const tex of textures) {
  const p = path.join(texDir, tex);
  const data = fs.readFileSync(p);
  texBase64[tex.replace('.png', '')] = 'data:image/png;base64,' + data.toString('base64');
}

const htmlContent = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Crop Manager - Visualizador & Editor do Reservatório Agrícola 3D</title>
  <style>
    :root {
      --bg-dark: #121418;
      --bg-card: #1b1e24;
      --bg-card-hover: #262b34;
      --accent: #2e8b57;
      --accent-hover: #3cb371;
      --accent-active: #4cd78a;
      --text: #e6edf3;
      --text-muted: #8b949e;
      --border: #30363d;
      --water: #3880c8;
      --weedex: #a29130;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      background: var(--bg-dark);
      color: var(--text);
      display: flex;
      height: 100vh;
      overflow: hidden;
    }
    #viewport-container {
      flex: 1;
      position: relative;
      background: radial-gradient(circle at 50% 50%, #222933 0%, #0d1117 100%);
      overflow: hidden;
    }
    #canvas3d {
      width: 100%;
      height: 100%;
      display: block;
    }
    #sidebar {
      width: 380px;
      background: var(--bg-card);
      border-left: 1px solid var(--border);
      display: flex;
      flex-direction: column;
      z-index: 10;
      box-shadow: -4px 0 20px rgba(0,0,0,0.5);
    }
    .header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border);
      background: #15181e;
    }
    .header h1 {
      font-size: 1.15rem;
      font-weight: 700;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .badge {
      font-size: 0.7rem;
      background: var(--accent);
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-weight: 600;
    }
    .subtitle {
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-top: 4px;
    }
    .tabs {
      display: flex;
      border-bottom: 1px solid var(--border);
      background: #15181e;
    }
    .tab-btn {
      flex: 1;
      padding: 10px 4px;
      background: transparent;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 0.82rem;
      font-weight: 600;
      transition: all 0.2s;
      border-bottom: 2px solid transparent;
    }
    .tab-btn:hover {
      color: #fff;
      background: rgba(255,255,255,0.03);
    }
    .tab-btn.active {
      color: var(--accent-active);
      border-bottom-color: var(--accent-active);
      background: rgba(46, 139, 87, 0.1);
    }
    .tab-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: none;
    }
    .tab-content.active {
      display: block;
    }
    .section-title {
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: 10px;
      font-weight: 700;
    }
    .group-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 12px;
      margin-bottom: 6px;
      background: #21252d;
      border-radius: 6px;
      border: 1px solid var(--border);
      cursor: pointer;
      transition: background 0.15s;
    }
    .group-item:hover {
      background: var(--bg-card-hover);
    }
    .group-label {
      font-size: 0.85rem;
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
    }
    .group-label input[type="checkbox"] {
      cursor: pointer;
      accent-color: var(--accent);
      width: 16px;
      height: 16px;
    }
    .group-faces {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .controls-overlay {
      position: absolute;
      top: 16px;
      left: 16px;
      background: rgba(21, 24, 30, 0.85);
      backdrop-filter: blur(8px);
      padding: 8px 14px;
      border-radius: 8px;
      border: 1px solid var(--border);
      font-size: 0.8rem;
      color: var(--text-muted);
      pointer-events: none;
    }
    .controls-overlay span {
      color: #fff;
      font-weight: 600;
    }
    .view-buttons {
      position: absolute;
      bottom: 16px;
      left: 16px;
      display: flex;
      gap: 8px;
    }
    .btn {
      background: #21262d;
      color: #c9d1d9;
      border: 1px solid var(--border);
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.8rem;
      cursor: pointer;
      transition: all 0.15s;
      font-weight: 500;
    }
    .btn:hover {
      background: #30363d;
      color: #fff;
      border-color: #8b949e;
    }
    .btn-primary {
      background: var(--accent);
      color: #fff;
      border-color: var(--accent-hover);
    }
    .btn-primary:hover {
      background: var(--accent-hover);
    }
    .slider-container {
      margin-bottom: 14px;
    }
    .slider-header {
      display: flex;
      justify-content: space-between;
      font-size: 0.82rem;
      margin-bottom: 6px;
    }
    input[type="range"] {
      width: 100%;
      accent-color: var(--accent);
    }
    .texture-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
    }
    .tex-card {
      background: #21252d;
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 8px;
      text-align: center;
      cursor: pointer;
      transition: border 0.2s;
    }
    .tex-card:hover, .tex-card.selected {
      border-color: var(--accent);
    }
    .tex-preview {
      width: 64px;
      height: 64px;
      image-rendering: pixelated;
      margin: 0 auto 6px;
      display: block;
      background: #000;
      border: 1px solid #444;
      border-radius: 4px;
    }
    .tex-name {
      font-size: 0.72rem;
      color: var(--text-muted);
      word-break: break-all;
    }
    .pixel-editor {
      margin-top: 14px;
      background: #15181e;
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px;
    }
    #pixelCanvas {
      image-rendering: pixelated;
      cursor: crosshair;
      border: 2px solid var(--border);
      display: block;
      margin: 10px auto;
      background: #000;
    }
    .palette {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;
      justify-content: center;
    }
    .palette-color {
      width: 22px;
      height: 22px;
      border-radius: 4px;
      cursor: pointer;
      border: 1px solid rgba(255,255,255,0.2);
    }
    .palette-color.active {
      outline: 2px solid #fff;
    }
    .help-card {
      background: #21252d;
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 12px;
      font-size: 0.83rem;
      line-height: 1.45;
    }
    .help-card h3 {
      font-size: 0.9rem;
      color: #fff;
      margin-bottom: 6px;
    }
    .code-tag {
      background: #161b22;
      border: 1px solid #30363d;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: monospace;
      font-size: 0.78rem;
      color: #7ee787;
    }
  </style>
  <!-- Three.js e OrbitControls via CDN -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
</head>
<body>

  <div id="viewport-container">
    <div class="controls-overlay">
      <span>Girar:</span> Botão Esquerdo &bull; <span>Pan:</span> Botão Direito &bull; <span>Zoom:</span> Scroll
    </div>
    <div class="view-buttons">
      <button class="btn" onclick="setView('iso')">Isometric</button>
      <button class="btn" onclick="setView('front')">Frontal</button>
      <button class="btn" onclick="setView('side')">Lateral</button>
      <button class="btn" onclick="setView('top')">Superior</button>
      <button class="btn" onclick="toggleAutoRotate()">Auto-Rotacionar</button>
    </div>
    <canvas id="canvas3d"></canvas>
  </div>

  <div id="sidebar">
    <div class="header">
      <h1>Reservatório Agrícola <span class="badge">3x5x3</span></h1>
      <div class="subtitle">Modelo 3D OBJ &bull; Rural-IC2 Multiblock</div>
    </div>

    <div class="tabs">
      <button class="tab-btn active" onclick="switchTab('groups')">Partes (Grupos)</button>
      <button class="tab-btn" onclick="switchTab('fluids')">Fluidos</button>
      <button class="tab-btn" onclick="switchTab('textures')">Texturas</button>
      <button class="tab-btn" onclick="switchTab('export')">Editar / Exportar</button>
    </div>

    <!-- TAB GRUPOS -->
    <div id="tab-groups" class="tab-content active">
      <div class="section-title">Componentes do Modelo</div>
      <div id="groups-list"></div>

      <div class="section-title" style="margin-top: 18px;">Opções de Visualização</div>
      <label class="group-item">
        <span class="group-label">
          <input type="checkbox" id="chk-wireframe" onchange="toggleWireframe(this.checked)">
          Modo Wireframe (Arestas)
        </span>
      </label>
      <label class="group-item">
        <span class="group-label">
          <input type="checkbox" id="chk-bounds" checked onchange="toggleBounds(this.checked)">
          Limites do Multiblock (3x5x3)
        </span>
      </label>
      <label class="group-item">
        <span class="group-label">
          <input type="checkbox" id="chk-grid" checked onchange="toggleGrid(this.checked)">
          Grade do Chão (Minecraft Y=0)
        </span>
      </label>
    </div>

    <!-- TAB FLUIDOS -->
    <div id="tab-fluids" class="tab-content">
      <div class="section-title">Simulação de Fluidos Internos</div>
      <p style="font-size:0.8rem; color:var(--text-muted); margin-bottom:12px;">
        O modelo real renderiza colunas dinâmicas recortadas de água e Weed-EX nos tanques de vidro.
      </p>

      <div class="slider-container">
        <div class="slider-header">
          <span>Tanque Esquerdo (Água)</span>
          <span id="lbl-water">16,000 / 32,000 mB</span>
        </div>
        <input type="range" id="rng-water" min="0" max="32000" value="20000" oninput="updateFluids()">
      </div>

      <div class="slider-container">
        <div class="slider-header">
          <span>Tanque Direito (Weed-EX)</span>
          <span id="lbl-weed">8,000 / 16,000 mB</span>
        </div>
        <input type="range" id="rng-weed" min="0" max="16000" value="8000" oninput="updateFluids()">
      </div>

      <label class="group-item" style="margin-top: 14px;">
        <span class="group-label">
          <input type="checkbox" id="chk-fluids" checked onchange="toggleFluids(this.checked)">
          Renderizar Fluidos Internos
        </span>
      </label>
    </div>

    <!-- TAB TEXTURAS -->
    <div id="tab-textures" class="tab-content">
      <div class="section-title">Texturas do Modelo (16x16)</div>
      <div class="texture-grid" id="tex-grid"></div>

      <div class="pixel-editor" id="pixel-editor-section" style="display:none;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <strong id="editor-tex-title" style="font-size:0.85rem; color:#fff;">Editar Textura</strong>
          <button class="btn" style="padding:2px 8px; font-size:0.75rem;" onclick="applyEditedTexture()">Aplicar 3D</button>
        </div>
        <canvas id="pixelCanvas" width="160" height="160"></canvas>
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <input type="color" id="colorPicker" value="#ffffff" style="width:40px; height:24px; cursor:pointer;">
          <button class="btn" style="padding:2px 8px; font-size:0.75rem;" onclick="uploadTexturePrompt()">Carregar PNG</button>
          <input type="file" id="texFileInput" accept="image/png" style="display:none;" onchange="loadCustomPNG(this)">
        </div>
      </div>
    </div>

    <!-- TAB EXPORTAR / EDITAR -->
    <div id="tab-export" class="tab-content">
      <div class="section-title">Como Editar este Modelo</div>

      <div class="help-card">
        <h3>1. No Blockbench (Recomendado)</h3>
        <p>O Blockbench é a ferramenta oficial e gratuita para modelar Minecraft:</p>
        <ol style="margin-left: 18px; margin-top: 6px;">
          <li>Abra o <strong>Blockbench</strong>.</li>
          <li>Vá em <strong>File &gt; Import &gt; OBJ Model</strong>.</li>
          <li>Selecione o arquivo: <br><span class="code-tag">models/block/agricultural_reservoir.obj</span></li>
          <li>Carregue as texturas da pasta: <br><span class="code-tag">textures/blocks/water_tower_*.png</span></li>
        </ol>
      </div>

      <div class="help-card">
        <h3>2. No Blender</h3>
        <p>No Blender, use <strong>File &gt; Import &gt; Wavefront (.obj)</strong> e selecione o arquivo com o material (.mtl) na mesma pasta.</p>
      </div>

      <div class="section-title" style="margin-top:16px;">Download dos Arquivos Atuais</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <button class="btn btn-primary" onclick="downloadFile('agricultural_reservoir.obj', rawOBJ)">Baixar Modelo (OBJ)</button>
        <button class="btn" onclick="downloadFile('agricultural_reservoir.mtl', rawMTL)">Baixar Materiais (MTL)</button>
      </div>
    </div>

  </div>

  <script>
    const rawOBJ = ${JSON.stringify(objContent)};
    const rawMTL = ${JSON.stringify(mtlContent)};
    const texturesBase64 = ${JSON.stringify(texBase64)};

    let scene, camera, renderer, controls;
    let modelGroup, fluidGroup, boundsBox, gridHelper;
    let autoRotate = false;
    const meshGroups = {};
    const materials = {};
    const textureObjects = {};

    function init3D() {
      const container = document.getElementById('viewport-container');
      const canvas = document.getElementById('canvas3d');

      scene = new THREE.Scene();
      camera = new THREE.PerspectiveCamera(45, container.clientWidth / container.clientHeight, 0.1, 100);
      camera.position.set(4, 5, 5);

      renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true });
      renderer.setSize(container.clientWidth, container.clientHeight);
      renderer.setPixelRatio(window.devicePixelRatio);
      renderer.outputEncoding = THREE.sRGBEncoding;

      controls = new THREE.OrbitControls(camera, renderer.domElement);
      controls.target.set(0.5, 2.5, 0.5);
      controls.enableDamping = true;
      controls.dampingFactor = 0.05;

      // Iluminação suave tipo Minecraft
      const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
      scene.add(ambientLight);

      const dirLight = new THREE.DirectionalLight(0xffffff, 0.6);
      dirLight.position.set(5, 10, 7);
      scene.add(dirLight);

      const dirLight2 = new THREE.DirectionalLight(0xffffff, 0.3);
      dirLight2.position.set(-5, 6, -5);
      scene.add(dirLight2);

      // Chão de grade Minecraft (bloco 1m)
      gridHelper = new THREE.GridHelper(10, 10, 0x2e8b57, 0x30363d);
      gridHelper.position.set(0.5, 0, 0.5);
      scene.add(gridHelper);

      // Caixa delimitadora do multiblock 3x5x3
      const boxGeo = new THREE.BoxGeometry(3, 5, 3);
      const wireGeo = new THREE.WireframeGeometry(boxGeo);
      boundsBox = new THREE.LineSegments(wireGeo, new THREE.LineBasicMaterial({ color: 0x4cd78a, transparent: true, opacity: 0.35 }));
      boundsBox.position.set(0.5, 2.5, 0.5);
      scene.add(boundsBox);

      // Criar Materiais com NearestFilter para estilo Pixel-Art
      for (const [name, b64] of Object.entries(texturesBase64)) {
        const img = new Image();
        img.src = b64;
        const tex = new THREE.Texture(img);
        img.onload = () => { tex.needsUpdate = true; };
        tex.magFilter = THREE.NearestFilter;
        tex.minFilter = THREE.NearestFilter;
        textureObjects[name] = tex;

        const isGlass = (name === 'water_tower_glass');
        materials[name] = new THREE.MeshStandardMaterial({
          map: tex,
          roughness: 0.8,
          metalness: name.includes('iron') ? 0.3 : 0.1,
          transparent: isGlass,
          opacity: isGlass ? 0.45 : 1.0,
          side: THREE.DoubleSide
        });
      }

      // Mapear nomes de material do MTL
      materials['steel'] = materials['storage_steel'];
      materials['iron_plate'] = materials['storage_steel'] || materials['water_tower_iron_plate'];
      materials['wood'] = materials['water_tower_wood'];
      materials['glass'] = materials['water_tower_glass'];
      materials['copper'] = materials['water_tower_copper'];
      materials['sheetmetal_copper'] = materials['sheetmetal_copper'];
      materials['storage_copper'] = materials['storage_copper'];
      materials['controller'] = materials['water_tower_controller'];
      materials['brass'] = materials['water_tower_brass'];

      loadModelFromOBJ(rawOBJ);
      createFluidMeshes();
      setupUI();

      window.addEventListener('resize', onWindowResize);
      animate();
    }

    function loadModelFromOBJ(objText) {
      if (modelGroup) scene.remove(modelGroup);
      modelGroup = new THREE.Group();

      const lines = objText.split(/\\r?\\n/);
      const positions = [];
      const uvs = [];

      let currentGroup = 'default';
      let currentMtl = 'iron_plate';

      const groupData = {};

      for (let line of lines) {
        line = line.trim();
        if (line.startsWith('v ')) {
          const parts = line.split(/\\s+/);
          positions.push([parseFloat(parts[1]), parseFloat(parts[2]), parseFloat(parts[3])]);
        } else if (line.startsWith('vt ')) {
          const parts = line.split(/\\s+/);
          uvs.push([parseFloat(parts[1]), parseFloat(parts[2])]);
        } else if (line.startsWith('g ')) {
          currentGroup = line.substring(2).trim();
        } else if (line.startsWith('usemtl ')) {
          currentMtl = line.substring(7).trim();
        } else if (line.startsWith('f ')) {
          if (!groupData[currentGroup]) groupData[currentGroup] = {};
          if (!groupData[currentGroup][currentMtl]) groupData[currentGroup][currentMtl] = { pos: [], uv: [] };

          const parts = line.split(/\\s+/).slice(1);
          if (parts.length === 4) {
            // Converter Quad em 2 Triângulos: (0, 1, 2) e (0, 2, 3)
            const addVert = (idxStr) => {
              const [pIdx, tIdx] = idxStr.split('/').map(n => parseInt(n) - 1);
              const p = positions[pIdx];
              const t = uvs[tIdx] || [0, 0];
              groupData[currentGroup][currentMtl].pos.push(p[0], p[1], p[2]);
              groupData[currentGroup][currentMtl].uv.push(t[0], t[1]);
            };
            // Tri 1
            addVert(parts[0]); addVert(parts[1]); addVert(parts[2]);
            // Tri 2
            addVert(parts[0]); addVert(parts[2]); addVert(parts[3]);
          } else if (parts.length === 3) {
            const addVert = (idxStr) => {
              const [pIdx, tIdx] = idxStr.split('/').map(n => parseInt(n) - 1);
              const p = positions[pIdx];
              const t = uvs[tIdx] || [0, 0];
              groupData[currentGroup][currentMtl].pos.push(p[0], p[1], p[2]);
              groupData[currentGroup][currentMtl].uv.push(t[0], t[1]);
            };
            addVert(parts[0]); addVert(parts[1]); addVert(parts[2]);
          }
        }
      }

      for (const [groupName, mtlMap] of Object.entries(groupData)) {
        const groupObj = new THREE.Group();
        groupObj.name = groupName;

        for (const [mtlName, data] of Object.entries(mtlMap)) {
          const geo = new THREE.BufferGeometry();
          geo.setAttribute('position', new THREE.Float32BufferAttribute(data.pos, 3));
          geo.setAttribute('uv', new THREE.Float32BufferAttribute(data.uv, 2));
          geo.computeVertexNormals();

          const mat = materials[mtlName] || materials['iron_plate'];
          const mesh = new THREE.Mesh(geo, mat);
          groupObj.add(mesh);
        }

        meshGroups[groupName] = groupObj;
        modelGroup.add(groupObj);
      }

      scene.add(modelGroup);
    }

    let waterMesh, weedMesh;
    function createFluidMeshes() {
      if (fluidGroup) scene.remove(fluidGroup);
      fluidGroup = new THREE.Group();

      const waterMat = new THREE.MeshStandardMaterial({
        color: 0x3880c8,
        transparent: true,
        opacity: 0.75,
        roughness: 0.1,
        metalness: 0.2
      });
      const weedMat = new THREE.MeshStandardMaterial({
        color: 0xa29130,
        transparent: true,
        opacity: 0.75,
        roughness: 0.1,
        metalness: 0.2
      });

      // Tanque Esquerdo (Água): X: -0.7 a 0.4, Y: 2.5 a 3.5625, Z: -0.7 a 1.7
      const waterGeo = new THREE.BoxGeometry(1.05, 1, 2.3);
      waterMesh = new THREE.Mesh(waterGeo, waterMat);
      waterMesh.position.set(-0.15, 3.0, 0.5);
      fluidGroup.add(waterMesh);

      // Tanque Direito (Weed-EX): X: 0.6 a 1.7, Y: 2.5 a 3.5625, Z: -0.7 a 1.7
      const weedGeo = new THREE.BoxGeometry(1.05, 1, 2.3);
      weedMesh = new THREE.Mesh(weedGeo, weedMat);
      weedMesh.position.set(1.15, 3.0, 0.5);
      fluidGroup.add(weedMesh);

      scene.add(fluidGroup);
      updateFluids();
    }

    function updateFluids() {
      const waterVal = parseInt(document.getElementById('rng-water').value);
      const weedVal = parseInt(document.getElementById('rng-weed').value);

      document.getElementById('lbl-water').innerText = waterVal.toLocaleString() + ' / 32,000 mB';
      document.getElementById('lbl-weed').innerText = weedVal.toLocaleString() + ' / 16,000 mB';

      const maxH = 1.0625;
      const baseH = 2.5;

      const waterFrac = waterVal / 32000;
      if (waterMesh) {
        if (waterFrac <= 0.001) {
          waterMesh.visible = false;
        } else {
          waterMesh.visible = true;
          const h = Math.max(0.05, maxH * waterFrac);
          waterMesh.scale.set(1, h, 1);
          waterMesh.position.y = baseH + h / 2;
        }
      }

      const weedFrac = weedVal / 16000;
      if (weedMesh) {
        if (weedFrac <= 0.001) {
          weedMesh.visible = false;
        } else {
          weedMesh.visible = true;
          const h = Math.max(0.05, maxH * weedFrac);
          weedMesh.scale.set(1, h, 1);
          weedMesh.position.y = baseH + h / 2;
        }
      }
    }

    function toggleFluids(visible) {
      if (fluidGroup) fluidGroup.visible = visible;
    }

    function setupUI() {
      const groupsList = document.getElementById('groups-list');
      groupsList.innerHTML = '';

      const labels = {
        'base_legs': 'Pés da Base (base_legs)',
        'bracing': 'Contraventamento (bracing)',
        'platform': 'Plataforma (platform)',
        'ladder': 'Escada de Acesso (ladder)',
        'controller_and_pipe': 'Controlador & Canos',
        'tank_body': 'Corpo do Tanque & Vidros',
        'roof': 'Teto / Cobertura (roof)',
        'railing': 'Guarda-corpo (railing)'
      };

      for (const groupName of Object.keys(meshGroups)) {
        const item = document.createElement('label');
        item.className = 'group-item';
        item.innerHTML = \`
          <span class="group-label">
            <input type="checkbox" checked id="chk-\${groupName}" onchange="toggleGroup('\${groupName}', this.checked)">
            \${labels[groupName] || groupName}
          </span>
          <span class="group-faces">Exibir</span>
        \`;
        groupsList.appendChild(item);
      }

      // Textures UI
      const texGrid = document.getElementById('tex-grid');
      texGrid.innerHTML = '';
      for (const [name, b64] of Object.entries(texturesBase64)) {
        const card = document.createElement('div');
        card.className = 'tex-card';
        card.id = 'card-' + name;
        card.onclick = () => selectTextureForEditor(name);
        card.innerHTML = \`
          <img class="tex-preview" id="preview-\${name}" src="\${b64}">
          <div class="tex-name">\${name.replace('water_tower_', '')}</div>
        \`;
        texGrid.appendChild(card);
      }
    }

    function toggleGroup(name, visible) {
      if (meshGroups[name]) meshGroups[name].visible = visible;
    }

    function toggleWireframe(enabled) {
      for (const mat of Object.values(materials)) {
        mat.wireframe = enabled;
      }
    }

    function toggleBounds(enabled) {
      if (boundsBox) boundsBox.visible = enabled;
    }

    function toggleGrid(enabled) {
      if (gridHelper) gridHelper.visible = enabled;
    }

    function toggleAutoRotate() {
      autoRotate = !autoRotate;
    }

    function setView(view) {
      controls.reset();
      controls.target.set(0.5, 2.5, 0.5);
      if (view === 'iso') camera.position.set(4, 5, 5);
      if (view === 'front') camera.position.set(0.5, 2.5, 6);
      if (view === 'side') camera.position.set(6, 2.5, 0.5);
      if (view === 'top') camera.position.set(0.5, 7.5, 0.51);
    }

    function switchTab(tabId) {
      document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      event.target.classList.add('active');
      document.getElementById('tab-' + tabId).classList.add('active');
    }

    // PIXEL EDITOR
    let activeTexName = null;
    let editCanvas, editCtx;
    let currentColor = '#ffffff';

    function selectTextureForEditor(name) {
      activeTexName = name;
      document.querySelectorAll('.tex-card').forEach(c => c.classList.remove('selected'));
      const card = document.getElementById('card-' + name);
      if (card) card.classList.add('selected');

      const section = document.getElementById('pixel-editor-section');
      section.style.display = 'block';
      document.getElementById('editor-tex-title').innerText = name + '.png';

      editCanvas = document.getElementById('pixelCanvas');
      editCtx = editCanvas.getContext('2d');

      const img = new Image();
      img.src = texturesBase64[name];
      img.onload = () => {
        editCtx.imageSmoothingEnabled = false;
        editCtx.clearRect(0, 0, 160, 160);
        editCtx.drawImage(img, 0, 0, 160, 160);
      };

      setupCanvasDrawing();
    }

    function setupCanvasDrawing() {
      let isDrawing = false;
      const draw = (e) => {
        if (!isDrawing) return;
        const rect = editCanvas.getBoundingClientRect();
        const x = Math.floor((e.clientX - rect.left) / 10) * 10;
        const y = Math.floor((e.clientY - rect.top) / 10) * 10;
        editCtx.fillStyle = document.getElementById('colorPicker').value;
        editCtx.fillRect(x, y, 10, 10);
      };

      editCanvas.onmousedown = (e) => { isDrawing = true; draw(e); };
      window.onmouseup = () => { isDrawing = false; };
      editCanvas.onmousemove = draw;
    }

    function applyEditedTexture() {
      if (!activeTexName) return;
      // Reduzir de 160x160 para 16x16
      const miniCanvas = document.createElement('canvas');
      miniCanvas.width = 16; miniCanvas.height = 16;
      const mCtx = miniCanvas.getContext('2d');
      mCtx.drawImage(editCanvas, 0, 0, 16, 16);

      const newB64 = miniCanvas.toDataURL('image/png');
      texturesBase64[activeTexName] = newB64;
      document.getElementById('preview-' + activeTexName).src = newB64;

      const img = new Image();
      img.src = newB64;
      img.onload = () => {
        if (textureObjects[activeTexName]) {
          textureObjects[activeTexName].image = img;
          textureObjects[activeTexName].needsUpdate = true;
        }
      };
    }

    function uploadTexturePrompt() {
      document.getElementById('texFileInput').click();
    }

    function loadCustomPNG(input) {
      if (input.files && input.files[0] && activeTexName) {
        const reader = new FileReader();
        reader.onload = (e) => {
          const img = new Image();
          img.src = e.target.result;
          img.onload = () => {
            editCtx.imageSmoothingEnabled = false;
            editCtx.clearRect(0, 0, 160, 160);
            editCtx.drawImage(img, 0, 0, 160, 160);
            applyEditedTexture();
          };
        };
        reader.readAsDataURL(input.files[0]);
      }
    }

    function downloadFile(filename, text) {
      const element = document.createElement('a');
      element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
      element.setAttribute('download', filename);
      element.style.display = 'none';
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
    }

    function onWindowResize() {
      const container = document.getElementById('viewport-container');
      camera.aspect = container.clientWidth / container.clientHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(container.clientWidth, container.clientHeight);
    }

    function animate() {
      requestAnimationFrame(animate);
      if (autoRotate && controls) {
        modelGroup.rotation.y += 0.005;
        if (boundsBox) boundsBox.rotation.y += 0.005;
        if (fluidGroup) fluidGroup.rotation.y += 0.005;
      }
      controls.update();
      renderer.render(scene, camera);
    }

    window.onload = init3D;
  </script>
</body>
</html>
`;

const outputPath = path.join(rootDir, 'tools/reservoir_3d_editor.html');
fs.writeFileSync(outputPath, htmlContent, 'utf8');
console.log('Successfully generated:', outputPath);
