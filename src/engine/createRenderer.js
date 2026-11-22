import * as THREE from "three";
export function createRenderer(mount,config){
 const scene=new THREE.Scene();scene.background=new THREE.Color("#8ac7bd");scene.fog=new THREE.Fog("#8ac7bd",25,66);
 const camera=new THREE.PerspectiveCamera(48,innerWidth/innerHeight,.1,150);camera.position.set(0,8,13);
 const renderer=new THREE.WebGLRenderer({antialias:true,alpha:false,powerPreference:"high-performance"});
 renderer.setPixelRatio(Math.min(devicePixelRatio||1,config.pixelRatioCap));renderer.setSize(innerWidth,innerHeight);
 renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;
 renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.05;
 renderer.domElement.setAttribute("aria-label","Explore Verdant");renderer.domElement.tabIndex=0;mount.appendChild(renderer.domElement);
 scene.add(new THREE.HemisphereLight("#e5fff0","#47665b",1.9));
 const sun=new THREE.DirectionalLight("#fff2ce",2.4);sun.position.set(-8,16,9);sun.castShadow=true;sun.shadow.mapSize.set(1024,1024);
 sun.shadow.camera.left=-18;sun.shadow.camera.right=18;sun.shadow.camera.top=18;sun.shadow.camera.bottom=-18;scene.add(sun);
 const fill=new THREE.DirectionalLight("#a5e7dc",.65);fill.position.set(10,7,-8);scene.add(fill);
 function resize(){camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setPixelRatio(Math.min(devicePixelRatio||1,config.pixelRatioCap));renderer.setSize(innerWidth,innerHeight);}
 addEventListener("resize",resize);
 return {scene,camera,renderer,dispose(){removeEventListener("resize",resize);renderer.dispose();renderer.domElement.remove();}};
}
