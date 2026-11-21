import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Aurora } from "./Aurora";
import { fontFamily, glass, muted } from "./theme";

const chips = ["v0.3", "Percuma", "Tanpa root"];

export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ fontFamily, color: "white" }}>
      <Aurora dim={0.3} />
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", gap: 44 }}>
        <Interactive.Div
          name="Title"
          style={{
            fontSize: 130, fontWeight: 800, letterSpacing: -4, textAlign: "center", lineHeight: 1.05,
            opacity: interpolate(frame, [0, 0.7 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            scale: interpolate(frame, [0, 1.2 * fps], [1.08, 1], {
              extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1), output: "perceptual-scale",
            }),
          }}
        >
          HONOR Hybrid Desktop
        </Interactive.Div>
        <div style={{ display: "flex", gap: 24 }}>
          {chips.map((c, i) => (
            <Interactive.Div
              key={c}
              name={`Chip ${c}`}
              style={{
                ...glass, borderRadius: 40, padding: "16px 40px", fontSize: 48, fontWeight: 600,
                opacity: interpolate(frame, [(0.6 + i * 0.2) * fps, (1 + i * 0.2) * fps], [0, 1], {
                  extrapolateLeft: "clamp", extrapolateRight: "clamp",
                }),
                translate: interpolate(frame, [(0.6 + i * 0.2) * fps, (1.2 + i * 0.2) * fps], ["0px 40px", "0px 0px"], {
                  extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
                }),
              }}
            >
              {c}
            </Interactive.Div>
          ))}
        </div>
        <Interactive.Div
          name="Link"
          style={{
            fontSize: 44, color: muted,
            opacity: interpolate(frame, [1.5 * fps, 2.1 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          github.com/apajadiclips/HONOR-Dekstop
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
