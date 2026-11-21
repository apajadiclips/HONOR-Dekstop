import { Composition, Folder } from "remotion";
import { Feature } from "./Feature";
import { Intro } from "./Intro";
import { Outro } from "./Outro";
import { features, Teaser, TEASER_FRAMES } from "./Teaser";
import { Zones } from "./Zones";

const size = { width: 1920, height: 1080, fps: 30 };

export const RemotionRoot: React.FC = () => (
  <>
    <Folder name="Teaser-Scenes">
      <Composition id="Intro" component={Intro} durationInFrames={100} {...size} />
      <Composition id="Feature" component={Feature} durationInFrames={105} defaultProps={features[0]} {...size} />
      <Composition id="Ruangan" component={Zones} durationInFrames={140} {...size} />
      <Composition id="Penutup" component={Outro} durationInFrames={120} {...size} />
    </Folder>
    <Composition id="Teaser" component={Teaser} durationInFrames={TEASER_FRAMES} {...size} />
  </>
);
