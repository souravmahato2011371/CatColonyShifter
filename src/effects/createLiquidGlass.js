export function createLiquidGlass(THREE,renderer,scene){
 const target=new THREE.WebGLRenderTarget(1,1,{minFilter:THREE.LinearFilter,magFilter:THREE.LinearFilter,format:THREE.RGBAFormat,depthBuffer:true,stencilBuffer:false});
 target.texture.generateMipmaps=false;
 const uniforms={uScene:{value:target.texture},uResolution:{value:new THREE.Vector2(1,1)},uTime:{value:0},uRefraction:{value:.035},uDispersion:{value:.012},uTint:{value:new THREE.Color("#bfffee")}};
 const vertexShader=[
 "varying vec3 vViewNormal;","varying vec3 vViewPosition;","void main(){",
 "vec4 p=modelViewMatrix*vec4(position,1.0);vViewPosition=p.xyz;vViewNormal=normalize(normalMatrix*normal);gl_Position=projectionMatrix*p;","}"
 ].join("\n");
 const fragmentShader=[
 "uniform sampler2D uScene;","uniform vec2 uResolution;","uniform float uTime;","uniform float uRefraction;","uniform float uDispersion;","uniform vec3 uTint;",
 "varying vec3 vViewNormal;","varying vec3 vViewPosition;","void main(){",
 "vec2 uv=gl_FragCoord.xy/uResolution;vec3 n=normalize(vViewNormal);vec3 v=normalize(-vViewPosition);",
 "float facing=abs(dot(n,v));float fresnel=pow(1.0-clamp(facing,0.0,1.0),3.0);",
 "vec2 bend=n.xy*uRefraction*(0.5+fresnel*1.8);bend+=vec2(sin(uTime*.8+uv.y*18.0),cos(uTime*.65+uv.x*15.0))*.0015;",
 "vec2 ur=clamp(uv+bend*(1.0+uDispersion),.002,.998);vec2 ug=clamp(uv+bend,.002,.998);vec2 ub=clamp(uv+bend*(1.0-uDispersion),.002,.998);",
 "vec3 refracted=vec3(texture2D(uScene,ur).r,texture2D(uScene,ug).g,texture2D(uScene,ub).b);",
 "float rim=pow(1.0-facing,5.0);vec3 hi=mix(vec3(.66,.94,.86),vec3(1.0),rim);",
 "vec3 color=mix(refracted,uTint,.10+fresnel*.18)+hi*(.08+rim*.40);",
 "gl_FragColor=vec4(color,.68+fresnel*.27);",
 "#include <tonemapping_fragment>","#include <colorspace_fragment>","}"
 ].join("\n");
 const material=new THREE.ShaderMaterial({uniforms,transparent:true,depthWrite:false,side:THREE.DoubleSide,vertexShader,fragmentShader});
 const crystal=new THREE.Mesh(new THREE.SphereGeometry(.72,40,32),material);
 crystal.position.set(0,2.05,-8.78);crystal.scale.set(.88,1.08,.78);crystal.renderOrder=4;scene.add(crystal);
 const light=new THREE.PointLight("#9fffea",.85,4.8);light.position.copy(crystal.position);scene.add(light);
 let enabled=false;crystal.visible=false;const size=new THREE.Vector2();
 function resize(){renderer.getDrawingBufferSize(size);target.setSize(Math.max(1,size.x),Math.max(1,size.y));uniforms.uResolution.value.copy(size);}
 resize();addEventListener("resize",resize);
 function render(camera,time){
  uniforms.uTime.value=time;
  if(!enabled){crystal.visible=false;renderer.render(scene,camera);return;}
  crystal.visible=false;const previous=renderer.getRenderTarget();renderer.setRenderTarget(target);renderer.clear();renderer.render(scene,camera);renderer.setRenderTarget(previous);crystal.visible=true;renderer.render(scene,camera);
 }
 return{render,update(t){crystal.rotation.y=t*.36;crystal.rotation.x=Math.sin(t*.75)*.12;crystal.position.y=2.05+Math.sin(t*1.35)*.13;light.position.copy(crystal.position);},toggle(){enabled=!enabled;crystal.visible=enabled;return enabled;},dispose(){removeEventListener("resize",resize);scene.remove(crystal,light);crystal.geometry.dispose();material.dispose();target.dispose();}};
}
