const CONFIG = {
  productionRotationMs: 30 * 60 * 1000,
  demoRotationMs: 18 * 1000,
  timings: {
    awaken: 720,
    coalesce: 1450,
    manifest: 650,
    hold: 6500,
    dissolve: 1450,
    rest: 850,
  },
  particles: {
    min: 110,
    max: 220,
    sampleStep: 5,
    dprCap: 2,
  },
};

const demoMode = new URLSearchParams(location.search).has('demo');
const motionQuery = matchMedia('(prefers-reduced-motion: reduce)');
let reducedMotion = motionQuery.matches;

const notes = [
  {
    id: 'demo-001-note-003',
    text: 'Some memories do not fade. They wait for the right silence to return.',
    book: 'Sample Library Note',
    location: 'Chapter 12 · Highlight 3',
    kind: 'highlight',
    locator: { bookId: 'demo-001', href: 'chapter-12.xhtml', progression: 0.42 },
  },
  {
    id: 'demo-002-note-007',
    text: 'A reader leaves traces in every world they pass through.',
    book: 'Sample Library Note',
    location: 'Chapter 4 · Note 7',
    kind: 'note',
    locator: { bookId: 'demo-002', href: 'chapter-04.xhtml', progression: 0.18 },
  },
  {
    id: 'demo-003-highlight-001',
    text: 'The page closes. The thought does not.',
    book: 'Sample Library Note',
    location: 'Chapter 21 · Highlight 1',
    kind: 'highlight',
    locator: { bookId: 'demo-003', href: 'chapter-21.xhtml', progression: 0.77 },
  },
  {
    id: 'demo-004-note-002',
    text: 'What you marked once can become the doorway back.',
    book: 'Sample Library Note',
    location: 'Chapter 8 · Note 2',
    kind: 'note',
    locator: { bookId: 'demo-004', href: 'chapter-08.xhtml', progression: 0.31 },
  },
  {
    id: 'demo-fa-001',
    text: 'بعضی جمله‌ها تمام نمی‌شوند؛ فقط در سکوت منتظر می‌مانند تا دوباره پیدایشان کنی.',
    book: 'یادداشت نمونه',
    location: 'فصل ۶ · یادداشت ۴',
    kind: 'note',
    locator: { bookId: 'demo-fa', href: 'chapter-06.xhtml', progression: 0.54 },
  },
];

const mirror = document.querySelector('#mirror');
const quote = document.querySelector('#quote');
const quoteCard = document.querySelector('#quoteCard');
const book = document.querySelector('#book');
const locationLabel = document.querySelector('#location');
const replay = document.querySelector('#replay');
const stateLabel = document.querySelector('#stateLabel');
const hint = document.querySelector('#hint');
const liveStatus = document.querySelector('#liveStatus');
const canvas = document.querySelector('#ashCanvas');
const ctx = canvas.getContext('2d', { alpha: true, desynchronized: true });
const maskCanvas = document.createElement('canvas');
const maskCtx = maskCanvas.getContext('2d', { willReadFrequently: true });

let currentNoteIndex = -1;
let currentNote = null;
let running = false;
let autoTimer = null;
let raf = null;
let particles = [];
let resizeObserver = null;
let sequenceToken = 0;
let requestedByUser = false;

function wait(ms, token) {
  const delay = reducedMotion ? Math.min(ms, 140) : ms;
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(token === sequenceToken), delay);
  });
}

function setState(state, label = state) {
  mirror.dataset.state = state;
  stateLabel.textContent = label;
  mirror.setAttribute('aria-busy', String(state !== 'idle' && state !== 'hold'));
}

function announce(message) {
  if (!liveStatus) return;
  liveStatus.textContent = '';
  requestAnimationFrame(() => {
    liveStatus.textContent = message;
  });
}

function hasRtlScript(text) {
  return /[\u0590-\u08FF\uFB1D-\uFDFD\uFE70-\uFEFC]/.test(text);
}

function nextNote() {
  if (notes.length === 1) {
    currentNoteIndex = 0;
    return notes[0];
  }

  let index;
  do index = Math.floor(Math.random() * notes.length);
  while (index === currentNoteIndex);

  currentNoteIndex = index;
  return notes[index];
}

function paintNote(note) {
  currentNote = note;
  const rtl = hasRtlScript(note.text);
  const lengthBand = note.text.length > 110 ? 'long' : note.text.length > 70 ? 'medium' : 'short';

  quote.textContent = `“${note.text}”`;
  quoteCard.dir = rtl ? 'rtl' : 'ltr';
  quoteCard.lang = rtl ? 'fa' : 'en';
  quoteCard.dataset.length = lengthBand;
  book.textContent = note.book;
  locationLabel.textContent = note.location;

  mirror.dataset.noteId = note.id;
  mirror.dataset.bookId = note.locator.bookId;
  mirror.dataset.href = note.locator.href || '';
  mirror.dataset.progression = note.locator.progression == null ? '' : String(note.locator.progression);
  mirror.setAttribute('aria-label', `${note.text} — ${note.book}, ${note.location}`);
}

