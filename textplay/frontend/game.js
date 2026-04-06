// ============================================================
// TextPlay - AI-Driven Sandbox Game Engine
// Designed for on-device FunctionGemma + Gemma 4 integration
// ============================================================

// ==================== CONSTANTS ====================
const TILE_SIZE = 40;
const MAP_COLS = 16;
const MAP_ROWS = 12;

const TILE = {
  GRASS: 0, TREE: 1, WATER: 2, ROCK: 3, PATH: 4,
  SAND: 5, WALL: 6, FLOOR: 7, BRIDGE: 8, FLOWER: 9,
};

const TILE_COLORS = {
  [TILE.GRASS]:  '#4a7c3f',
  [TILE.TREE]:   '#2d5a27',
  [TILE.WATER]:  '#3b6ba5',
  [TILE.ROCK]:   '#6b6b6b',
  [TILE.PATH]:   '#c4a35a',
  [TILE.SAND]:   '#d4b96a',
  [TILE.WALL]:   '#4a4a5a',
  [TILE.FLOOR]:  '#8b7355',
  [TILE.BRIDGE]: '#8b6914',
  [TILE.FLOWER]: '#5a8c4f',
};

const TILE_DETAIL = {
  [TILE.TREE]:  { char: '\u2660', color: '#1a4a14', size: 22 },  // tree top
  [TILE.WATER]: { char: '\u223c', color: '#5a9bd5', size: 16 },  // wave
  [TILE.ROCK]:  { char: '\u25cf', color: '#888', size: 14 },
  [TILE.FLOWER]:{ char: '\u273f', color: '#f06595', size: 14 },
};

const WALKABLE = new Set([TILE.GRASS, TILE.PATH, TILE.SAND, TILE.FLOOR, TILE.BRIDGE, TILE.FLOWER]);

