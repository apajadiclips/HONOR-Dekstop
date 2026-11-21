import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Aurora } from "./Aurora";
import { fontFamily, glass, muted } from "./theme";

const cards = [
  { img: "6-Game.png", label: "Game", rot: -6, x: -540 },
  { img: "7-Sosial.png", label: "Media Sosial", rot: 0, x: 0 },
  { img: "8-Hiburan.png", label: "Hiburan", rot: 6, x: 540 },
];

export const Zones: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ fontFamily, color: "white" }}>
      <Aurora dim={0.2} />
      <Interactive.Div
        name="Title"
        style={{
          position: "absolute", top: 100, left: 110, right: 110, textAlign: "center", fontSize: 84, fontWeight: 800, letterSpacing: -2,
          opacity: interpolate(frame, [0, 0.6 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        Ruangan khas untuk setiap gaya
      </Interactive.Div>
      <Interactive.Div
        name="Subtitle"
        style={{
          position: "absolute", top: 210, left: 110, right: 110, textAlign: "center", fontSize: 44, color: muted,
          opacity: interpolate(frame, [0.3 * fps, 0.9 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        Game · Sosial · Hiburan · Kerja · Kreatif · Alat
      </Interactive.Div>
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", top: 60 }}>
        {cards.map((c, i) => (
          <Interactive.Div
            key={c.img}
            name={c.label}
            style={{
              position: "absolute", width: 560, height: 336, borderRadius: 24, overflow: "hidden", ...glass, padding: 0,
              zIndex: i === 1 ? 2 : 1,
              opacity: interpolate(frame, [(0.5 + i * 0.3) * fps, (1 + i * 0.3) * fps], [0, 1], {
                extrapolateLeft: "clamp", extrapolateRight: "clamp",
              }),
              translate: interpolate(frame, [(0.5 + i * 0.3) * fps, (1.4 + i * 0.3) * fps], [`${c.x}px 260px`, `${c.x}px 0px`], {
                extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
              }),
              rotate: interpolate(frame, [(0.5 + i * 0.3) * fps, (1.4 + i * 0.3) * fps], ["0deg", `${c.rot}deg`], {
                extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
              }),
            }}
          >
            <Img src={staticFile(c.img)} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
          </Interactive.Div>
        ))}
      </AbsoluteFill>
      {cards.map((c, i) => (
        <Interactive.Div
          key={c.label}
          name={`Label ${c.label}`}
          style={{
            position: "absolute", bottom: 110, left: 960 + c.x, translate: "-50% 0px",
            ...glass, borderRadius: 30, padding: "12px 30px", fontSize: 44, fontWeight: 600, whiteSpace: "nowrap",
            opacity: interpolate(frame, [(1.4 + i * 0.3) * fps, (1.8 + i * 0.3) * fps], [0, 1], {
              extrapolateLeft: "clamp", extrapolateRight: "clamp",
            }),
          }}
        >
          {c.label}
        </Interactive.Div>
      ))}
    </AbsoluteFill>
  );
};