function fitCanvas() {
  const rect = canvas.getBoundingClientRect();
  const dpr = Math.min(window.devicePixelRatio || 1, CONFIG.particles.dprCap);
  canvas.width = Math.max(1, Math.round(rect.width * dpr));
  canvas.height = Math.max(1, Math.round(rect.height * dpr));
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function wrapCanvasText(context, text, maxWidth) {
  const words = text.trim().split(/\s+/);
  const lines = [];
  let line = '';

  for (const word of words) {
    const candidate = line ? `${line} ${word}` : word;
    if (context.measureText(candidate).width <= maxWidth || !line) {
      line = candidate;
    } else {
      lines.push(line);
      line = word;
    }
  }
  if (line) lines.push(line);
  return lines.slice(0, 5);
}

function buildGlyphTargets(text) {
  const rect = canvas.getBoundingClientRect();
  const width = Math.max(1, Math.round(rect.width));
  const height = Math.max(1, Math.round(rect.height));
  const rtl = hasRtlScript(text);

  maskCanvas.width = width;
  maskCanvas.height = height;
  maskCtx.clearRect(0, 0, width, height);

  const baseSize = clamp(width * 0.078, 22, 34);
  const size = text.length > 110 ? baseSize * 0.76 : text.length > 70 ? baseSize * 0.88 : baseSize;
  const lineHeight = size * 1.42;

  maskCtx.fillStyle = '#fff';
  maskCtx.font = `400 ${size}px Georgia, "Times New Roman", serif`;
  maskCtx.textAlign = 'center';
  maskCtx.textBaseline = 'middle';
  maskCtx.direction = rtl ? 'rtl' : 'ltr';

  const lines = wrapCanvasText(maskCtx, text, width * 0.72);
  const totalHeight = Math.max(lineHeight, lines.length * lineHeight);
  const firstY = height / 2 - totalHeight / 2 + lineHeight / 2 - height * 0.025;

  lines.forEach((line, index) => {
    maskCtx.fillText(line, width / 2, firstY + index * lineHeight, width * 0.74);
  });

  const pixels = maskCtx.getImageData(0, 0, width, height).data;
  const candidates = [];
  const step = CONFIG.particles.sampleStep;

  for (let y = Math.max(0, Math.floor(height * 0.18)); y < height * 0.78; y += step) {
    for (let x = Math.floor(width * 0.1); x < width * 0.9; x += step) {
      const alpha = pixels[(Math.floor(y) * width + Math.floor(x)) * 4 + 3];
      if (alpha > 80) candidates.push({ x, y });
    }
  }

  if (!candidates.length) return [];

  const count = clamp(Math.round(width * 0.48), CONFIG.particles.min, CONFIG.particles.max);
  const targets = [];
  const stride = Math.max(1, Math.floor(candidates.length / count));
  const offset = Math.floor(Math.random() * stride);

  for (let i = offset; i < candidates.length && targets.length < count; i += stride) {
    targets.push(candidates[i]);
  }

  while (targets.length < Math.min(count, candidates.length)) {
    targets.push(candidates[Math.floor(Math.random() * candidates.length)]);
  }

  return targets;
}

function edgePoint(width, height) {
  const side = Math.floor(Math.random() * 4);
  const pad = 18;
  if (side === 0) return { x: Math.random() * width, y: -pad };
  if (side === 1) return { x: width + pad, y: Math.random() * height };
  if (side === 2) return { x: Math.random() * width, y: height + pad };
  return { x: -pad, y: Math.random() * height };
}

function spawnParticles(mode = 'coalesce') {
  if (reducedMotion || !currentNote) return;

  const { width, height } = canvas.getBoundingClientRect();
  const glyphTargets = buildGlyphTargets(currentNote.text);
  if (!glyphTargets.length) return;

  particles = glyphTargets.map((target, index) => {
    const edge = edgePoint(width, height);
    const outward = edgePoint(width, height);
    const origin = mode === 'dissolve' ? target : edge;
    const destination = mode === 'dissolve' ? outward : target;

    return {
      x: origin.x,
      y: origin.y,
      tx: destination.x,
      ty: destination.y,
      size: 0.55 + Math.random() * 1.55,
      alpha: 0.24 + Math.random() * 0.58,
      life: -Math.min(0.3, (index % 17) * 0.008 + Math.random() * 0.08),
      speed: 0.012 + Math.random() * 0.012,
      warm: Math.random() > 0.36,
      drift: (Math.random() - 0.5) * 10,
    };
  });

  cancelAnimationFrame(raf);
  animateParticles(mode);
}

function animateParticles(mode) {
  const rect = canvas.getBoundingClientRect();
  ctx.clearRect(0, 0, rect.width, rect.height);
  let alive = false;

  for (const p of particles) {
    p.life = Math.min(1, p.life + p.speed);
    if (p.life <= 0) {
      alive = true;
      continue;
    }

    const t = p.life;
    const ease = 1 - Math.pow(1 - t, 3);
    const x = p.x + (p.tx - p.x) * ease + Math.sin(t * Math.PI) * p.drift;
    const y = p.y + (p.ty - p.y) * ease;
    const fade = mode === 'dissolve'
      ? Math.pow(1 - t, 0.72)
      : Math.sin(Math.PI * Math.min(0.96, t)) * 0.75 + (t > 0.78 ? 0.25 : 0);
    const alpha = p.alpha * Math.max(0, fade);

    ctx.beginPath();
    ctx.fillStyle = p.warm
      ? `rgba(214, 166, 95, ${alpha})`
      : `rgba(135, 157, 194, ${alpha * 0.68})`;
    ctx.shadowBlur = p.size > 1.25 ? 5 : 2;
    ctx.shadowColor = p.warm ? 'rgba(188,137,70,.26)' : 'rgba(94,122,171,.18)';
    ctx.arc(x, y, p.size, 0, Math.PI * 2);
    ctx.fill();

    if (p.life < 1) alive = true;
  }

  ctx.shadowBlur = 0;
  if (alive) raf = requestAnimationFrame(() => animateParticles(mode));
  else if (mode === 'dissolve') ctx.clearRect(0, 0, rect.width, rect.height);
}

function clearParticles() {
  cancelAnimationFrame(raf);
  particles = [];
  const rect = canvas.getBoundingClientRect();
  ctx.clearRect(0, 0, rect.width, rect.height);
}

function scheduleNext() {
  clearTimeout(autoTimer);
  if (document.hidden) return;
  const interval = demoMode ? CONFIG.demoRotationMs : CONFIG.productionRotationMs;
  autoTimer = setTimeout(() => runSequence(false), interval);
}

function emitOpenSource() {
  if (!currentNote) return;
  const detail = {
    fragmentId: currentNote.id,
    kind: currentNote.kind,
    locator: { ...currentNote.locator },
  };

  window.dispatchEvent(new CustomEvent('arrodes:open-source', { detail }));
  setState('hold', 'Source requested');
  announce(`Open source requested for ${currentNote.book}, ${currentNote.location}`);
  stateLabel.title = JSON.stringify(detail.locator);
}

async function runSequence(fromUser = false) {
  if (running || document.hidden) return;

  running = true;
  requestedByUser = fromUser;
  clearTimeout(autoTimer);
  clearParticles();
  const token = ++sequenceToken;

  const note = nextNote();
  paintNote(note);

  setState('awaken', 'Awakening');
  if (!await wait(CONFIG.timings.awaken, token)) return;

  setState('coalesce', 'Coalescing');
  spawnParticles('coalesce');
  if (!await wait(CONFIG.timings.coalesce, token)) return;

  setState('manifest', 'Manifesting');
  if (!await wait(CONFIG.timings.manifest, token)) return;

  setState('hold', 'Readable · tap to return');
  if (requestedByUser) announce(`Saved fragment from ${note.book}. Tap again to return to its source.`);
  if (!await wait(CONFIG.timings.hold, token)) return;

  setState('dissolve', 'Dissolving');
  spawnParticles('dissolve');
  if (!await wait(CONFIG.timings.dissolve, token)) return;

  setState('idle', 'Silent');
  currentNote = null;
  mirror.removeAttribute('aria-label');
  if (!await wait(CONFIG.timings.rest, token)) return;

  running = false;
  requestedByUser = false;
  clearParticles();
  scheduleNext();
}

function abortSequence() {
  sequenceToken += 1;
  running = false;
  requestedByUser = false;
  clearTimeout(autoTimer);
  clearParticles();
  setState('idle', 'Silent');
}

function activateMirror(event) {
  if (event.type === 'keydown' && !['Enter', ' '].includes(event.key)) return;
  event.preventDefault();

  if (mirror.dataset.state === 'hold' && currentNote) {
    emitOpenSource();
    return;
  }

  if (!running) runSequence(true);
}

mirror.addEventListener('click', activateMirror);
mirror.addEventListener('keydown', activateMirror);
replay.addEventListener('click', () => {
  if (!running) runSequence(true);
});

window.addEventListener('arrodes:open-source', (event) => {
  console.info('[Arrodes prototype] open-source intent', event.detail);
});

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    abortSequence();
  } else {
    scheduleNext();
  }
});

motionQuery.addEventListener?.('change', (event) => {
  reducedMotion = event.matches;
  abortSequence();
  scheduleNext();
});

resizeObserver = new ResizeObserver(() => {
  fitCanvas();
  if (mirror.dataset.state !== 'coalesce' && mirror.dataset.state !== 'dissolve') clearParticles();
});
resizeObserver.observe(canvas);
fitCanvas();

setState('idle', 'Silent');
hint.textContent = 'Tap to awaken · when a fragment is readable, tap again to return to its source.';
setTimeout(() => runSequence(false), reducedMotion ? 250 : 1600);
