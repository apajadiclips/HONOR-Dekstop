import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";

export const fontFamily = "Inter";
for (const weight of ["400", "600", "800"]) {
  loadFont({ family: fontFamily, url: staticFile(`fonts/Inter-${weight}.otf`), weight });
}

export const glass: React.CSSProperties = {
  background: "linear-gradient(180deg, rgba(26,36,56,0.55), rgba(16,24,40,0.72))",
  border: "1.5px solid rgba(255,255,255,0.32)",
  boxShadow: "inset 0 1.5px 0 rgba(255,255,255,0.55), 0 30px 80px rgba(0,0,0,0.45)",
  backdropFilter: "blur(30px) saturate(170%)",
};

export const muted = "#D7DFEE";
