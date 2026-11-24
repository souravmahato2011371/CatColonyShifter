// WebGPU port reference. The playable WebGL screen-capture effect lives in
// src/effects/createLiquidGlass.js until the main renderer is migrated to WebGPU.
struct GlassUniforms {
 resolution: vec2f,
 time: f32,
 refraction_strength: f32,
 dispersion: f32,
 edge_width: f32,
 corner_radius: f32,
 padding: vec2f,
};
@group(0) @binding(0) var<uniform> glass: GlassUniforms;
@group(0) @binding(1) var scene_color: texture_2d<f32>;
@group(0) @binding(2) var scene_sampler: sampler;
fn rounded_box_sdf(p:vec2f,half_size:vec2f,radius:f32)->f32{
 let q=abs(p)-half_size+vec2f(radius);
 return length(max(q,vec2f(0.0)))+min(max(q.x,q.y),0.0)-radius;
}
fn sample_scene(uv:vec2f)->vec3f{return textureSample(scene_color,scene_sampler,clamp(uv,vec2f(.001),vec2f(.999))).rgb;}
@fragment
fn liquid_glass_fragment(@builtin(position) position:vec4f)->@location(0) vec4f{
 let uv=position.xy/glass.resolution;
 let aspect=glass.resolution.x/glass.resolution.y;
 let p=(uv-vec2f(.5))*vec2f(aspect,1.0);
 let half_size=vec2f(.28*aspect,.28);
 let d=rounded_box_sdf(p,half_size,glass.corner_radius);
 let inside=1.0-smoothstep(0.0,glass.edge_width,d);
 let edge=1.0-smoothstep(0.0,glass.edge_width*2.0,abs(d));
 let e=1.5/min(glass.resolution.x,glass.resolution.y);
 let nx=rounded_box_sdf(p+vec2f(e,0.0),half_size,glass.corner_radius)-rounded_box_sdf(p-vec2f(e,0.0),half_size,glass.corner_radius);
 let ny=rounded_box_sdf(p+vec2f(0.0,e),half_size,glass.corner_radius)-rounded_box_sdf(p-vec2f(0.0,e),half_size,glass.corner_radius);
 let n=normalize(vec2f(nx,ny)+vec2f(.00001));
 let bend=n*glass.refraction_strength*inside;
 let red=sample_scene(uv+bend*(1.0+glass.dispersion));
 let green=sample_scene(uv+bend);
 let blue=sample_scene(uv+bend*(1.0-glass.dispersion));
 let refracted=vec3f(red.r,green.g,blue.b);
 let fresnel=pow(clamp(1.0-abs(dot(n,normalize(vec2f(.35,.55)))),0.0,1.0),4.0);
 let rim=vec3f(.72,1.0,.91)*edge*(.14+fresnel*.32);
 return vec4f(mix(sample_scene(uv),refracted+rim,inside),inside*.94);
}
