import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";

const blobs = [
  { c: "#2F5BFF", x: 0.18, y: 0.25, r: 0.48, dx: 0.06, dy: 0.04 },
  { c: "#8A4DFF", x: 0.62, y: 0.82, r: 0.5, dx: -0.05, dy: -0.05 },
  { c: "#12A99B", x: 0.88, y: 0.2, r: 0.4, dx: -0.04, dy: 0.06 },
  { c: "#FF7A45", x: 0.9, y: 0.85, r: 0.32, dx: -0.06, dy: -0.03 },
];

/** Slowly drifting colour blobs - the launcher's "Aurora" wallpaper. */
export const Aurora: React.FC<{ dim?: number }> = ({ dim = 0.15 }) => {
  const frame = useCurrentFrame();
  const t = interpolate(frame, [0, 900], [0, 1]);
  return (
    <AbsoluteFill style={{ backgroundColor: "#0B1426" }}>
      {blobs.map((b, i) => (
        <AbsoluteFill
          key={i}
          style={{
            background: `radial-gradient(circle at ${(b.x + b.dx * Math.sin(t * 6.28 + i)) * 100}% ${
              (b.y + b.dy * Math.cos(t * 6.28 + i)) * 100
            }%, ${b.c}F2 0%, ${b.c}00 ${b.r * 100}%)`,
          }}
        />
      ))}
      <AbsoluteFill style={{ backgroundColor: `rgba(0,0,0,${dim})` }} />
    </AbsoluteFill>
  );
};
