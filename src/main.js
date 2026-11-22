import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';
import { GAME_CONFIG } from './config.js';
const mount=document.querySelector('#game'), scene=new THREE.Scene();scene.background=new THREE.Color('#8ac7bd');scene.fog=new THREE.Fog('#8ac7bd',24,62);
const camera=new THREE.PerspectiveCamera(48,innerWidth/innerHeight,.1,120);camera.position.set(0,8,13);
const renderer=new THREE.WebGLRenderer({antialias:true,alpha:false});renderer.setPixelRatio(Math.min(devicePixelRatio,1.8));renderer.setSize(innerWidth,innerHeight);renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;renderer.outputColorSpace=THREE.SRGBColorSpace;mount.appendChild(renderer.domElement);
scene.add(new THREE.HemisphereLight('#e5fff0','#47665b',2));const sun=new THREE.DirectionalLight('#fff2ce',2.3);sun.position.set(-7,14,8);sun.castShadow=true;sun.shadow.mapSize.set(1024,1024);scene.add(sun);
const mat=(color,roughness=.85)=>new THREE.MeshStandardMaterial({color,roughness});
const groundMat=mat('#77a96d'),soil=mat('#80654c'),bark=mat('#76533d'),leaf=mat('#3f8066'),leaf2=mat('#72ad77'),stone=mat('#b6c6a3'),gold=mat('#e9ef85'),water= new THREE.MeshStandardMaterial({color:'#64b8b1',roughness:.3,metalness:.1});
const island=new THREE.Mesh(new THREE.CylinderGeometry(13,11,2.4,9),soil);island.position.y=-1.2;island.receiveShadow=true;scene.add(island);
const lawn=new THREE.Mesh(new THREE.CylinderGeometry(13.1,13.1,.45,9),groundMat);lawn.position.y=-.05;lawn.receiveShadow=true;scene.add(lawn);
const waterRing=new THREE.Mesh(new THREE.TorusGeometry(13.15,.45,8,64),water);waterRing.rotation.x=Math.PI/2;waterRing.position.y=-.25;scene.add(waterRing);
const group=new THREE.Group();scene.add(group);
function tree(x,z,s=1){const g=new THREE.Group();const trunk=new THREE.Mesh(new THREE.CylinderGeometry(.18*s,.28*s,1.7*s,6),bark);trunk.position.y=.8*s;trunk.castShadow=true;g.add(trunk);for(let i=0;i<3;i++){const crown=new THREE.Mesh(new THREE.IcosahedronGeometry((.8-i*.12)*s,1),i%2?leaf:leaf2);crown.position.set((i-1)*.25*s,1.7*s+i*.36*s,0);crown.castShadow=true;g.add(crown)}g.position.set(x,0,z);group.add(g)}
[[-8,-5,1.3],[-6,5,1],[-10,1,.9],[7,-6,1.2],[9,2,1.1],[5,7,.9],[0,-9,1],[-3,8,.8]].forEach(v=>tree(...v));
function rock(x,z,s=1){const o=new THREE.Mesh(new THREE.DodecahedronGeometry(s,0),stone);o.position.set(x,s*.45,z);o.scale.y=.65;o.castShadow=true;group.add(o)}
[[-5,-2,.7],[5,1,.8],[-8,3,.5],[7,5,.6],[2,-7,.4]].forEach(v=>rock(...v));
const platform=new THREE.Mesh(new THREE.CylinderGeometry(2.1,2.4,.45,8),mat('#c2a77a'));platform.position.set(0,.22,0);platform.castShadow=true;group.add(platform);
const trunk=new THREE.Mesh(new THREE.CylinderGeometry(.45,.65,3.6,7),bark);trunk.position.set(0,2,0);trunk.castShadow=true;group.add(trunk);
const canopy=new THREE.Mesh(new THREE.IcosahedronGeometry(2.3,2),leaf);canopy.position.set(0,4.2,0);canopy.castShadow=true;group.add(canopy);
const glow=new THREE.PointLight('#d9f36b',2,10);glow.position.set(0,4.4,0);scene.add(glow);
const cat=new THREE.Group();scene.add(cat);const fur=mat('#e9d5ae'),dark=mat('#344e52'),pink=mat('#df947d'),eye=mat('#273c3d');
const body=new THREE.Mesh(new THREE.SphereGeometry(.58,16,12),fur);body.scale.set(1,.8,1.15);body.position.y=.75;body.castShadow=true;cat.add(body);
const head=new THREE.Mesh(new THREE.SphereGeometry(.48,16,12),fur);head.position.set(0,1.35,-.28);head.castShadow=true;cat.add(head);
for(const x of [-.29,.29]){const ear=new THREE.Mesh(new THREE.ConeGeometry(.19,.48,4),fur);ear.position.set(x,1.76,-.3);ear.rotation.z=x<0?.18:-.18;cat.add(ear);const inner=new THREE.Mesh(new THREE.ConeGeometry(.09,.28,4),pink);inner.position.set(x,1.77,-.27);cat.add(inner);const e=new THREE.Mesh(new THREE.SphereGeometry(.045,8,8),eye);e.position.set(x*.7,1.4,-.7);cat.add(e)}
const tail=new THREE.Mesh(new THREE.TorusGeometry(.35,.095,8,16,Math.PI*1.5),fur);tail.position.set(0,.85,.55);tail.rotation.x=1.1;cat.add(tail);
for(const x of [-.3,.3]){const leg=new THREE.Mesh(new THREE.CylinderGeometry(.12,.14,.42,8),fur);leg.position.set(x,.28,-.12);cat.add(leg)}
cat.position.set(0,0,5);
const seedGeo=new THREE.OctahedronGeometry(.32,0),seeds=[];const seedPositions=[[-4,1],[-7,-2],[4,-3],[8,1],[3,7],[-5,6],[7,-6],[-1,-7]];
seedPositions.forEach((p,i)=>{const s=new THREE.Mesh(seedGeo,new THREE.MeshStandardMaterial({color:'#e9f58c',emissive:'#8c9d39',emissiveIntensity:1.1,roughness:.25}));s.position.set(p[0],.9,p[1]);s.castShadow=true;s.userData={id:i};scene.add(s);const l=new THREE.PointLight('#d9f36b',.6,3);s.add(l);seeds.push(s)});
const gate=new THREE.Group();gate.position.set(0,0,-10.5);scene.add(gate);const gateRing=new THREE.Mesh(new THREE.TorusGeometry(1.55,.22,10,32),mat('#a4c9a8'));gateRing.position.y=2;gate.add(gateRing);const gateStone=new THREE.Mesh(new THREE.BoxGeometry(3.9,.5,.8),stone);gateStone.position.y=.25;gate.add(gateStone);for(const x of [-1.85,1.85]){const p=new THREE.Mesh(new THREE.BoxGeometry(.45,3.8,.7),stone);p.position.set(x,1.8,0);gate.add(p)}
const keys={};let touchMove={};let collected=0,started=false,velY=0,onGround=true,clock=new THREE.Clock(),built=new Set();
try{const save=JSON.parse(localStorage.getItem(GAME_CONFIG.saveKey)||'{}');collected=save.collected||0;built=new Set(save.built||[]);seeds.forEach((s,i)=>s.visible=!(save.seedIds||[]).includes(i));document.querySelector('#count').textContent=collected;document.querySelector('#colony').textContent=1+built.size;document.querySelector('#level').textContent=String(1+built.size).padStart(2,'0')}catch{}
function save(){localStorage.setItem(GAME_CONFIG.saveKey,JSON.stringify({collected,built:[...built],seedIds:seeds.filter(s=>!s.visible).map(s=>s.userData.id)}))}
function toast(t){const el=document.querySelector('#toast');el.textContent=t;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),1900)}
function updateUI(){document.querySelector('#count').textContent=collected;document.querySelector('#bar').style.width=(collected/GAME_CONFIG.starseedGoal*100)+'%';if(collected>=GAME_CONFIG.starseedGoal){document.querySelector('#questTitle').textContent='The gate remembers';document.querySelector('#questText').textContent='All eight starseeds found. Return to the ancient gate and step through.'}save()}
addEventListener('keydown',e=>{keys[e.key.toLowerCase()]=true;if(e.code==='Space'){e.preventDefault();jump()}});addEventListener('keyup',e=>keys[e.key.toLowerCase()]=false);
function jump(){if(onGround&&started){velY=GAME_CONFIG.jumpVelocity;onGround=false}}
document.querySelectorAll('[data-move]').forEach(b=>{const k=b.dataset.move;const down=e=>{e.preventDefault();touchMove[k]=true;if(k==='jump')jump()},up=e=>{e.preventDefault();touchMove[k]=false};b.addEventListener('pointerdown',down);b.addEventListener('pointerup',up);b.addEventListener('pointerleave',up);b.addEventListener('pointercancel',up)});
let dragging=false,lastX=0;renderer.domElement.addEventListener('pointerdown',e=>{dragging=true;lastX=e.clientX});addEventListener('pointerup',()=>dragging=false);renderer.domElement.addEventListener('pointermove',e=>{if(dragging&&e.pointerType==='touch'){const dx=e.clientX-lastX;cat.rotation.y-=dx*.006;lastX=e.clientX}});
document.querySelector('#start').onclick=()=>{started=true;document.querySelector('#intro').style.display='none';toast('Welcome home, little Shifter.')};
document.querySelector('#continue').onclick=()=>{started=true;document.querySelector('#intro').style.display='none';toast('Your journey continues.')};
document.querySelector('#campBtn').onclick=()=>document.querySelector('#panel').classList.toggle('open');document.querySelector('#closePanel').onclick=()=>document.querySelector('#panel').classList.remove('open');
document.querySelector('#mapBtn').onclick=()=>toast('Verdant · Sky-tree grove · Ancient gate');
document.querySelectorAll('[data-build]').forEach(b=>b.onclick=()=>{const id=b.dataset.build,cost=id==='garden'?3:5;if(built.has(id)){toast('Already part of your colony!');return}if(collected<cost){toast('You need '+cost+' starseeds first.');return}collected-=cost;built.add(id);document.querySelector('#colony').textContent=1+built.size;document.querySelector('#level').textContent=String(1+built.size).padStart(2,'0');b.textContent='BUILT ✓';b.disabled=true;updateUI();toast(id==='garden'?'A garden takes root!':'Beacon restored. The stars can see you.');});
let camYaw=0;
function animate(){requestAnimationFrame(animate);const dt=Math.min(clock.getDelta(),.04),t=clock.elapsedTime;
seeds.forEach(s=>{if(s.visible){s.rotation.y+=dt*1.5;s.position.y=.9+Math.sin(t*2+s.userData.id)*.15;if(cat.position.distanceTo(s.position)<1.15){s.visible=false;collected++;updateUI();toast('Starseed found · '+collected+'/8')}}});
const forward=keys.w||keys.arrowup||touchMove.forward,back=keys.s||keys.arrowdown||touchMove.back,left=keys.a||keys.arrowleft||touchMove.left,right=keys.d||keys.arrowright||touchMove.right;
let dx=(right?1:0)-(left?1:0),dz=(back?1:0)-(forward?1:0);const len=Math.hypot(dx,dz)||1;dx/=len;dz/=len;
if(started){cat.position.x+=dx*dt*GAME_CONFIG.playerSpeed;cat.position.z+=dz*dt*5;if(dx||dz){cat.rotation.y=Math.atan2(dx,dz);body.position.y=.75+Math.sin(t*13)*.045;cat.position.y=Math.abs(Math.sin(t*13))*.06}else{body.position.y=.75}velY-=GAME_CONFIG.gravity*dt;cat.position.y+=velY*dt;if(cat.position.y<=0){cat.position.y=0;velY=0;onGround=true}cat.position.x=Math.max(-11.2,Math.min(11.2,cat.position.x));cat.position.z=Math.max(-11.2,Math.min(11.2,cat.position.z));if(collected>=8&&cat.position.distanceTo(new THREE.Vector3(0,0,-10.5))<2.3){document.querySelector('#questTitle').textContent='A path between worlds';document.querySelector('#questText').textContent='The first world is restored. More planets await beyond the gate.';toast('WORLD RESTORED · VERDANT');collected=8}}
gateRing.rotation.z=Math.sin(t)*.04;gateRing.material=collected>=8?gold:stone;
const target=new THREE.Vector3(cat.position.x,cat.position.y+1,cat.position.z);const desired=new THREE.Vector3(cat.position.x,cat.position.y+7,cat.position.z+11);camera.position.lerp(desired,.07);camera.lookAt(target);renderer.render(scene,camera)}
animate();addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});