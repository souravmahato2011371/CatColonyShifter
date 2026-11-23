import {createVerdantTrees} from "./createTrees.js";
export function createVerdantWorld(THREE,scene){
 const group=new THREE.Group();scene.add(group);
 const mat=(color,roughness=.86)=>new THREE.MeshStandardMaterial({color,roughness});
 const soil=mat("#80654c"),grass=mat("#77a96d"),grassLight=mat("#94bd78"),bark=mat("#76533d"),leaf=mat("#3f8066"),leafLight=mat("#72ad77"),stone=mat("#b6c6a3");
 const water=new THREE.MeshStandardMaterial({color:"#64b8b1",roughness:.26,metalness:.12});
 const island=new THREE.Mesh(new THREE.CylinderGeometry(13,11,2.4,12),soil);island.position.y=-1.2;island.receiveShadow=true;group.add(island);
 const lawn=new THREE.Mesh(new THREE.CylinderGeometry(13.08,13.08,.45,12),grass);lawn.position.y=-.05;lawn.receiveShadow=true;group.add(lawn);
 const ring=new THREE.Mesh(new THREE.TorusGeometry(13.12,.42,8,72),water);ring.rotation.x=Math.PI/2;ring.position.y=-.28;group.add(ring);
 [[-3,-3,2.6,1.2],[4,3,2.2,1.5],[-7,2,1.4,2],[7,-3,1.7,1.3]].forEach((p,i)=>{
  const patch=new THREE.Mesh(new THREE.CircleGeometry(1,7),i%2?grassLight:grass);patch.rotation.x=-Math.PI/2;patch.position.set(p[0],.185,p[1]);patch.scale.set(p[2],p[3],1);group.add(patch);
 });
 const treeSet=createVerdantTrees(group);
 function rock(x,z,s){const o=new THREE.Mesh(new THREE.DodecahedronGeometry(s,0),stone);o.position.set(x,s*.38,z);o.scale.y=.62;o.rotation.set(.2,x*.13,z*.11);o.castShadow=true;group.add(o);}
 [[-5,-2,.65],[5,1,.78],[-8,3,.48],[7,5,.58],[2,-7,.4],[-2,6,.35],[9,-5,.42]].forEach(v=>rock(...v));
 const plantGeo=new THREE.ConeGeometry(.13,.55,5),plantMats=[leaf,leafLight,grassLight];
 [[-5,1],[-3,-5],[2,5],[6,1],[-8,-1],[8,4],[4,-8],[-1,7],[-6,6],[6,-4],[0,-6],[9,0]].forEach((p,i)=>{
  for(let n=0;n<3;n++){const plant=new THREE.Mesh(plantGeo,plantMats[(i+n)%3]);plant.position.set(p[0]+(n-1)*.18,.22,p[1]+(n%2)*.17);plant.rotation.z=(n-1)*.24;group.add(plant);}
 });
 const hub=new THREE.Group();group.add(hub);
 const plinth=new THREE.Mesh(new THREE.CylinderGeometry(2.1,2.4,.45,9),mat("#c2a77a"));plinth.position.y=.22;plinth.castShadow=true;hub.add(plinth);
 const glow=new THREE.PointLight("#d9f36b",2.2,12);glow.position.set(0,4.7,0);scene.add(glow);
 const gate=new THREE.Group();gate.position.set(0,0,-10.5);group.add(gate);
 const base=new THREE.Mesh(new THREE.BoxGeometry(4.1,.5,.8),stone);base.position.y=.25;gate.add(base);
 for(const x of [-1.85,1.85]){const p=new THREE.Mesh(new THREE.BoxGeometry(.45,3.8,.7),stone);p.position.set(x,1.8,0);p.castShadow=true;gate.add(p);}
 const gateRing=new THREE.Mesh(new THREE.TorusGeometry(1.55,.2,10,40),mat("#a4c9a8",.38));gateRing.position.y=2;gate.add(gateRing);
 const portal=new THREE.Mesh(new THREE.CircleGeometry(1.34,40),new THREE.MeshBasicMaterial({color:"#6aa99b",transparent:true,opacity:.18,side:THREE.DoubleSide}));portal.position.set(0,2,-.04);gate.add(portal);
 const positions=[[-4,1],[-7,-2],[4,-3],[8,1],[3,7],[-5,6],[7,-6],[-1,-7]];
 const seeds=positions.map((p,id)=>{
  const g=new THREE.Group(),core=new THREE.Mesh(new THREE.OctahedronGeometry(.29,0),new THREE.MeshStandardMaterial({color:"#e9f58c",emissive:"#8c9d39",emissiveIntensity:1.1,roughness:.2,metalness:.08}));
  core.rotation.z=Math.PI/4;g.add(core);
  const halo=new THREE.Mesh(new THREE.TorusGeometry(.43,.018,5,24),new THREE.MeshBasicMaterial({color:"#efffb1",transparent:true,opacity:.78}));
  halo.rotation.x=Math.PI/2;g.add(halo);g.add(new THREE.PointLight("#d9f36b",.45,2.8));
  g.position.set(p[0],.95,p[1]);g.userData.id=id;scene.add(g);return g;
 });
 const count=80,coords=new Float32Array(count*3);
 for(let i=0;i<count;i++){const a=Math.random()*Math.PI*2,r=4+Math.random()*14;coords[i*3]=Math.cos(a)*r;coords[i*3+1]=.5+Math.random()*7;coords[i*3+2]=Math.sin(a)*r;}
 const moteGeometry=new THREE.BufferGeometry();moteGeometry.setAttribute("position",new THREE.BufferAttribute(coords,3));
 const motes=new THREE.Points(moteGeometry,new THREE.PointsMaterial({color:"#e2ffc0",size:.055,transparent:true,opacity:.65,depthWrite:false}));scene.add(motes);
 function update(t,restored){
  gateRing.rotation.z=Math.sin(t*.7)*.035;gateRing.material.color.set(restored?"#e9ef85":"#a4c9a8");portal.material.opacity=restored?.48+Math.sin(t*3)*.12:.12;
  motes.rotation.y=t*.018;
  treeSet.update(t);
  seeds.forEach(s=>{if(!s.visible)return;s.rotation.y+=.012;s.position.y=.95+Math.sin(t*2.2+s.userData.id)*.13;s.children[1].rotation.z+=.018;});
 }
 return {group,seeds,gate,update,dispose(){scene.remove(glow,motes);moteGeometry.dispose();treeSet.dispose();}};
}
