const CONFIG = {
  productionRotationMs: 30 * 60 * 1000,
  demoRotationMs: 18 * 1000,
  timings: {
    activate: 900,
    gather: 1700,
    reveal: 1000,
    hold: 6000,
    dissolve: 1700,
    settle: 900,
  },
};

const demoMode = new URLSearchParams(location.search).has('demo');
const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

const notes = [
  {
    text: 'Some memories do not fade. They wait for the right silence to return.',
    book: 'Sample Library Note',
    location: 'Chapter 12 · Highlight 3',
    locator: { bookId: 'demo-001', href: 'chapter-12.xhtml', progression: 0.42 },
  },
  {
    text: 'A reader leaves traces in every world they pass through.',
    book: 'Sample Library Note',
    location: 'Chapter 4 · Note 7',
    locator: { bookId: 'demo-002', href: 'chapter-04.xhtml', progression: 0.18 },
  },
  {
    text: 'The page closes. The thought does not.',
    book: 'Sample Library Note',
    location: 'Chapter 21 · Highlight 1',
    locator: { bookId: 'demo-003', href: 'chapter-21.xhtml', progression: 0.77 },
  },
  {
    text: 'What you marked once can become the doorway back.',
    book: 'Sample Library Note',
    location: 'Chapter 8 · Note 2',
    locator: { bookId: 'demo-004', href: 'chapter-08.xhtml', progression: 0.31 },
  },
];

const mirror = document.querySelector('#mirror');
const quote = document.querySelector('#quote');
const book = document.querySelector('#book');
const locationLabel = document.querySelector('#location');
const replay = document.querySelector('#replay');
const stateLabel = document.querySelector('#stateLabel');
const canvas = document.querySelector('#ashCanvas');
const ctx = canvas.getContext('2d', { alpha: true });

let currentNote = -1;
let running = false;
let autoTimer = null;
let raf = null;
let particles = [];
let resizeObserver = null;

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, reducedMotion ? Math.min(ms, 120) : ms));

function setState(state, label = state) {
  mirror.dataset.state = state;
  stateLabel.textContent = label;
}

function nextNote() {
  if (notes.length < 2) return notes[0];
  let index;
  do index = Math.floor(Math.random() * notes.length);
  while (index === currentNote);
  currentNote = index;
  return notes[index];
}

function paintNote(note) {
  quote.textContent = `“${note.text}”`;
  book.textContent = note.book;
  locationLabel.textContent = note.location;
  mirror.dataset.bookId = note.locator.bookId;
  mirror.dataset.href = note.locator.href;
  mirror.dataset.progression = String(note.locator.progression);
}

function fitCanvas() {
  const rect = canvas.getBoundingClientRect();
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = Math.max(1, Math.round(rect.width * dpr));
  canvas.height = Math.max(1, Math.round(rect.height * dpr));
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function spawnParticles(mode = 'gather') {
  if (reducedMotion) return;
  const { width, height } = canvas.getBoundingClientRect();
  const count = Math.round(Math.min(170, Math.max(90, width * 0.36)));
  const centerX = width / 2;
  const centerY = height / 2;

  particles = Array.from({ length: count }, () => {
    const angle = Math.random() * Math.PI * 2;
    const radiusX = width * (0.34 + Math.random() * 0.26);
    const radiusY = height * (0.28 + Math.random() * 0.28);
    const edgeX = centerX + Math.cos(angle) * radiusX;
    const edgeY = centerY + Math.sin(angle) * radiusY;
    const targetX = centerX + (Math.random() - 0.5) * width * 0.48;
    const targetY = centerY + (Math.random() - 0.5) * height * 0.42;

    if (mode === 'dissolve') {
      return {
        x: targetX,
        y: targetY,
        tx: edgeX,
        ty: edgeY,
        size: 0.6 + Math.random() * 1.8,
        alpha: 0.14 + Math.random() * 0.72,
        life: 0,
        speed: 0.010 + Math.random() * 0.015,
        warm: Math.random() > 0.3,
      };
    }

    return {
      x: edgeX,
      y: edgeY,
      tx: targetX,
      ty: targetY,
      size: 0.6 + Math.random() * 1.8,
      alpha: 0.14 + Math.random() * 0.72,
      life: 0,
      speed: 0.010 + Math.random() * 0.015,
      warm: Math.random() > 0.3,
    };
  });

  cancelAnimationFrame(raf);
  animateParticles();
}

function animateParticles() {
  const rect = canvas.getBoundingClientRect();
  ctx.clearRect(0, 0, rect.width, rect.height);
  let alive = false;

  for (const p of particles) {
    p.life = Math.min(1, p.life + p.speed);
    const ease = 1 - Math.pow(1 - p.life, 3);
    const x = p.x + (p.tx - p.x) * ease;
    const y = p.y + (p.ty - p.y) * ease;
    const fade = Math.sin(Math.PI * p.life);
    const alpha = p.alpha * Math.max(0.15, fade);

    ctx.beginPath();
    ctx.fillStyle = p.warm
      ? `rgba(214, 166, 95, ${alpha})`
      : `rgba(135, 157, 194, ${alpha * 0.72})`;
    ctx.shadowBlur = 7;
    ctx.shadowColor = p.warm ? 'rgba(188,137,70,.34)' : 'rgba(94,122,171,.24)';
    ctx.arc(x, y, p.size, 0, Math.PI * 2);
    ctx.fill();

    if (p.life < 1) alive = true;
  }

  ctx.shadowBlur = 0;
  if (alive) raf = requestAnimationFrame(animateParticles);
  else ctx.clearRect(0, 0, rect.width, rect.height);
}

async function runSequence() {
  if (running) return;
  running = true;
  clearTimeout(autoTimer);

  const note = nextNote();
  paintNote(note);

  setState('activate', 'Activation');
  await wait(CONFIG.timings.activate);

  setState('gather', 'Gathering ash');
  spawnParticles('gather');
  await wait(CONFIG.timings.gather);

  setState('reveal', 'Manifesting');
  await wait(CONFIG.timings.reveal);

  setState('hold', 'Readable');
  await wait(CONFIG.timings.hold);

  setState('dissolve', 'Dissolving');
  spawnParticles('dissolve');
  await wait(CONFIG.timings.dissolve);

  setState('idle', 'Idle');
  await wait(CONFIG.timings.settle);

  running = false;
  scheduleNext();
}

function scheduleNext() {
  clearTimeout(autoTimer);
  const interval = demoMode ? CONFIG.demoRotationMs : CONFIG.productionRotationMs;
  autoTimer = setTimeout(runSequence, interval);
}

function activateFromInput(event) {
  if (event.type === 'keydown' && !['Enter', ' '].includes(event.key)) return;
  event.preventDefault();
  runSequence();
}

mirror.addEventListener('click', activateFromInput);
mirror.addEventListener('keydown', activateFromInput);
replay.addEventListener('click', runSequence);

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    clearTimeout(autoTimer);
    cancelAnimationFrame(raf);
  } else if (!running) {
    scheduleNext();
  }
});

resizeObserver = new ResizeObserver(fitCanvas);
resizeObserver.observe(canvas);
fitCanvas();

setState('idle', 'Idle');
setTimeout(runSequence, reducedMotion ? 250 : 1800);
