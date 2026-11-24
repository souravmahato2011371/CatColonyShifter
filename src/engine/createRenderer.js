import * as THREE from "three";
export function createRenderer(mount,config){
 const scene=new THREE.Scene();scene.background=new THREE.Color("#8ac7bd");scene.fog=new THREE.Fog("#8ac7bd",25,66);
 const camera=new THREE.PerspectiveCamera(48,innerWidth/innerHeight,.1,150);camera.position.set(0,8,13);
 const renderer=new THREE.WebGLRenderer({antialias:false,alpha:false,powerPreference:"high-performance",stencil:false,depth:true,preserveDrawingBuffer:false});
 let qualityScale=1;
 function applyResolution(){renderer.setPixelRatio(Math.min(devicePixelRatio||1,config.pixelRatioCap)*qualityScale);renderer.setSize(innerWidth,innerHeight,false);}
 applyResolution();
 // Dynamic shadow maps are expensive on mobile GPUs; lighting and ambient occlusion-free art keep the scene readable without them.
 renderer.shadowMap.enabled=false;
 renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.05;
 renderer.domElement.setAttribute("aria-label","Explore Verdant");renderer.domElement.tabIndex=0;mount.appendChild(renderer.domElement);
 scene.add(new THREE.HemisphereLight("#e5fff0","#47665b",1.9));
 const sun=new THREE.DirectionalLight("#fff2ce",2.4);sun.position.set(-8,16,9);scene.add(sun);
 const fill=new THREE.DirectionalLight("#a5e7dc",.65);fill.position.set(10,7,-8);scene.add(fill);
 function resize(){camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();applyResolution();}
 addEventListener("resize",resize);
 return {scene,camera,renderer,setQualityScale(value){qualityScale=Math.max(.68,Math.min(1,value));applyResolution();},dispose(){removeEventListener("resize",resize);renderer.dispose();renderer.domElement.remove();}};
}
