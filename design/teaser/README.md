# Video teaser (Remotion)

```sh
cd design/teaser
npm i
mkdir -p public/fonts
cp ../previews/[1-8]-*.png public/
# letak fon Inter (lesen OFL) sebagai public/fonts/Inter-400.otf, Inter-600.otf, Inter-800.otf
npx remotion render Teaser out/teaser.mp4
```
Hasil: `design/HonorHybridDesktop-teaser.mp4` (1920x1080, 30fps, 23 saat, tanpa bunyi).
