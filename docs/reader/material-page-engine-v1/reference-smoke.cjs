// Run against the pinned upstream source compiled into CommonJS; no library code vendored.
// npm exec --yes --package=typescript@4.9.5 -- tsc --project UPSTREAM/tsconfig.json \
//   --module commonjs --outDir COMPILED
// node reference-smoke.cjs COMPILED/Flip/FlipCalculation.js
const { FlipCalculation } = require(require('node:path').resolve(process.argv[2]));
let accepted = 0;
for (const [width, height] of [[412, 900], [900, 412], [360, 1800]]) {
    for (const direction of [0, 1]) for (const corner of ['top', 'bottom']) {
        for (let i = 1; i < 100; i++) {
            const geometry = new FlipCalculation(direction, corner, String(width), String(height));
            if (!geometry.calc({ x: width * (1 - 1.95 * i / 100),
                y: height * (corner === 'top' ? .08 : .92) })) throw Error('Rejected sample');
            for (const point of Object.values(geometry.getRect())) {
                if (!Number.isFinite(point.x) || !Number.isFinite(point.y)) throw Error('Non-finite geometry');
            }
            if (!Number.isFinite(geometry.getAngle()) || !Number.isFinite(geometry.getFlippingProgress())) {
                throw Error('Non-finite state');
            }
            accepted++;
        }
    }
}
console.log(JSON.stringify({ reference: 'Nodlik/StPageFlip@ab30ecc1', accepted, rejected: 0, nonFinite: 0 }));
