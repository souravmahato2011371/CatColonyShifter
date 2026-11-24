import * as THREE from "three";
import {GAME_CONFIG} from "./config.js";
import {createRenderer} from "./engine/createRenderer.js";
import {createVerdantWorld} from "./worlds/verdant.js?v=verdant-oak-01";
import {createCat} from "./entities/createCat.js?v=20261124-catglb";
import {createControls} from "./game/createControls.js?v=20261123-4";
import {loadProgress,saveProgress} from "./game/progress.js?v=20261123-4";
import {createInterface} from "./ui/createInterface.js?v=20261123-4";
const mount=document.getElementById("game");let started=false;
const ui=createInterface({start(){started=true;const root=document.documentElement;if(!document.fullscreenElement&&root.requestFullscreen)root.requestFullscreen().catch(()=>{});ui.hideIntro();ui.toast("Welcome home, little Shifter.");},build:buildColony});
const {scene,camera,renderer,setQualityScale}=createRenderer(mount,GAME_CONFIG);
const world=createVerdantWorld(THREE,scene),player=createCat(THREE,scene),controls=createControls(renderer.domElement);
const prior=loadProgress(GAME_CONFIG.saveKey,world.seeds.length);
const state={currency:prior.currency,totalFound:prior.totalFound,foundIds:prior.foundIds.slice(),built:prior.built.slice(),goal:GAME_CONFIG.starseedGoal,worldRestored:false};
let velocityY=0,grounded=true;const clock=new THREE.Clock(),lookAt=new THREE.Vector3();
function persist(){saveProgress(GAME_CONFIG.saveKey,state);}
function refresh(){ui.update(state);persist();}
function makeStructure(id){
 const group=new THREE.Group(),stone=new THREE.MeshStandardMaterial({color:id==="garden"?"#9cae77":"#b8c8bd",roughness:.7});
 if(id==="garden"){
  const base=new THREE.Mesh(new THREE.CylinderGeometry(.95,1.05,.22,8),stone);base.position.y=.12;group.add(base);
  for(let i=0;i<5;i++){const sprout=new THREE.Mesh(new THREE.ConeGeometry(.17,.75,5),new THREE.MeshStandardMaterial({color:i%2?"#72ad77":"#d9f36b",roughness:.8}));const a=i*Math.PI*2/5;sprout.position.set(Math.cos(a)*.58,.55,Math.sin(a)*.58);sprout.rotation.z=Math.cos(a)*.18;group.add(sprout);}
 }else{
  const pole=new THREE.Mesh(new THREE.CylinderGeometry(.12,.2,2.35,7),stone);pole.position.y=1.15;group.add(pole);
  const orb=new THREE.Mesh(new THREE.SphereGeometry(.36,16,12),new THREE.MeshStandardMaterial({color:"#d9f36b",emissive:"#a0b84e",emissiveIntensity:1.4,roughness:.2}));orb.position.y=2.48;group.add(orb);
  const lamp=new THREE.PointLight("#d9f36b",1.2,7);lamp.position.y=2.5;group.add(lamp);
 }
 group.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;}});group.position.set(id==="garden"?-3.2:3.2,0,1.2);scene.add(group);return group;
}
function buildColony(id){
 if(state.built.includes(id)){ui.toast("Already part of your colony!");return;}
 const cost=id==="garden"?3:5;if(state.currency<cost){ui.toast("You need "+cost+" spendable starseeds first.");return;}
 state.currency-=cost;state.built.push(id);makeStructure(id);refresh();ui.toast(id==="garden"?"A garden takes root!":"The beacon is calling across the stars.");
}
state.built.forEach(makeStructure);
state.foundIds.forEach(id=>{if(world.seeds[id])world.seeds[id].visible=false;});
refresh();
function collectSeeds(){
 for(const seed of world.seeds){if(!seed.visible)continue;if(player.group.position.distanceTo(seed.position)<1.22){
  seed.visible=false;state.foundIds.push(seed.userData.id);state.totalFound++;state.currency++;refresh();ui.toast("Starseed found · "+state.totalFound+"/"+state.goal);break;
 }}
}
function movePlayer(dt,t){
 if(!started){player.animate(t,false,true);return;}
 if(controls.consumeJump()&&grounded){velocityY=GAME_CONFIG.jumpVelocity;grounded=false;}
 const input=controls.getMove(),yaw=controls.yaw,fx=Math.sin(yaw),fz=-Math.cos(yaw),rx=Math.cos(yaw),rz=Math.sin(yaw);
 const mx=rx*input.x+fx*-input.z,mz=rz*input.x+fz*-input.z;
 if(input.moving){player.group.position.x+=mx*GAME_CONFIG.playerSpeed*dt;player.group.position.z+=mz*GAME_CONFIG.playerSpeed*dt;player.group.rotation.y=Math.atan2(mx,-mz);}
 velocityY-=GAME_CONFIG.gravity*dt;player.group.position.y+=velocityY*dt;
 if(player.group.position.y<=0){player.group.position.y=0;velocityY=0;grounded=true;}
 const limit=GAME_CONFIG.islandRadius-1.3,distance=Math.hypot(player.group.position.x,player.group.position.z);
 if(distance>limit){const k=limit/distance;player.group.position.x*=k;player.group.position.z*=k;}
 player.animate(t,input.moving,grounded);
}
function updateCamera(dt){
 const yaw=controls.yaw;lookAt.set(player.group.position.x,player.group.position.y+1.05,player.group.position.z);
 const desired=new THREE.Vector3(player.group.position.x+Math.sin(yaw)*GAME_CONFIG.cameraDistance,player.group.position.y+GAME_CONFIG.cameraHeight,player.group.position.z+Math.cos(yaw)*GAME_CONFIG.cameraDistance);
 camera.position.lerp(desired,1-Math.pow(.001,dt));camera.lookAt(lookAt);
}
function checkGate(){
 if(state.totalFound<GAME_CONFIG.starseedGoal||state.worldRestored)return;
 if(player.group.position.distanceTo(world.gate.position)<2.35){state.worldRestored=true;refresh();ui.toast("WORLD RESTORED · VERDANT");ui.closePanels();}
}
let quality=1,qualitySampleTime=0,qualityFrames=0,lastFps=60;
function frame(){
 requestAnimationFrame(frame);
 if(document.hidden)return;
 const dt=Math.min(clock.getDelta(),.04),t=clock.elapsedTime;
 qualityFrames++;
 if(t-qualitySampleTime>=1){
  lastFps=qualityFrames/Math.max(.001,t-qualitySampleTime);qualityFrames=0;qualitySampleTime=t;
  if(lastFps<43)quality=Math.max(.68,quality-.09);
  else if(lastFps<50)quality=Math.max(.68,quality-.045);
  else if(lastFps>57)quality=Math.min(1,quality+.025);
  setQualityScale(quality);
 }
 world.update(t,state.totalFound>=state.goal);movePlayer(dt,t);collectSeeds();checkGate();updateCamera(dt);renderer.render(scene,camera);
}
document.addEventListener("visibilitychange",()=>{if(!document.hidden)clock.getDelta();});
frame();
setTimeout(()=>ui.hideLoading(),180);
addEventListener("beforeunload",()=>{persist();controls.dispose();world.dispose();});
