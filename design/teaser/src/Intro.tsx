import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Aurora } from "./Aurora";
import { fontFamily, glass, muted } from "./theme";

export const Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ fontFamily, color: "white" }}>
      <Aurora dim={0.25} />
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", gap: 36 }}>
        <Interactive.Div
          name="Kicker"
          style={{
            ...glass, borderRadius: 40, padding: "14px 34px", fontSize: 34, fontWeight: 600, color: muted,
            opacity: interpolate(frame, [0, 0.6 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [0, 0.8 * fps], ["0px 40px", "0px 0px"], {
              extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          HONOR Pad X9
        </Interactive.Div>
        <Interactive.Div
          name="Title"
          style={{
            fontSize: 150, fontWeight: 800, letterSpacing: -4, textAlign: "center", lineHeight: 1,
            textShadow: "0 10px 60px rgba(0,0,0,0.45)",
            opacity: interpolate(frame, [0.3 * fps, 1.1 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            scale: interpolate(frame, [0.3 * fps, 1.4 * fps], [0.9, 1], {
              extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1), output: "perceptual-scale",
            }),
          }}
        >
          HONOR Hybrid Desktop
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            fontSize: 52, fontWeight: 400, color: muted, textAlign: "center",
            opacity: interpolate(frame, [1.1 * fps, 1.9 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        >
          Tablet anda, kini rasa seperti desktop.
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
