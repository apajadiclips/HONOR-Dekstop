import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Aurora } from "./Aurora";
import { fontFamily, glass, muted } from "./theme";

export type FeatureProps = {
  image: string;
  kicker: string;
  title: string;
  chips: string[];
  /** Point of the screenshot to zoom towards, in % (e.g. "70% 30%"). */
  focus?: string;
};

export const Feature: React.FC<FeatureProps> = ({ image, kicker, title, chips, focus = "50% 50%" }) => {
  const frame = useCurrentFrame();
  const { fps, durationInFrames } = useVideoConfig();
  return (
    <AbsoluteFill style={{ fontFamily, color: "white" }}>
      <Aurora />
      <Interactive.Div
        name="Kicker"
        style={{
          position: "absolute", left: 110, top: 90, ...glass, borderRadius: 30, padding: "10px 26px",
          fontSize: 30, fontWeight: 600, color: muted,
          opacity: interpolate(frame, [0, 0.5 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        {kicker}
      </Interactive.Div>
      <Interactive.Div
        name="Title"
        style={{
          position: "absolute", left: 110, top: 160, right: 110, fontSize: 84, fontWeight: 800, letterSpacing: -2,
          opacity: interpolate(frame, [0.15 * fps, 0.75 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [0.15 * fps, 0.9 * fps], ["0px 30px", "0px 0px"], {
            extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        {title}
      </Interactive.Div>
      <Interactive.Div
        name="Screenshot"
        style={{
          position: "absolute", left: 110, top: 300, width: 1150, height: 690, borderRadius: 28, overflow: "hidden",
          ...glass, padding: 0,
          opacity: interpolate(frame, [0.2 * fps, 0.9 * fps], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [0.2 * fps, 1.1 * fps], ["0px 80px", "0px 0px"], {
            extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        <Img
          src={staticFile(image)}
          style={{
            width: "100%", height: "100%", objectFit: "cover", transformOrigin: focus,
            scale: interpolate(frame, [0, durationInFrames], [1, 1.12], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          }}
        />
      </Interactive.Div>
      <div style={{ position: "absolute", left: 1310, top: 330, right: 110, display: "flex", flexDirection: "column", gap: 22 }}>
        {chips.map((c, i) => (
          <Interactive.Div
            key={c}
            name={`Chip ${i + 1}`}
            style={{
              ...glass, borderRadius: 26, padding: "22px 26px", fontSize: 40, fontWeight: 600, lineHeight: 1.2,
              opacity: interpolate(frame, [(0.7 + i * 0.25) * fps, (1.1 + i * 0.25) * fps], [0, 1], {
                extrapolateLeft: "clamp", extrapolateRight: "clamp",
              }),
              translate: interpolate(frame, [(0.7 + i * 0.25) * fps, (1.3 + i * 0.25) * fps], ["60px 0px", "0px 0px"], {
                extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1),
              }),
            }}
          >
            {c}
          </Interactive.Div>
        ))}
      </div>
    </AbsoluteFill>
  );
};
