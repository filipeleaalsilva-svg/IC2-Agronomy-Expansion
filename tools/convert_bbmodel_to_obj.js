const fs = require('fs');
const path = require('path');

const bbPath = process.argv[2] || path.join(__dirname, 'agricultural_reservoir_design_a_v2.bbmodel');
const model = JSON.parse(fs.readFileSync(bbPath, 'utf8'));

const groupNames = [
  'base_legs',
  'bracing',
  'platform',
  'controller_and_pipe',
  'tank_body',
  'roof',
  'railing'
];

const texIndexToMtl = {
  8: 'steel',
  1: 'wood',
  2: 'sheetmetal_copper',
  3: 'storage_copper',
  4: 'glass',
  5: 'brass',
  6: 'controller'
};

const elByUuid = {};
model.elements.forEach(el => elByUuid[el.uuid] = el);

function rotate(p, origin, rot) {
  let x = p[0] - origin[0];
  let y = p[1] - origin[1];
  let z = p[2] - origin[2];
  const toRad = Math.PI / 180;
  if (rot && rot[0]) {
    const r = rot[0] * toRad;
    const cy = y * Math.cos(r) - z * Math.sin(r);
    const cz = y * Math.sin(r) + z * Math.cos(r);
    y = cy; z = cz;
  }
  if (rot && rot[1]) {
    const r = rot[1] * toRad;
    const cx = x * Math.cos(r) + z * Math.sin(r);
    const cz = -x * Math.sin(r) + z * Math.cos(r);
    x = cx; z = cz;
  }
  if (rot && rot[2]) {
    const r = rot[2] * toRad;
    const cx = x * Math.cos(r) - y * Math.sin(r);
    const cy = x * Math.sin(r) + y * Math.cos(r);
    x = cx; y = cy;
  }
  return [
    parseFloat(((x + origin[0]) / 16).toFixed(4)),
    parseFloat(((y + origin[1]) / 16).toFixed(4)),
    parseFloat(((z + origin[2]) / 16).toFixed(4))
  ];
}

const objLines = [];
objLines.push('# Rural-IC2 Agricultural Reservoir (Design A v2) - 3x5x3 Multiblock');
objLines.push('mtllib agricultural_reservoir.mtl');

let vIndex = 0;
let vtIndex = 0;
let currentMtl = '';

// We can define standard 4 vt or per-face vt.
// ReservoirTowerModel reads vt u (1 - v).
// If we define a pool of vt coordinates or per face:
const vtMap = new Map();
function getVtIndex(u, v) {
  // Clamp or normalize UVs
  const uNorm = parseFloat((u / 16).toFixed(4));
  // In OBJ, V is 1 - v_pixel/16, so that ReservoirTowerModel does 1 - (1 - v_pixel/16) = v_pixel/16
  const vNorm = parseFloat((1 - v / 16).toFixed(4));
  const key = uNorm + ' ' + vNorm;
  if (vtMap.has(key)) {
    return vtMap.get(key);
  }
  vtIndex++;
  vtMap.set(key, vtIndex);
  objLines.push(`vt ${uNorm.toFixed(4)} ${vNorm.toFixed(4)}`);
  return vtIndex;
}

