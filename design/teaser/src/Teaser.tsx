import { linearTiming, springTiming, TransitionSeries } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { slide } from "@remotion/transitions/slide";
import { Feature, FeatureProps } from "./Feature";
import { Intro } from "./Intro";
import { Outro } from "./Outro";
import { Zones } from "./Zones";

export const features: FeatureProps[] = [
  { image: "1-Main.png", kicker: "Desktop & Menu Mula", title: "Taskbar, Menu Mula, widget",
    chips: ["Semua aplikasi sebenar", "Seret ikon & buat folder", "Widget Debloat Manager"], focus: "45% 70%" },
  { image: "2-Notifikasi.png", kicker: "Liquid Glass", title: "Notifikasi gaya kaca",
    chips: ["Balas terus", "Kecerahan & bunyi", "Kawalan muzik"], focus: "75% 40%" },
  { image: "3-Multitask.png", kicker: "Multitasking", title: "Desktop maya & papan klip",
    chips: ["Desktop Kerja & Hiburan", "Penukar tugas", "Sejarah papan klip"], focus: "50% 45%" },
  { image: "4-Wallpaper.png", kicker: "Peribadi", title: "Wallpaper kustom",
    chips: ["Dari Galeri atau kamera", "Lain setiap desktop", "Juga untuk skrin kunci"], focus: "35% 45%" },
];

export const FEATURE_FRAMES = 105;
export const T = 15;
export const TEASER_FRAMES = 100 + FEATURE_FRAMES * features.length + 140 + 120 - T * (features.length + 2);

export const Teaser: React.FC = () => (
  <TransitionSeries>
    <TransitionSeries.Sequence name="Intro" durationInFrames={100}>
      <Intro />
    </TransitionSeries.Sequence>
    {features.flatMap((f, i) => [
      <TransitionSeries.Transition
        key={`t${i}`}
        presentation={i === 0 ? fade() : slide({ direction: "from-right" })}
        timing={i === 0 ? linearTiming({ durationInFrames: T }) : springTiming({ config: { damping: 200 }, durationInFrames: T })}
      />,
      <TransitionSeries.Sequence key={f.image} name={f.kicker} durationInFrames={105}>
        <Feature {...f} />
      </TransitionSeries.Sequence>,
    ])}
    <TransitionSeries.Transition presentation={slide({ direction: "from-bottom" })}
      timing={springTiming({ config: { damping: 200 }, durationInFrames: T })} />
    <TransitionSeries.Sequence name="Ruangan" durationInFrames={140}>
      <Zones />
    </TransitionSeries.Sequence>
    <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: T })} />
    <TransitionSeries.Sequence name="Penutup" durationInFrames={120}>
      <Outro />
    </TransitionSeries.Sequence>
  </TransitionSeries>
);
