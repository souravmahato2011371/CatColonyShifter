// Cat Colony: Shifter — future WebGPU liquid-glass material
// This starter shader defines the fragment-stage interface only.
// It is not wired into the current Three.js WebGL renderer yet.
struct GlassUniforms {
  resolution: vec2f,
  time: f32,
  refractionStrength: f32,
  edgeWidth: f32,
  dispersion: f32,
};
@group(0) @binding(0) var<uniform> glass: GlassUniforms;
@group(0) @binding(1) var sceneColor: texture_2d<f32>;
@group(0) @binding(2) var sceneSampler: sampler;

@fragment
fn liquidGlassFragment(@builtin(position) position: vec4f) -> @location(0) vec4f {
  let uv = position.xy / glass.resolution;
  let color = textureSample(sceneColor, sceneSampler, uv);
  return color;
}