model.outliner.forEach((group, gIdx) => {
  const gName = groupNames[gIdx];
  objLines.push('');
  objLines.push(`g ${gName}`);

  for (const childId of group.children) {
    const el = elByUuid[childId];
    if (!el || el.export === false) continue;

    const from = el.from;
    const to = el.to;
    const origin = el.origin || [0, 0, 0];
    const rot = el.rotation || [0, 0, 0];

    // 8 cube corners:
    // v1: from.x, from.y, from.z
    // v2: to.x,   from.y, from.z
    // v3: to.x,   from.y, to.z
    // v4: from.x, from.y, to.z
    // v5: from.x, to.y,   from.z
    // v6: to.x,   to.y,   from.z
    // v7: to.x,   to.y,   to.z
    // v8: from.x, to.y,   to.z

    const p1 = rotate([from[0], from[1], from[2]], origin, rot);
    const p2 = rotate([to[0],   from[1], from[2]], origin, rot);
    const p3 = rotate([to[0],   from[1], to[2]],   origin, rot);
    const p4 = rotate([from[0], from[1], to[2]],   origin, rot);
    const p5 = rotate([from[0], to[1],   from[2]], origin, rot);
    const p6 = rotate([to[0],   to[1],   from[2]], origin, rot);
    const p7 = rotate([to[0],   to[1],   to[2]],   origin, rot);
    const p8 = rotate([from[0], to[1],   to[2]],   origin, rot);

    const baseV = vIndex;
    const pts = [p1, p2, p3, p4, p5, p6, p7, p8];
    for (const p of pts) {
      vIndex++;
      objLines.push(`v ${p[0].toFixed(4)} ${p[1].toFixed(4)} ${p[2].toFixed(4)}`);
    }

    // Face definitions matching standard outward winding order:
    // down:  1, 2, 3, 4
    // up:    8, 7, 6, 5
    // north: 5, 6, 2, 1
    // south: 7, 8, 4, 3
    // west:  8, 5, 1, 4
    // east:  6, 7, 3, 2

    const faceDefs = [
      {
        name: 'down',
        v: [baseV + 1, baseV + 2, baseV + 3, baseV + 4],
        uvOrder: (u1, v1, u2, v2) => [[u1, v1], [u2, v1], [u2, v2], [u1, v2]]
      },
      {
        name: 'up',
        v: [baseV + 8, baseV + 7, baseV + 6, baseV + 5],
        uvOrder: (u1, v1, u2, v2) => [[u1, v2], [u2, v2], [u2, v1], [u1, v1]]
      },
      {
        name: 'north',
        v: [baseV + 5, baseV + 6, baseV + 2, baseV + 1],
        uvOrder: (u1, v1, u2, v2) => [[u1, v1], [u2, v1], [u2, v2], [u1, v2]]
      },
      {
        name: 'south',
        v: [baseV + 7, baseV + 8, baseV + 4, baseV + 3],
        uvOrder: (u1, v1, u2, v2) => [[u1, v1], [u2, v1], [u2, v2], [u1, v2]]
      },
      {
        name: 'west',
        v: [baseV + 8, baseV + 5, baseV + 1, baseV + 4],
        uvOrder: (u1, v1, u2, v2) => [[u1, v1], [u2, v1], [u2, v2], [u1, v2]]
      },
      {
        name: 'east',
        v: [baseV + 6, baseV + 7, baseV + 3, baseV + 2],
        uvOrder: (u1, v1, u2, v2) => [[u1, v1], [u2, v1], [u2, v2], [u1, v2]]
      }
    ];

    for (const fDef of faceDefs) {
      const f = el.faces[fDef.name];
      if (!f || f.texture === undefined || f.texture === null) continue;

      const mtlName = texIndexToMtl[f.texture] || 'steel';
      if (mtlName !== currentMtl) {
        currentMtl = mtlName;
        objLines.push(`usemtl ${currentMtl}`);
      }

      const uv = f.uv || [0, 0, 16, 16];
      const u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];
      const uvCorners = fDef.uvOrder(u1, v1, u2, v2);

      const vtIndices = uvCorners.map(c => getVtIndex(c[0], c[1]));
      objLines.push(`f ${fDef.v[0]}/${vtIndices[0]} ${fDef.v[1]}/${vtIndices[1]} ${fDef.v[2]}/${vtIndices[2]} ${fDef.v[3]}/${vtIndices[3]}`);
    }
  }
});

const outPath = 'src/main/resources/assets/cropmanager/models/block/agricultural_reservoir.obj';
fs.writeFileSync(outPath, objLines.join('\n') + '\n', 'utf8');
console.log('Successfully wrote:', outPath, 'Total lines:', objLines.length, 'Vertices:', vIndex, 'TexCoords:', vtIndex);