// ==================== MAP DATA (16x12) ====================
const MAP_DATA = [
  [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
  [1,0,0,9,0,0,1,1,0,0,0,0,9,0,0,1],
  [1,0,0,0,0,4,4,4,4,0,0,7,7,7,0,1],
  [1,9,0,0,0,4,0,0,4,0,0,7,6,7,0,1],
  [1,0,0,0,0,4,0,0,4,0,0,7,7,7,0,1],
  [1,0,0,4,4,4,0,0,4,4,4,4,0,0,0,1],
  [1,0,0,4,0,0,0,0,0,0,0,4,0,0,0,1],
  [1,0,0,4,0,0,0,8,0,0,0,4,4,4,0,1],
  [1,0,0,4,4,0,0,2,2,0,0,0,0,4,0,1],
  [1,0,0,0,4,0,2,2,2,2,0,0,0,4,0,1],
  [1,0,9,0,4,4,0,2,2,0,0,9,0,0,0,1],
  [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
];

// ==================== DIRECTION HELPERS ====================
const DIR = {
  north: { dx: 0, dy: -1 }, south: { dx: 0, dy: 1 },
  east:  { dx: 1, dy: 0 },  west:  { dx: -1, dy: 0 },
  up: { dx: 0, dy: -1 }, down: { dx: 0, dy: 1 },
  left: { dx: -1, dy: 0 }, right: { dx: 1, dy: 0 },
};

// ==================== GAME OBJECTS ====================
const INITIAL_OBJECTS = [
  { id: 'berry_bush_1', type: 'berry_bush', x: 3, y: 1, name: 'Berry Bush',
    icon: '\ud83c\udf53', desc: 'A bush full of ripe red berries.', harvestItem: 'berry', harvested: false },
  { id: 'berry_bush_2', type: 'berry_bush', x: 12, y: 1, name: 'Berry Bush',
    icon: '\ud83c\udf53', desc: 'Wild berries growing beside the path.', harvestItem: 'berry', harvested: false },
  { id: 'chest_1', type: 'chest', x: 12, y: 3, name: 'Old Chest',
    icon: '\ud83d\udce6', desc: 'A dusty wooden chest. It seems unlocked.', items: ['rusty_key'], opened: false },
  { id: 'sign_1', type: 'sign', x: 5, y: 6, name: 'Wooden Sign',
    icon: '\ud83e\udea7', desc: 'A weathered sign reads: "Village Center \u2192  |  Cave \u2191  |  Lake \u2193"' },
  { id: 'campfire', type: 'campfire', x: 7, y: 6, name: 'Campfire',
    icon: '\ud83d\udd25', desc: 'A warm campfire. You could cook something here.' },
  { id: 'well', type: 'well', x: 10, y: 6, name: 'Stone Well',
    icon: '\ud83e\udea3', desc: 'An old stone well with clear water.', hasWater: true },
  { id: 'herb_patch', type: 'herb', x: 2, y: 10, name: 'Herb Patch',
    icon: '\ud83c\udf3f', desc: 'Fragrant herbs growing wild.', harvestItem: 'herb', harvested: false },
  { id: 'locked_gate', type: 'gate', x: 12, y: 2, name: 'Iron Gate',
    icon: '\ud83d\udeaa', desc: 'A heavy iron gate blocks the entrance to the cave.', locked: true },
  { id: 'torch_holder', type: 'pickup', x: 3, y: 5, name: 'Wall Torch',
    icon: '\ud83d\udd26', desc: 'A torch mounted on a wooden post.', item: 'torch', taken: false },
];

const INITIAL_NPCS = [
  { id: 'farmer', x: 6, y: 3, name: 'Farmer Lin',
    icon: '\ud83d\udc68\u200d\ud83c\udf3e', color: '#51cf66',
    dialogue: {
      default: '"Welcome, traveler! I\'m Farmer Lin. These berry bushes are ripe for picking. Could you gather some berries for me? I\'ll teach you a recipe in return."',
      hasBerry: '"Wonderful! You found berries! Here\'s a tip: cook them at the campfire for a tasty treat. Also, the Elder might have some wisdom for you."',
      quest_done: '"Thanks for all your help! The village is better for it."',
    }
  },
  { id: 'elder', x: 8, y: 3, name: 'Elder Sage',
    icon: '\ud83e\uddd9', color: '#fcc419',
    dialogue: {
      default: '"Greetings, young one. There is a cave to the north, sealed by an iron gate. The key was lost long ago... perhaps hidden in an old chest nearby. Combine herbs with water to make a potion - you may need it."',
      hasKey: '"You found the key! The cave holds ancient secrets. Be prepared before you enter."',
      quest_done: '"You\'ve proven yourself worthy. The cave awaits your courage."',
    }
  },
  { id: 'merchant', x: 10, y: 7, name: 'Merchant Tao',
    icon: '\ud83e\uddd1\u200d\ud83d\udcbc', color: '#da77f2',
    dialogue: {
      default: '"Looking to trade? I deal in useful items. Bring me a cooked berry and I\'ll give you something special. Try looking around the campfire area!"',
      hasCookedBerry: '"A cooked berry! Delicious. Here, take this magic coin. It might be useful later..."',
      traded: '"A pleasure doing business! Good luck on your adventure."',
    }
  },
];

// ==================== ITEM DATABASE ====================
const ITEMS = {
  berry:        { name: 'Berry',         icon: '\ud83c\udf53' },
  rusty_key:    { name: 'Rusty Key',     icon: '\ud83d\udddd\ufe0f' },
  torch:        { name: 'Torch',         icon: '\ud83d\udd26' },
  herb:         { name: 'Herb',          icon: '\ud83c\udf3f' },
  water_bucket: { name: 'Water Bucket',  icon: '\ud83e\udea3' },
  cooked_berry: { name: 'Cooked Berry',  icon: '\ud83c\udf72' },
  potion:       { name: 'Health Potion', icon: '\ud83e\uddea' },
  magic_coin:   { name: 'Magic Coin',    icon: '\ud83e\ude99' },
};

// ==================== CRAFTING RECIPES ====================
const RECIPES = [
  { ingredients: ['berry', 'campfire'], result: 'cooked_berry', station: 'campfire',
    msg: 'You roast the berry over the fire. It smells delicious!' },
  { ingredients: ['herb', 'water_bucket'], result: 'potion', station: null,
    msg: 'You mix the herb into the water. It glows faintly - a health potion!' },
];

// ==================== QUEST DEFINITIONS ====================
const QUEST_DEFS = [
  { id: 'gather_berries', name: 'Gather Berries', desc: 'Pick berries for Farmer Lin.',
    check: (game) => game.player.inventory.includes('berry') },
  { id: 'find_key', name: 'Find the Key', desc: 'Find the key to the cave gate.',
    check: (game) => game.player.inventory.includes('rusty_key') },
  { id: 'brew_potion', name: 'Brew a Potion', desc: 'Combine herb and water to make a potion.',
    check: (game) => game.player.inventory.includes('potion') },
  { id: 'open_cave', name: 'Enter the Cave', desc: 'Use the key to unlock the cave gate.',
    check: (game) => {
      const gate = game.objects.find(o => o.id === 'locked_gate');
      return gate && !gate.locked;
    }
  },
];

// ==================== PLAYER CLASS ====================
class Player {
  constructor(x, y) {
    this.x = x;
    this.y = y;
    this.inventory = [];
    this.health = 100;
    this.maxHealth = 100;
    this.facing = 'south';
  }
}

// ==================== GAME ENGINE ====================
class TextPlayGame {
  constructor() {
    this.canvas = document.getElementById('game-canvas');
    this.ctx = this.canvas.getContext('2d');
    this.chatMessages = document.getElementById('chat-messages');
    this.inventoryList = document.getElementById('inventory-list');
    this.questList = document.getElementById('quest-list');

    this.player = new Player(5, 7);
    this.objects = JSON.parse(JSON.stringify(INITIAL_OBJECTS));
    this.npcs = JSON.parse(JSON.stringify(INITIAL_NPCS));
    this.quests = QUEST_DEFS.map(q => ({ ...q, completed: false }));
    this.turn = 0;
    this.gameWon = false;

    // Animation state
    this.animPlayer = { x: this.player.x, y: this.player.y };
    this.animFrame = 0;
    this.waterOffset = 0;

    this.init();
  }

  init() {
    this.addMessage('system', 'Welcome to TextPlay! You awaken in a small village clearing.');
    this.addMessage('system', 'Type natural language commands to explore. Try: "look around", "walk north", "pick up berries"');
    this.addMessage('system', 'Talk to the villagers to discover quests and secrets.');
    this.updateUI();
    this.startRenderLoop();
  }

  // ==================== RENDERING ====================
  startRenderLoop() {
    const loop = () => {
      this.animFrame++;
      if (this.animFrame % 30 === 0) this.waterOffset = (this.waterOffset + 1) % 3;

      // Smooth player movement
      const speed = 0.15;
      this.animPlayer.x += (this.player.x - this.animPlayer.x) * speed;
      this.animPlayer.y += (this.player.y - this.animPlayer.y) * speed;

      this.render();
      requestAnimationFrame(loop);
    };
    loop();
  }

  render() {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    // Draw tiles
    for (let y = 0; y < MAP_ROWS; y++) {
      for (let x = 0; x < MAP_COLS; x++) {
        const tile = MAP_DATA[y][x];
        const px = x * TILE_SIZE;
        const py = y * TILE_SIZE;

        // Base color
        ctx.fillStyle = TILE_COLORS[tile] || '#333';
        ctx.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // Grid lines (subtle)
        ctx.strokeStyle = 'rgba(0,0,0,0.15)';
        ctx.strokeRect(px, py, TILE_SIZE, TILE_SIZE);

        // Tile detail characters
        const detail = TILE_DETAIL[tile];
        if (detail) {
          ctx.fillStyle = detail.color;
          ctx.font = `${detail.size}px serif`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          // Animate water
          const offX = tile === TILE.WATER ? Math.sin(this.animFrame * 0.05 + x) * 2 : 0;
          ctx.fillText(detail.char, px + TILE_SIZE / 2 + offX, py + TILE_SIZE / 2);
        }

        // Grass variation
        if (tile === TILE.GRASS && ((x + y) % 3 === 0)) {
          ctx.fillStyle = 'rgba(80,140,60,0.5)';
          ctx.fillRect(px + 10, py + 10, 3, 8);
          ctx.fillRect(px + 25, py + 20, 3, 6);
        }
      }
    }

    // Draw objects
    for (const obj of this.objects) {
      if (obj.taken) continue;
      const px = obj.x * TILE_SIZE;
      const py = obj.y * TILE_SIZE;
      ctx.font = '24px serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';

      // Harvested/opened state visual
      if (obj.harvested) {
        ctx.globalAlpha = 0.4;
      }
      if (obj.type === 'gate' && !obj.locked) {
        ctx.globalAlpha = 0.3;
      }

      ctx.fillText(obj.icon, px + TILE_SIZE / 2, py + TILE_SIZE / 2);
      ctx.globalAlpha = 1;
    }

    // Draw NPCs
    for (const npc of this.npcs) {
      const px = npc.x * TILE_SIZE;
      const py = npc.y * TILE_SIZE;
      // NPC glow
      ctx.fillStyle = npc.color + '33';
      ctx.beginPath();
      ctx.arc(px + TILE_SIZE / 2, py + TILE_SIZE / 2, TILE_SIZE / 2 - 2, 0, Math.PI * 2);
      ctx.fill();
      // NPC icon
      ctx.font = '24px serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      const bounce = Math.sin(this.animFrame * 0.04 + npc.x) * 2;
      ctx.fillText(npc.icon, px + TILE_SIZE / 2, py + TILE_SIZE / 2 + bounce);
      // NPC name
      ctx.font = '10px sans-serif';
      ctx.fillStyle = npc.color;
      ctx.fillText(npc.name, px + TILE_SIZE / 2, py - 4);
    }

    // Draw player
    const ppx = this.animPlayer.x * TILE_SIZE;
    const ppy = this.animPlayer.y * TILE_SIZE;
    // Shadow
    ctx.fillStyle = 'rgba(0,0,0,0.3)';
    ctx.beginPath();
    ctx.ellipse(ppx + TILE_SIZE / 2, ppy + TILE_SIZE - 4, 12, 5, 0, 0, Math.PI * 2);
    ctx.fill();
    // Body
    ctx.font = '26px serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const playerBounce = Math.sin(this.animFrame * 0.08) * 1.5;
    ctx.fillText('\ud83e\uddd1\u200d\ud83e\uddb1', ppx + TILE_SIZE / 2, ppy + TILE_SIZE / 2 + playerBounce);

    // Highlight adjacent interactables
    this.getAdjacentObjects().forEach(obj => {
      const ox = obj.x * TILE_SIZE;
      const oy = obj.y * TILE_SIZE;
      ctx.strokeStyle = '#fcc419';
      ctx.lineWidth = 2;
      ctx.setLineDash([4, 4]);
      ctx.strokeRect(ox + 2, oy + 2, TILE_SIZE - 4, TILE_SIZE - 4);
      ctx.setLineDash([]);
      ctx.lineWidth = 1;
    });
  }

  // ==================== GAME LOGIC ====================
  isWalkable(x, y) {
    if (x < 0 || x >= MAP_COLS || y < 0 || y >= MAP_ROWS) return false;
    const tile = MAP_DATA[y][x];
    if (!WALKABLE.has(tile)) return false;
    // Check locked gate
    const gate = this.objects.find(o => o.type === 'gate' && o.x === x && o.y === y && o.locked);
    if (gate) return false;
    // Check NPCs blocking
    const npc = this.npcs.find(n => n.x === x && n.y === y);
    if (npc) return false;
    return true;
  }

  getAdjacentObjects() {
    const { x, y } = this.player;
    return [...this.objects, ...this.npcs].filter(o => {
      if (o.taken) return false;
      const dist = Math.abs(o.x - x) + Math.abs(o.y - y);
      return dist <= 1;
    });
  }

  findObjectByName(name) {
    const lower = name.toLowerCase();
    return this.objects.find(o =>
      !o.taken && (o.name.toLowerCase().includes(lower) || o.type.includes(lower) || o.id.includes(lower))
    );
  }

  findNPCByName(name) {
    const lower = name.toLowerCase();
    return this.npcs.find(n =>
      n.name.toLowerCase().includes(lower) || n.id.includes(lower)
    );
  }

  isAdjacent(target) {
    const dist = Math.abs(target.x - this.player.x) + Math.abs(target.y - this.player.y);
    return dist <= 1;
  }

  // ==================== PLAYER ACTIONS ====================
  movePlayer(direction, steps = 1) {
    const dir = DIR[direction.toLowerCase()];
    if (!dir) {
      return this.result('error', `Unknown direction: "${direction}". Try north, south, east, west.`);
    }

    let moved = 0;
    for (let i = 0; i < Math.min(steps, 5); i++) {
      const nx = this.player.x + dir.dx;
      const ny = this.player.y + dir.dy;
      if (!this.isWalkable(nx, ny)) {
        const tile = MAP_DATA[ny]?.[nx];
        if (tile === TILE.WATER) {
          return this.result('error', `You can't walk into the water! ${moved > 0 ? `(Moved ${moved} step${moved > 1 ? 's' : ''})` : ''}`);
        }
        if (tile === TILE.TREE) {
          return this.result('error', `Dense trees block your path. ${moved > 0 ? `(Moved ${moved} step${moved > 1 ? 's' : ''})` : ''}`);
        }
        const gate = this.objects.find(o => o.type === 'gate' && o.x === nx && o.y === ny && o.locked);
        if (gate) {
          return this.result('error', `The iron gate is locked! You need a key to open it. ${moved > 0 ? `(Moved ${moved} step${moved > 1 ? 's' : ''})` : ''}`);
        }
        return this.result('error', `Something blocks your way. ${moved > 0 ? `(Moved ${moved} step${moved > 1 ? 's' : ''})` : 'You cannot move there.'}`);
      }
      this.player.x = nx;
      this.player.y = ny;
      this.player.facing = direction.toLowerCase();
      moved++;
    }

    this.turn++;
    const nearby = this.describeNearby();
    return this.result('action', `You walk ${direction}${steps > 1 ? ` ${moved} steps` : ''}. ${nearby}`);
  }

  lookAround() {
    const { x, y } = this.player;
    const lines = [`You are at position (${x}, ${y}).`];

    // Describe tile
    const tile = MAP_DATA[y][x];
    const tileNames = { [TILE.GRASS]: 'grass', [TILE.PATH]: 'a dirt path', [TILE.SAND]: 'sandy ground',
      [TILE.FLOOR]: 'a stone floor', [TILE.BRIDGE]: 'a wooden bridge', [TILE.FLOWER]: 'a flower field' };
    lines.push(`You stand on ${tileNames[tile] || 'open ground'}.`);

    // Nearby objects
    const nearbyObjs = this.objects.filter(o => !o.taken && Math.abs(o.x - x) + Math.abs(o.y - y) <= 2);
    if (nearbyObjs.length > 0) {
      lines.push('Nearby: ' + nearbyObjs.map(o => `${o.icon} ${o.name}`).join(', '));
    }

    // Nearby NPCs
    const nearbyNPCs = this.npcs.filter(n => Math.abs(n.x - x) + Math.abs(n.y - y) <= 3);
    if (nearbyNPCs.length > 0) {
      lines.push('People: ' + nearbyNPCs.map(n => `${n.icon} ${n.name}`).join(', '));
    }

    // Directions
    const dirs = [];
    if (this.isWalkable(x, y - 1)) dirs.push('north');
    if (this.isWalkable(x, y + 1)) dirs.push('south');
    if (this.isWalkable(x + 1, y)) dirs.push('east');
    if (this.isWalkable(x - 1, y)) dirs.push('west');
    lines.push(`You can move: ${dirs.join(', ')}.`);

    return this.result('system', lines.join(' '));
  }

  pickupItem(itemName) {
    const lower = itemName.toLowerCase();

    // Check berry bush / herb
    const harvestable = this.objects.find(o =>
      !o.taken && !o.harvested && o.harvestItem &&
      (o.type.includes(lower) || o.name.toLowerCase().includes(lower) || o.harvestItem.includes(lower))
    );
    if (harvestable) {
      if (!this.isAdjacent(harvestable)) {
        return this.result('error', `The ${harvestable.name} is too far away. Walk closer first.`);
      }
      harvestable.harvested = true;
      this.player.inventory.push(harvestable.harvestItem);
      this.turn++;
      this.checkQuests();
      return this.result('action', `You pick ${ITEMS[harvestable.harvestItem]?.icon || ''} ${ITEMS[harvestable.harvestItem]?.name || harvestable.harvestItem} from the ${harvestable.name}.`);
    }

    // Check pickup objects (torch, etc.)
    const pickup = this.objects.find(o =>
      !o.taken && o.type === 'pickup' &&
      (o.name.toLowerCase().includes(lower) || o.item?.includes(lower))
    );
    if (pickup) {
      if (!this.isAdjacent(pickup)) {
        return this.result('error', `The ${pickup.name} is too far away. Walk closer first.`);
      }
      pickup.taken = true;
      this.player.inventory.push(pickup.item);
      this.turn++;
      return this.result('action', `You take the ${ITEMS[pickup.item]?.icon || ''} ${ITEMS[pickup.item]?.name || pickup.item}.`);
    }

    // Check chest
    const chest = this.objects.find(o =>
      o.type === 'chest' && !o.opened &&
      (o.name.toLowerCase().includes(lower) || lower.includes('chest'))
    );
    if (chest) {
      if (!this.isAdjacent(chest)) {
        return this.result('error', `The ${chest.name} is too far away. Walk closer first.`);
      }
      chest.opened = true;
      chest.items.forEach(item => this.player.inventory.push(item));
      this.turn++;
      this.checkQuests();
      const itemNames = chest.items.map(i => `${ITEMS[i]?.icon || ''} ${ITEMS[i]?.name || i}`).join(', ');
      return this.result('action', `You open the chest and find: ${itemNames}!`);
    }

    return this.result('error', `There's nothing called "${itemName}" to pick up nearby.`);
  }

  useItem(itemName, targetName) {
    const lower = itemName.toLowerCase();
    const targetLower = targetName.toLowerCase();

    if (!this.player.inventory.some(i => i.toLowerCase().includes(lower))) {
      return this.result('error', `You don't have "${itemName}" in your inventory.`);
    }

    // Use key on gate
    if (lower.includes('key') && targetLower.includes('gate')) {
      const gate = this.objects.find(o => o.type === 'gate');
      if (!gate) return this.result('error', 'There is no gate here.');
      if (!this.isAdjacent(gate)) return this.result('error', 'The gate is too far away. Walk closer first.');
      if (!gate.locked) return this.result('system', 'The gate is already unlocked.');

      gate.locked = false;
      this.player.inventory = this.player.inventory.filter(i => i !== 'rusty_key');
      this.turn++;
      this.checkQuests();
      return this.result('action', 'You insert the rusty key into the lock. *Click!* The iron gate swings open, revealing a dark cave entrance!');
    }

    // Use potion
    if (lower.includes('potion')) {
      this.player.inventory = this.player.inventory.filter(i => i !== 'potion');
      this.player.health = Math.min(this.player.maxHealth, this.player.health + 30);
      this.turn++;
      return this.result('action', `You drink the potion. Health restored! (HP: ${this.player.health})`);
    }

    // Check crafting at campfire
    if (targetLower.includes('campfire') || targetLower.includes('fire')) {
      return this.craftItems(itemName, 'campfire');
    }

    return this.result('error', `You can't figure out how to use ${itemName} on ${targetName}.`);
  }

  talkToNPC(npcName) {
    const npc = this.findNPCByName(npcName);
    if (!npc) return this.result('error', `There's no one called "${npcName}" nearby.`);
    if (!this.isAdjacent(npc)) return this.result('error', `${npc.name} is too far away. Walk closer first.`);

    // Determine dialogue state
    let dialogueKey = 'default';

    if (npc.id === 'farmer') {
      if (this.quests.find(q => q.id === 'open_cave')?.completed) dialogueKey = 'quest_done';
      else if (this.player.inventory.includes('berry')) dialogueKey = 'hasBerry';
    } else if (npc.id === 'elder') {
      if (this.quests.find(q => q.id === 'open_cave')?.completed) dialogueKey = 'quest_done';
      else if (this.player.inventory.includes('rusty_key')) dialogueKey = 'hasKey';
    } else if (npc.id === 'merchant') {
      if (npc.traded) dialogueKey = 'traded';
      else if (this.player.inventory.includes('cooked_berry')) {
        dialogueKey = 'hasCookedBerry';
        // Trade
        this.player.inventory = this.player.inventory.filter(i => i !== 'cooked_berry');
        this.player.inventory.push('magic_coin');
        npc.traded = true;
      }
    }

    this.turn++;
    const dialogue = npc.dialogue[dialogueKey] || npc.dialogue.default;
    return this.result('npc', `${npc.icon} ${npc.name}: ${dialogue}`);
  }

  craftItems(item1, item2) {
    const lower1 = item1.toLowerCase();
    const lower2 = item2.toLowerCase();

    // Find matching recipe
    const recipe = RECIPES.find(r => {
      const [a, b] = r.ingredients;
      return (lower1.includes(a) && lower2.includes(b)) || (lower1.includes(b) && lower2.includes(a));
    });

    if (!recipe) {
      return this.result('error', `You can't combine ${item1} with ${item2}. No known recipe.`);
    }

    // Check if near station
    if (recipe.station) {
      const station = this.objects.find(o => o.type === recipe.station);
      if (station && !this.isAdjacent(station)) {
        return this.result('error', `You need to be near the ${station.name} to craft this.`);
      }
    }

    // Check inventory for non-station ingredient
    const consumeItem = recipe.ingredients.find(i => i !== recipe.station);
    if (consumeItem && !this.player.inventory.includes(consumeItem)) {
      return this.result('error', `You don't have ${ITEMS[consumeItem]?.name || consumeItem} in your inventory.`);
    }

    // Craft
    if (consumeItem) {
      this.player.inventory = this.player.inventory.filter(i => i !== consumeItem);
    }
    this.player.inventory.push(recipe.result);
    this.turn++;
    this.checkQuests();

    return this.result('action', `${ITEMS[recipe.result]?.icon || ''} ${recipe.msg}`);
  }

  checkInventory() {
    if (this.player.inventory.length === 0) {
      return this.result('system', 'Your inventory is empty.');
    }
    const items = this.player.inventory.map(i => `${ITEMS[i]?.icon || ''} ${ITEMS[i]?.name || i}`);
    return this.result('system', `Inventory: ${items.join(', ')}`);
  }

  interact(targetName) {
    const lower = targetName.toLowerCase();

    // Try object
    const obj = this.findObjectByName(targetName);
    if (obj && this.isAdjacent(obj)) {
      if (obj.type === 'sign') {
        this.turn++;
        return this.result('system', `You read the sign: ${obj.desc}`);
      }
      if (obj.type === 'well' && obj.hasWater) {
        this.player.inventory.push('water_bucket');
        obj.hasWater = false;
        this.turn++;
        return this.result('action', `You draw water from the well. Got ${ITEMS.water_bucket.icon} Water Bucket!`);
      }
      if (obj.type === 'chest' && !obj.opened) {
        return this.pickupItem('chest');
      }
      return this.result('system', obj.desc);
    }

    // Try NPC
    const npc = this.findNPCByName(targetName);
    if (npc) return this.talkToNPC(targetName);

    return this.result('error', `You don't see "${targetName}" nearby.`);
  }

  // ==================== QUEST SYSTEM ====================
  checkQuests() {
    for (const quest of this.quests) {
      if (!quest.completed && quest.check(this)) {
        quest.completed = true;
        this.addMessage('action', `Quest completed: "${quest.name}"!`);
        // Check win condition
        if (quest.id === 'open_cave') {
          this.addMessage('system', '--- Congratulations! You\'ve unlocked the cave! ---');
          this.addMessage('system', 'This concludes the TextPlay prototype demo. In the full version, the cave leads to a vast underground world powered by Gemma 4\'s reasoning!');
          this.gameWon = true;
        }
      }
    }
    this.updateUI();
  }

  // ==================== HELPERS ====================
  describeNearby() {
    const nearby = this.getAdjacentObjects();
    if (nearby.length === 0) return '';
    const names = nearby.map(o => o.icon + ' ' + (o.name || o.id)).slice(0, 3);
    return `You see nearby: ${names.join(', ')}.`;
  }

  result(type, message) {
    this.addMessage(type, message);
    this.updateUI();
    return { success: type !== 'error', message, type };
  }

  addMessage(type, text) {
    const div = document.createElement('div');
    div.className = `msg ${type}`;
    const prefix = {
      system: '[System]', action: '[Action]', npc: '', error: '[!]', user: '[You]', think: '[Thinking]'
    };
    div.innerHTML = `<span class="prefix">${prefix[type] || ''}</span>${text}`;
    this.chatMessages.appendChild(div);
    this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
  }

  updateUI() {
    // Inventory
    this.inventoryList.innerHTML = '';
    this.player.inventory.forEach(item => {
      const li = document.createElement('li');
      const info = ITEMS[item] || { name: item, icon: '?' };
      li.innerHTML = `<span class="item-icon">${info.icon}</span> ${info.name}`;
      this.inventoryList.appendChild(li);
    });

    // Quests
    this.questList.innerHTML = '';
    this.quests.forEach(q => {
      const li = document.createElement('li');
      li.className = q.completed ? 'completed' : '';
      li.textContent = q.completed ? `\u2713 ${q.name}` : q.name;
      this.questList.appendChild(li);
    });

    // Status bar
    document.getElementById('hp-display').textContent = `HP: ${this.player.health}`;
    document.getElementById('pos-display').textContent = `Pos: ${this.player.x},${this.player.y}`;
    document.getElementById('turn-display').textContent = `Turn: ${this.turn}`;
  }

  getState() {
    return {
      player: { x: this.player.x, y: this.player.y, hp: this.player.health, inventory: this.player.inventory },
      turn: this.turn,
      quests: this.quests.map(q => ({ id: q.id, completed: q.completed })),
      objects: this.objects.filter(o => o.harvested || o.opened || !o.locked || o.taken).map(o => o.id),
    };
  }
}

// ==================== COMMAND BRIDGE ====================
// Interface for Kotlin WebView: textPlay.runCommands(jsonStr)
// Also used by browser-mode command parser below
const textPlay = {
  game: null,

  init() {
    this.game = new TextPlayGame();
  },

  // Called from Kotlin: textPlay.runCommands('[{"action":"move","direction":"north"}]')
  runCommands(jsonStr) {
    const commands = JSON.parse(jsonStr);
    const results = [];
    for (const cmd of commands) {
      results.push(this.executeCommand(cmd));
    }
    return JSON.stringify(results);
  },

  executeCommand(cmd) {
    const g = this.game;
    switch (cmd.action) {
      case 'move':       return g.movePlayer(cmd.direction, cmd.steps || 1);
      case 'look':       return g.lookAround();
      case 'pickup':     return g.pickupItem(cmd.item);
      case 'use':        return g.useItem(cmd.item, cmd.target);
      case 'talk':       return g.talkToNPC(cmd.npc);
      case 'craft':      return g.craftItems(cmd.item1, cmd.item2);
      case 'inventory':  return g.checkInventory();
      case 'interact':   return g.interact(cmd.target);
      case 'message':
        g.addMessage(cmd.type || 'npc', cmd.text);
        return { success: true, message: cmd.text };
      default:
        return g.result('error', `Unknown action: ${cmd.action}`);
    }
  },

  getGameState() {
    return JSON.stringify(this.game.getState());
  },
};

// ==================== BROWSER-MODE: LOCAL COMMAND PARSER ====================
// Simulates FunctionGemma's function calling for browser testing
function parseUserInput(input) {
  const text = input.trim().toLowerCase();

  // Movement
  const moveMatch = text.match(/(?:walk|move|go|run|step)\s+(\w+)(?:\s+(\d+))?/);
  if (moveMatch) return { action: 'move', direction: moveMatch[1], steps: parseInt(moveMatch[2]) || 1 };
  // Simple direction
  if (DIR[text]) return { action: 'move', direction: text, steps: 1 };

  // Look
  if (/^(?:look|observe|examine|scan|survey|check surroundings)/.test(text)) return { action: 'look' };

  // Inventory
  if (/^(?:inventory|items|bag|backpack|check inv)/.test(text)) return { action: 'inventory' };

  // Pickup
  const pickupMatch = text.match(/(?:pick\s*up|grab|take|gather|harvest|collect|get)\s+(?:the\s+|a\s+|some\s+)?(.+)/);
  if (pickupMatch) return { action: 'pickup', item: pickupMatch[1] };

  // Open (chest)
  const openMatch = text.match(/(?:open)\s+(?:the\s+)?(.+)/);
  if (openMatch) return { action: 'pickup', item: openMatch[1] };

  // Use item on target
  const useMatch = text.match(/(?:use|apply|put)\s+(?:the\s+|a\s+)?(.+?)\s+(?:on|at|with|into)\s+(?:the\s+|a\s+)?(.+)/);
  if (useMatch) return { action: 'use', item: useMatch[1], target: useMatch[2] };

  // Talk
  const talkMatch = text.match(/(?:talk|speak|chat|ask|greet|hello)\s+(?:to\s+|with\s+)?(?:the\s+)?(.+)/);
  if (talkMatch) return { action: 'talk', npc: talkMatch[1] };

  // Craft
  const craftMatch = text.match(/(?:craft|cook|combine|mix|brew)\s+(?:the\s+|a\s+)?(.+?)\s+(?:with|and|at|on|over)\s+(?:the\s+|a\s+)?(.+)/);
  if (craftMatch) return { action: 'craft', item1: craftMatch[1], item2: craftMatch[2] };

  // Draw water
  if (/(?:draw|fetch)\s+water/.test(text) || /use\s+(?:the\s+)?well/.test(text)) {
    return { action: 'interact', target: 'well' };
  }

  // Read sign
  if (/(?:read)\s+(?:the\s+)?(.+)/.test(text)) {
    const match = text.match(/read\s+(?:the\s+)?(.+)/);
    return { action: 'interact', target: match[1] };
  }

  // Generic interact
  const interactMatch = text.match(/(?:interact|check|examine|inspect|touch)\s+(?:the\s+|a\s+)?(.+)/);
  if (interactMatch) return { action: 'interact', target: interactMatch[1] };

  return null;
}

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
  textPlay.init();

  const form = document.getElementById('chat-form');
  const input = document.getElementById('chat-input');

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const text = input.value.trim();
    if (!text) return;

    textPlay.game.addMessage('user', text);
    input.value = '';

    // Parse locally (in production, FunctionGemma handles this)
    const cmd = parseUserInput(text);
    if (cmd) {
      textPlay.executeCommand(cmd);
    } else {
      // Simulate Gemma 4 "thinking mode" response
      textPlay.game.addMessage('think', `Hmm, I'm not sure how to "${text}". Try commands like: walk north, pick up berries, talk to farmer, look around, use key on gate.`);
    }
  });

  // Keyboard shortcut: arrow keys for movement
  document.addEventListener('keydown', (e) => {
    if (document.activeElement === input) return;
    const keyDir = { ArrowUp: 'north', ArrowDown: 'south', ArrowLeft: 'west', ArrowRight: 'east' };
    if (keyDir[e.key]) {
      e.preventDefault();
      textPlay.executeCommand({ action: 'move', direction: keyDir[e.key] });
    }
  });
});
